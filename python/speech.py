import os
import logging
import tempfile
from typing import Dict, Any

import librosa
import numpy as np
import parselmouth
from fastapi import UploadFile, HTTPException

from config import get_sample_audio_path
from text_comparison import compare_words_with_structure, compare_texts_hybrid
from openai_client import generate_personalized_feedback

logger = logging.getLogger(__name__)


def _pitch_contour(y: np.ndarray, sr: int):
    if len(y) == 0:
        return None
    f0, voiced_flag, _ = librosa.pyin(
        y,
        fmin=librosa.note_to_hz("C2"),
        fmax=librosa.note_to_hz("C7"),
        sr=sr,
        hop_length=256,
    )
    valid = ~np.isnan(f0) & voiced_flag
    if not np.any(valid):
        return None
    contour = f0[valid]
    return contour if len(contour) >= 5 else None


def _intonation_score(user_contour, sample_contour, pitch_mean: float) -> tuple[int, str]:
    if user_contour is not None and sample_contour is not None and len(user_contour) > 10 and len(sample_contour) > 10:
        um, us, ur = np.mean(user_contour), np.std(user_contour), np.ptp(user_contour)
        sm, ss, sr = np.mean(sample_contour), np.std(sample_contour), np.ptp(sample_contour)
        if not (50 <= um <= 500):
            return 0, "Không thể phân tích ngữ điệu (cao độ không hợp lệ)"
        mean_s = int(100 * (1 - min(1.0, abs(um - sm) / max(sm, 1))))
        std_s = int(100 * (1 - min(1.0, abs(us - ss) / max(ss, 1))))
        range_s = int(100 * (1 - min(1.0, abs(ur - sr) / max(sr, 1))))
        score = int(0.5 * mean_s + 0.3 * std_s + 0.2 * range_s)
    elif pitch_mean < 50:
        return 0, "Không thể phân tích ngữ điệu (cao độ quá thấp)"
    elif pitch_mean > 500:
        return 0, "Không thể phân tích ngữ điệu (cao độ quá cao)"
    elif 150 <= pitch_mean <= 300:
        score = int(80 + min(20, 20 * (1 - abs(pitch_mean - 225) / 75)))
    elif 80 <= pitch_mean <= 400:
        dist = min(abs(pitch_mean - 150), abs(pitch_mean - 300))
        score = int(max(50, 80 - dist / 5))
    elif pitch_mean < 80:
        score = int(max(10, 50 - (80 - pitch_mean) / 2))
    else:
        score = int(max(10, 50 - (pitch_mean - 400) / 10))

    score = max(0, min(100, score))
    if score >= 90:
        label = "Ngữ điệu xuất sắc"
    elif score >= 75:
        label = "Ngữ điệu tự nhiên"
    elif score >= 50:
        label = "Ngữ điệu chấp nhận được"
    else:
        label = "Cần điều chỉnh cao độ"
    return score, label


def _clarity_score(user_y: np.ndarray, sr: int, sample_y) -> tuple[int, str, float]:
    try:
        snd = parselmouth.Sound(np.array(user_y), sr)
        formants = snd.to_formant_burg()
        f1_values = [
            formants.get_value_at_time(1, t)
            for t in formants.ts()
            if not np.isnan(formants.get_value_at_time(1, t))
        ]
        f1_mean = np.mean(f1_values) if f1_values else 500.0

        if sample_y is not None:
            s_snd = parselmouth.Sound(np.array(sample_y), sr)
            s_formants = s_snd.to_formant_burg()
            s_f1 = [
                s_formants.get_value_at_time(1, t)
                for t in s_formants.ts()
                if not np.isnan(s_formants.get_value_at_time(1, t))
            ]
            s_f1_mean = np.mean(s_f1) if s_f1 else None
        else:
            s_f1_mean = None

        if s_f1_mean:
            score = int(100 * (1 - min(1.0, abs(f1_mean - s_f1_mean) / max(s_f1_mean, 1))))
            score = max(30, score)
        elif 450 <= f1_mean <= 650:
            score = int(100 * (1 - abs(f1_mean - 550) / 100))
        elif f1_mean < 450:
            score = max(0, int(70 - (450 - f1_mean) / 10))
        else:
            score = max(0, int(70 - (f1_mean - 650) / 15))

        score = max(0, min(100, score))
    except Exception as e:
        logger.error(f"clarity_score failed: {e}")
        f1_mean, score = 500.0, 50

    if score >= 90:
        label = "Độ rõ xuất sắc"
    elif score >= 75:
        label = "Độ rõ tốt"
    elif score >= 50:
        label = "Độ rõ chấp nhận được"
    elif f1_mean < 450:
        label = "Cần mở rộng miệng hơn khi phát âm"
    elif f1_mean > 650:
        label = "Cần điều chỉnh vị trí lưỡi khi phát âm"
    else:
        label = "Cần cải thiện độ rõ khi phát âm"

    return score, label, float(f1_mean)


async def analyze_audio(
    user_audio: UploadFile,
    reference_text: str,
    audio_type: str | None,
) -> Dict[str, Any]:
    user_audio_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3") as tmp:
            tmp.write(await user_audio.read())
            user_audio_path = tmp.name
        await user_audio.seek(0)

        # Resolve sample audio
        sample_audio_path = None
        if audio_type in ("conversation", "vocabulary"):
            candidate = get_sample_audio_path(audio_type, reference_text)
            if os.path.exists(candidate):
                sample_audio_path = candidate
                logger.info(f"Sample audio found: {sample_audio_path}")
            else:
                logger.info(f"Sample audio not found: {candidate}")

        reference_text = reference_text or ""

        # Transcribe
        from openai_client import client as _client
        try:
            with open(user_audio_path, "rb") as f:
                transcription = _client.audio.transcriptions.create(
                    model="whisper-1", file=f, language="ja"
                ).text
        except Exception as e:
            logger.warning(f"Transcription failed: {e}")
            transcription = reference_text

        # Load audio
        try:
            user_y, sr = librosa.load(user_audio_path, sr=16000)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Cannot read audio: {e}")

        sample_y = None
        if sample_audio_path:
            try:
                sample_y, _ = librosa.load(sample_audio_path, sr=sr)
            except Exception:
                sample_y = None

        # Pitch / intonation
        audio_rms = float(np.sqrt(np.mean(user_y ** 2)))
        pitch_mean = 0.0
        user_pitch_contour = None
        sample_pitch_contour = None

        if audio_rms >= 0.01:
            pitches, magnitudes = librosa.piptrack(y=user_y, sr=sr)
            pitch_values = [p for p, m in zip(pitches.T, magnitudes.T) if m.max() > 0]
            if pitch_values:
                pitch_mean = float(np.mean(pitch_values))
            try:
                user_pitch_contour = _pitch_contour(user_y, sr)
                if sample_y is not None:
                    sample_pitch_contour = _pitch_contour(sample_y, sr)
            except Exception as e:
                logger.error(f"Pitch extraction failed: {e}")

        intonation_score, intonation_label = _intonation_score(
            user_pitch_contour, sample_pitch_contour, pitch_mean
        )

        # Formants / clarity
        clarity_score, clarity_label, f1_mean = _clarity_score(user_y, sr, sample_y)

        # Text comparison
        comparison = await compare_words_with_structure(reference_text, transcription)
        hybrid = await compare_texts_hybrid(reference_text, transcription)
        text_score = int(hybrid["hybrid_similarity"] * 100)

        if text_score == 0:
            intonation_score, clarity_score = 0, 0
            intonation_label = "Không thể phân tích ngữ điệu (không khớp văn bản)"
            clarity_label = "Không thể phân tích độ rõ (không khớp văn bản)"

        # Final score
        if text_score == 0:
            score = 0
        else:
            weighted = text_score * 0.7 + intonation_score * 0.15 + clarity_score * 0.15
            if text_score >= 95 and intonation_score >= 95 and clarity_score >= 95:
                bonus = min(1.0, (text_score + intonation_score + clarity_score) / 300 * 1.05)
                score = int(weighted * bonus)
            else:
                score = int(weighted)
            score = max(0, min(100, score))

        feedback = await generate_personalized_feedback(
            comparison["enhanced_words"],
            reference_text,
            transcription,
            comparison["llm_analysis"],
            score,
            intonation_score,
            clarity_score,
            text_score,
        )

        return {
            "score": score,
            "feedback": feedback,
            "transcription": transcription,
            "analysis": {
                "intonation": {"status": intonation_label, "score": float(intonation_score)},
                "clarity": {"status": clarity_label, "score": float(clarity_score)},
                "text": {
                    "score": float(text_score),
                    "similarity_ratio": float(comparison["token_comparison"]["similarity_ratio"]),
                },
            },
            "measurements": {"pitchMean": pitch_mean, "f1Mean": f1_mean},
            "textAnalysis": {
                "originalStructure": comparison["original_structure"],
                "enhancedWords": comparison["enhanced_words"],
                "tokenComparison": {
                    "matched_tokens": comparison["token_comparison"]["matched_tokens"],
                    "original_tokens": comparison["token_comparison"]["original_tokens"],
                    "transcription_tokens": comparison["token_comparison"]["transcription_tokens"],
                },
                "llmAnalysis": comparison["llm_analysis"],
            },
            "pitchData": {
                "user": user_pitch_contour.tolist() if user_pitch_contour is not None else [],
                "sample": sample_pitch_contour.tolist() if sample_pitch_contour is not None else [],
            },
        }

    finally:
        if user_audio_path and os.path.exists(user_audio_path):
            try:
                os.unlink(user_audio_path)
            except Exception:
                pass
