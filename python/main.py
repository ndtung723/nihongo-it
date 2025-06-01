import os
import json
import logging
import traceback
from fastapi import FastAPI, File, UploadFile, Form, HTTPException, Request
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from openai import OpenAI
import librosa
import parselmouth
import numpy as np
from string import punctuation
import soundfile as sf
import tempfile
from dotenv import load_dotenv
import re
from typing import Optional, Dict, List, Any, Union
import mojimoji
import difflib

# Load environment variables from .env file
load_dotenv()

# Debug mode (set to True for detailed error responses)
DEBUG_MODE = os.getenv("DEBUG_MODE", "False").lower() in ("true", "1", "t")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")

# Sample audio configuration
SAMPLE_AUDIO_BASE_PATH = os.getenv("SAMPLE_AUDIO_BASE_PATH", "services/ai-service/src/main/resources")
PROJECT_ROOT_PATH = os.getenv("PROJECT_ROOT_PATH", "")

def get_project_root() -> str:
    """Get the project root path, auto-detect if not configured."""
    if PROJECT_ROOT_PATH:
        return PROJECT_ROOT_PATH
    
    # Auto-detect project root by looking for common project files
    current_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Go up directories until we find project root indicators
    while current_dir != os.path.dirname(current_dir):  # Not at filesystem root
        # Check for common project root indicators
        if any(os.path.exists(os.path.join(current_dir, indicator)) for indicator in 
               ['services', 'pom.xml', '.git', 'package.json', 'README.md']):
            return current_dir
        current_dir = os.path.dirname(current_dir)
    
    # Fallback to current directory's parent
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def get_sample_audio_path(audio_type: str, reference_text: str) -> str:
    """Build sample audio file path dynamically."""
    project_root = get_project_root()
    sample_path = os.path.join(
        project_root,
        SAMPLE_AUDIO_BASE_PATH,
        audio_type,
        f"{reference_text}.mp3"
    )
    return sample_path

app = FastAPI()

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"],
    allow_headers=["*"],
    expose_headers=["*"],
    max_age=86400,  # 24 hours
)

# Cấu hình logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Khởi tạo Open AI API key
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# Try to import SudachiPy for advanced tokenization
try:
    from sudachipy import tokenizer
    from sudachipy import dictionary
    SUDACHI_AVAILABLE = True
    logger.info("SudachiPy imported successfully - using advanced tokenization")
except ImportError:
    SUDACHI_AVAILABLE = False
    logger.error("SudachiPy not available - this is required for the enhanced API")
    raise ImportError("SudachiPy is required for the enhanced API. Please install it with: pip install sudachipy sudachidict_core")

# Initialize SudachiPy tokenizer
try:
    sudachi_tokenizer = dictionary.Dictionary().create()
    logger.info("SudachiPy tokenizer initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize SudachiPy tokenizer: {e}")
    raise RuntimeError("Failed to initialize SudachiPy tokenizer")

# Regex để loại bỏ dấu câu và ký tự đặc biệt tiếng Nhật
JAPANESE_PUNCTUATION = '、。！？；：「」『』・〜'
CLEAN_REGEX = re.compile(f'[{re.escape(punctuation + JAPANESE_PUNCTUATION)}]|[^\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\sA-Za-z0-9]')

async def katakana_to_hiragana(text: str) -> str:
    """Convert katakana to hiragana."""
    result = ""
    for char in text:
        if '\u30A1' <= char <= '\u30FA':  # Katakana range
            # Convert to hiragana by subtracting 0x60
            result += chr(ord(char) - 0x60)
        else:
            result += char
    return result

async def to_hiragana(text: str) -> str:
    """Convert text to hiragana for normalization using SudachiPy."""
    try:
        mode = tokenizer.Tokenizer.SplitMode.C
        tokens = sudachi_tokenizer.tokenize(text, mode)
        
        hiragana_parts = []
        for token in tokens:
            reading = token.reading_form()
            # Convert katakana reading to hiragana
            hiragana_reading = await katakana_to_hiragana(reading)
            hiragana_parts.append(hiragana_reading)
        
        result = ''.join(hiragana_parts)
        logger.debug(f"SudachiPy hiragana conversion: '{text}' -> '{result}'")
        return result
        
    except Exception as e:
        logger.error(f"Hiragana conversion failed: {e}")
        return text

async def clean_text(text: str) -> str:
    """Loại bỏ dấu câu và ký tự đặc biệt."""
    return CLEAN_REGEX.sub('', text)

async def tokenize_japanese(text: str) -> List[str]:
    """Enhanced Japanese tokenization using SudachiPy."""
    cleaned_text = await clean_text(text)
    if not cleaned_text:
        return []
    
    try:
        # Use mode C for detailed tokenization
        mode = tokenizer.Tokenizer.SplitMode.C
        tokens = sudachi_tokenizer.tokenize(cleaned_text, mode)
        
        words = []
        for token in tokens:
            surface = token.surface()
            # Skip punctuation and empty tokens
            if surface.strip() and not re.match(r'^[^\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]+$', surface):
                words.append(surface)
    
        logger.debug(f"SudachiPy tokenization: '{cleaned_text}' -> {words}")
        return words
        
    except Exception as e:
        logger.error(f"Tokenization failed: {e}")
        # Last resort: split by common particles and basic characters
        return [char for char in cleaned_text if char.strip()]

async def analyze_with_llm(original: str, transcription: str) -> Dict[str, Any]:
    try:
        prompt = f"""
Analyze these two Japanese sentences:
- Original: '{original}'
- Transcription: '{transcription}'

Identify:
1. Words that are incorrect or different
2. Auxiliary words/particles (like ね, よ) that don't affect the core meaning
3. Brief pronunciation suggestions in Vietnamese

Return ONLY a JSON object with this structure:
{{
  "incorrect_words": [
    {{"word": "original_word", "transcription_word": "spoken_word", "suggestion": "pronunciation_tip_in_vietnamese"}}
  ],
  "auxiliary_words": ["word1", "word2"],
  "personalized_feedback": "brief_encouraging_feedback_in_vietnamese"
}}
"""
        # Call OpenAI API
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=500
        )
        
        content = response.choices[0].message.content.strip()
        # Extract JSON from the response (handle potential text wrapping)
        json_content = re.search(r'({.*})', content.replace('\n', ' '), re.DOTALL)
        if json_content:
            result = json.loads(json_content.group(1))
            return result
        else:
            return {"incorrect_words": [], "auxiliary_words": [], "personalized_feedback": ""}
            
    except Exception as e:
        return {
            "incorrect_words": [],
            "auxiliary_words": ["ね", "よ", "な", "わ", "さ"],
            "personalized_feedback": "Cần cải thiện phát âm. Hãy thử lại nhé!"
        }

async def generate_personalized_feedback(words: List[Dict], 
                                       sentence: str, 
                                       transcription: str, 
                                       llm_result: Dict,
                                       score: int = 0,
                                       intonation_score: int = 0,
                                       clarity_score: int = 0,
                                       text_score: int = 0) -> str:
    """Generate intelligent personalized feedback based on detailed analysis."""
    
    # Analyze the specific issues for detailed feedback first
    try:
        # Get detailed analysis of what went wrong
        incorrect_words = [w["text"] for w in words if not w.get("isCorrect", True)]
        correct_words = [w["text"] for w in words if w.get("isCorrect", True)]
        
        # Analyze missing words (in original but not in transcription)
        original_tokens = await process_text_to_tokens(sentence)
        transcription_tokens = await process_text_to_tokens(transcription)
        
        missing_words = [token for token in original_tokens if token not in transcription_tokens]
        extra_words = [token for token in transcription_tokens if token not in original_tokens]
        
        # Calculate percentages for better analysis
        total_words = len(original_tokens)
        correct_percentage = len(correct_words) / total_words * 100 if total_words > 0 else 0
        
        # Determine primary issues
        issues = []
        strengths = []
        suggestions = []
        
        # Analyze text accuracy issues with more detail
        if text_score < 20:
            if not transcription.strip():
                issues.append("không nhận diện được âm thanh")
                suggestions.append("hãy nói to và rõ ràng hơn, đảm bảo micro hoạt động tốt")
            else:
                issues.append("phát âm sai hoàn toàn")
                suggestions.append("hãy nghe lại câu mẫu và luyện tập từng từ")
        elif text_score < 40:
            if len(transcription_tokens) < len(original_tokens) / 2:
                issues.append("thiếu quá nhiều từ trong câu")
                suggestions.append("hãy đọc chậm và đầy đủ từng từ trong câu")
            else:
                issues.append("phát âm sai nhiều từ")
                if incorrect_words:
                    suggestions.append(f"tập trung luyện phát âm: {', '.join(incorrect_words[:3])}")
        elif text_score < 70:
            if missing_words:
                issues.append(f"thiếu {len(missing_words)} từ")
                suggestions.append(f"nhớ phát âm đầy đủ, đặc biệt từ '{missing_words[0]}'")
            if incorrect_words:
                issues.append(f"phát âm chưa chuẩn {len(incorrect_words)} từ")
                suggestions.append(f"cải thiện phát âm từ '{incorrect_words[0]}'")
        elif text_score < 90:
            if incorrect_words:
                issues.append("một vài từ chưa chuẩn")
                suggestions.append(f"hoàn thiện phát âm từ '{incorrect_words[0]}'")
            else:
                strengths.append("phát âm từ ngữ rất tốt")
        else:
            strengths.append("phát âm từ ngữ xuất sắc")
            
        # Analyze intonation issues with more detail
        if intonation_score < 30:
            issues.append("ngữ điệu chưa đúng")
            suggestions.append("nghe và bắt chước ngữ điệu tiếng Nhật từ audio mẫu")
        elif intonation_score < 50:
            issues.append("ngữ điệu cần cải thiện")
            suggestions.append("chú ý cao độ và nhấn mạnh khi nói")
        elif intonation_score < 70:
            suggestions.append("tiếp tục cải thiện ngữ điệu để tự nhiên hơn")
        elif intonation_score < 90:
            strengths.append("ngữ điệu khá tốt")
        else:
            strengths.append("ngữ điệu tuyệt vời")
            
        # Analyze clarity issues with more detail
        if clarity_score < 30:
            issues.append("độ rõ kém")
            suggestions.append("mở miệng rộng hơn và phát âm từng âm tiết rõ ràng")
        elif clarity_score < 50:
            issues.append("độ rõ chưa tốt")
            suggestions.append("chú ý vị trí lưỡi và môi khi phát âm")
        elif clarity_score < 70:
            suggestions.append("phát âm rõ ràng hơn nữa")
        elif clarity_score < 90:
            strengths.append("phát âm khá rõ ràng")
        else:
            strengths.append("phát âm rất rõ ràng")

        # Build intelligent feedback with more variety
        feedback_parts = []
        
        # Start with encouragement based on performance
        if score >= 90:
            feedback_parts.append("Xuất sắc!")
        elif score >= 75:
            feedback_parts.append("Rất tốt!")
        elif score >= 60:
            feedback_parts.append("Khá tốt!")
        elif score >= 40:
            feedback_parts.append("Bạn đã cố gắng!")
        else:
            feedback_parts.append("Đừng nản lòng!")
            
        # Add strengths if any
        if strengths:
            feedback_parts.append(f"{', '.join(strengths)}.")
            
        # Add main issues and suggestions
        if issues and suggestions:
            main_issue = issues[0]
            main_suggestion = suggestions[0]
            feedback_parts.append(f"Cần cải thiện {main_issue} - {main_suggestion}.")
            
            # Add second suggestion if available and different
            if len(suggestions) > 1 and suggestions[1] != main_suggestion:
                feedback_parts.append(f"{suggestions[1]}.")
        elif suggestions:
            feedback_parts.append(f"{suggestions[0]}.")
            
        # Add encouragement to continue
        if score >= 70:
            feedback_parts.append("Tiếp tục phát huy!")
        elif score >= 40:
            feedback_parts.append("Hãy luyện tập thêm!")
        else:
            feedback_parts.append("Đừng bỏ cuộc, bạn sẽ tiến bộ!")
            
        # Combine all parts
        final_feedback = " ".join(feedback_parts)
        
        # Ensure we have meaningful feedback
        if len(final_feedback) < 30:
            return await generate_llm_feedback(sentence, transcription, score, intonation_score, clarity_score, text_score, incorrect_words, missing_words)
            
        return final_feedback
        
    except Exception as e:
        logger.error(f"Error generating intelligent feedback: {e}")
        # Final fallback to LLM
        return await generate_llm_feedback(sentence, transcription, score, intonation_score, clarity_score, text_score, [], [])

async def generate_llm_feedback(sentence: str, transcription: str, score: int, intonation_score: int, 
                               clarity_score: int, text_score: int, incorrect_words: List[str], 
                               missing_words: List[str]) -> str:
    """Generate feedback using LLM with detailed context."""
    try:
        # Create comprehensive prompt for better AI feedback
        prompt = f"""
Phân tích phát âm tiếng Nhật chi tiết và đưa ra phản hồi khuyến khích:

THÔNG TIN PHÂN TÍCH:
- Câu gốc: '{sentence}'
- Câu đã nói: '{transcription}'
- Điểm tổng: {score}/100
- Điểm văn bản: {text_score}/100 (độ chính xác từ ngữ)
- Điểm ngữ điệu: {intonation_score}/100 (cao độ, nhấn mạnh)
- Điểm độ rõ: {clarity_score}/100 (phát âm rõ ràng)

PHÂN TÍCH CHI TIẾT:
- Từ phát âm sai: {incorrect_words if incorrect_words else 'Không có'}
- Từ bị thiếu: {missing_words if missing_words else 'Không có'}

YÊU CẦU PHẢN HỒI:
1. Bắt đầu bằng lời khuyến khích tích cực
2. Chỉ ra cụ thể 1-2 lỗi chính cần cải thiện (dựa trên phân tích trên)
3. Đưa ra gợi ý thực tế để cải thiện
4. Tối đa 3 câu, bằng tiếng Việt
5. Tập trung vào điểm mạnh và lời khuyên cụ thể

Ví dụ phản hồi tốt:
- "Tuyệt vời! Bạn đã phát âm đúng hầu hết các từ. Hãy chú ý phát âm từ 'がくせい' rõ ràng hơn và cải thiện ngữ điệu để tự nhiên hơn. Tiếp tục luyện tập, bạn đang tiến bộ rất tốt!"
"""
        
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=400  # Increased for more detailed feedback
        )
        return response.choices[0].message.content.strip()
        
    except Exception as e:
        logger.error(f"Error generating LLM feedback: {e}")

async def analyze_audio_enhanced(user_audio: UploadFile, 
                            reference_text: str = Form(None),
                            type: str = Form(None)):
    """Enhanced audio analysis with integrated audio features analysis and LLM comparison."""
    
    user_audio_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3") as user_temp:
            user_content = await user_audio.read()
            user_temp.write(user_content)
            user_audio_path = user_temp.name
            user_audio.file.seek(0)
            
        sample_audio_path = None
        if type:
            # Determine sample path based on type
            if type == "conversation":
                sample_path = get_sample_audio_path("conversation", reference_text)
            elif type == "vocabulary":
                sample_path = get_sample_audio_path("vocabulary", reference_text)
            else:
                sample_path = None
            
            logger.info(f"Type: {type}, Reference text: {reference_text}")
            logger.info(f"Constructed sample path: {sample_path}")
           
            if sample_path and os.path.exists(sample_path):
                sample_audio_path = sample_path
                logger.info(f"Sample file found: {sample_audio_path}")
            else:
                sample_audio_path = None
                logger.info(f"Sample file NOT found at: {sample_path}")
        else:
            logger.info("No type provided, skipping sample audio")
            
        if not reference_text:
            reference_text = ""
            
        # Transcribe user audio
        try:
            with open(user_audio_path, "rb") as audio_file:
                response = client.audio.transcriptions.create(
                    model="whisper-1",
                    file=audio_file,
                    language="ja"
                )
                transcription = response.text
        except Exception as e:
            transcription = reference_text

        # Load audio files
        try:
            user_y, sr = librosa.load(user_audio_path, sr=16000)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Cannot read audio file: {str(e)}")
            
        sample_y = None
        if sample_audio_path and os.path.exists(sample_audio_path):
            try:
                sample_y, _ = librosa.load(sample_audio_path, sr=sr)
            except Exception as e:
                sample_y = None

        # ===== INTEGRATED AUDIO FEATURES ANALYSIS =====
        
        # Check audio energy first to validate audio quality
        audio_rms = np.sqrt(np.mean(user_y**2))
        audio_energy = np.sum(user_y**2)
        
        # Initialize default values
        intonation_score = 0
        intonation = "Không thể phân tích ngữ điệu"
        pitch_mean = 0
        user_pitch_data = []
        sample_pitch_data = []
        
        # ===== PITCH AND INTONATION ANALYSIS =====
        
        # Initialize pitch contour variables at the beginning
        user_pitch_contour = None
        sample_pitch_contour = None
        
        if audio_rms < 0.01:
            intonation = "Không thể phân tích ngữ điệu (âm thanh quá yếu)"
        else:
            # Extract pitch values from audio using librosa
            pitches, magnitudes = librosa.piptrack(y=user_y, sr=sr)
            pitch_values = [p for p, m in zip(pitches.T, magnitudes.T) if m.max() > 0]
            
            # Only analyze if we have extracted pitch values
            if len(pitch_values) > 0:
                pitch_mean = np.mean(pitch_values)
                pitch_std = np.std(pitch_values)
                
                # Analyze pitch contour for more detailed intonation information
                try:
                    # Extract pitch contour (F0) using more precise method
                    if len(user_y) > 0:
                        user_hop_length = 256
                        user_f0, voiced_flag, voiced_probs = librosa.pyin(
                            user_y, 
                            fmin=librosa.note_to_hz('C2'), 
                            fmax=librosa.note_to_hz('C7'),
                            sr=sr,
                            hop_length=user_hop_length
                        )
                        # Filter out NaN values and check we have valid data
                        valid_indices = ~np.isnan(user_f0) & voiced_flag
                        if np.any(valid_indices):
                            user_pitch_contour = user_f0[valid_indices]
                            
                            # Only continue if we have enough pitch points
                            if len(user_pitch_contour) < 5:
                                user_pitch_contour = None
                        else:
                            logger.warning("No valid pitch points detected in audio")
                            
                    # Extract pitch contour from sample if available
                    if sample_y is not None and len(sample_y) > 0:
                        sample_hop_length = 256
                        sample_f0, sample_voiced_flag, sample_voiced_probs = librosa.pyin(
                            sample_y, 
                            fmin=librosa.note_to_hz('C2'), 
                            fmax=librosa.note_to_hz('C7'),
                            sr=sr,
                            hop_length=sample_hop_length
                        )
                        
                        # Filter out NaN values and check we have valid data
                        valid_sample_indices = ~np.isnan(sample_f0) & sample_voiced_flag
                        if np.any(valid_sample_indices):
                            sample_pitch_contour = sample_f0[valid_sample_indices]
                            
                            # Log sample pitch statistics
                            sample_pitch_mean = np.mean(sample_pitch_contour) if len(sample_pitch_contour) > 0 else 0
                            sample_pitch_std = np.std(sample_pitch_contour) if len(sample_pitch_contour) > 0 else 0
                except Exception as e:
                    logger.error(f"Error extracting pitch contours: {str(e)}")
                    user_pitch_contour = None
                    sample_pitch_contour = None
                    
                # Calculate intonation score based on comparison with sample or sensible defaults
                if (sample_pitch_contour is not None and len(sample_pitch_contour) > 10 and 
                    user_pitch_contour is not None and len(user_pitch_contour) > 10):
                    # Calculate sample statistics
                    sample_pitch_mean = np.mean(sample_pitch_contour)
                    sample_pitch_std = np.std(sample_pitch_contour)
                    sample_pitch_range = np.ptp(sample_pitch_contour)
                    
                    # Calculate user statistics
                    user_pitch_mean = np.mean(user_pitch_contour)
                    user_pitch_std = np.std(user_pitch_contour)
                    user_pitch_range = np.ptp(user_pitch_contour)
                    
                    # Check if user pitch is realistic - should be between 50-500Hz for human voice
                    if 50 <= user_pitch_mean <= 500:
                        # Compare means (how close is the average pitch?)
                        mean_diff_ratio = min(1.0, abs(user_pitch_mean - sample_pitch_mean) / max(sample_pitch_mean, 1))
                        mean_score = int(100 * (1 - mean_diff_ratio))
                        
                        # Compare variability (how similar is the pitch variation?)
                        std_diff_ratio = min(1.0, abs(user_pitch_std - sample_pitch_std) / max(sample_pitch_std, 1))
                        std_score = int(100 * (1 - std_diff_ratio))
                        
                        # Compare pitch range (how similar is the overall pitch range?)
                        range_diff_ratio = min(1.0, abs(user_pitch_range - sample_pitch_range) / max(sample_pitch_range, 1))
                        range_score = int(100 * (1 - range_diff_ratio))
                        
                        # Weight the scores
                        intonation_score = int(0.5 * mean_score + 0.3 * std_score + 0.2 * range_score)
                        
                        # Use the appropriate pitch mean for display
                        pitch_mean = user_pitch_mean
                    else:
                        intonation_score = 0
                        intonation = "Không thể phân tích ngữ điệu (cao độ không hợp lệ)"
                else:
                    # Check if pitch_mean is realistic - should be between 50-500Hz for human voice
                    if pitch_mean < 50:
                        intonation_score = 0
                        intonation = "Không thể phân tích ngữ điệu (cao độ quá thấp)"
                    elif pitch_mean > 500:
                        intonation_score = 0
                        intonation = "Không thể phân tích ngữ điệu (cao độ quá cao)"
                    # For normal human voice ranges (adjusted to be more lenient)
                    elif 80 <= pitch_mean <= 400:
                        # Very good range is 150-300Hz for Japanese speech
                        if 150 <= pitch_mean <= 300:
                            # Ideal range gets high score
                            intonation_score = int(80 + min(20, 20 * (1 - abs(pitch_mean - 225) / 75)))
                        else:
                            # Still acceptable but not ideal
                            distance_from_good = min(abs(pitch_mean - 150), abs(pitch_mean - 300))
                            intonation_score = int(max(50, 80 - (distance_from_good / 5)))
                    else:
                        # Outside typical ranges but still potentially valid
                        if pitch_mean < 80:
                            # Too low
                            intonation_score = int(max(10, 50 - (80 - pitch_mean) / 2))
                        else:
                            # Too high
                            intonation_score = int(max(10, 50 - (pitch_mean - 400) / 10))
        
        # Ensure score is within valid range
        intonation_score = max(0, min(100, intonation_score))
        
        # Set status text based on the calculated score if not already set
        if intonation == "Không thể phân tích ngữ điệu" or intonation.startswith("Không thể phân tích ngữ điệu ("):
            if intonation_score >= 90:
                intonation = "Ngữ điệu xuất sắc"
            elif intonation_score >= 75:
                intonation = "Ngữ điệu tự nhiên"
            elif intonation_score >= 50:
                intonation = "Ngữ điệu chấp nhận được"
            else:
                intonation = "Cần điều chỉnh cao độ"
        
        # Add pitch contour data for visualization
        user_pitch_data = user_pitch_contour.tolist() if user_pitch_contour is not None and len(user_pitch_contour) > 0 else []
        sample_pitch_data = sample_pitch_contour.tolist() if sample_pitch_contour is not None and len(sample_pitch_contour) > 0 else []

        # Formant analysis with Parselmouth
        try:
            snd = parselmouth.Sound(np.array(user_y), sr)
            formants = snd.to_formant_burg()
            f1_values = [formants.get_value_at_time(1, t) for t in formants.ts() if not np.isnan(formants.get_value_at_time(1, t))]
            f1_mean = np.mean(f1_values) if f1_values else 500
            
            # Get sample formants if sample audio is available
            sample_f1_mean = None
            sample_f1_values = []
            if sample_y is not None and len(sample_y) > 0:
                try:
                    sample_snd = parselmouth.Sound(np.array(sample_y), sr)
                    sample_formants = sample_snd.to_formant_burg()
                    sample_f1_values = [sample_formants.get_value_at_time(1, t) for t in sample_formants.ts() 
                                       if not np.isnan(sample_formants.get_value_at_time(1, t))]
                    sample_f1_mean = np.mean(sample_f1_values) if sample_f1_values else None
                except Exception as e:
                    sample_f1_mean = None
                    sample_f1_values = []
            
            # Calculate clarity score based on sample if available
            if sample_f1_mean is not None and sample_f1_mean > 0:
                # Calculate the difference ratio between user and sample F1
                f1_diff_ratio = min(1.0, abs(f1_mean - sample_f1_mean) / max(sample_f1_mean, 1))
                
                # Score based on similarity to sample
                clarity_score = int(100 * (1 - f1_diff_ratio))
                
                # Apply limits to ensure reasonable scores
                if clarity_score < 30:
                    # Even with poor match, give minimum baseline score
                    clarity_score = max(30, clarity_score)
            else:
                # Use typical optimal ranges for Japanese vowels
                # Optimal range for F1 in Japanese is ~450-650Hz with ideal at 550Hz
                if 450 <= f1_mean <= 650:
                    # Calculate distance from ideal as a percentage of the acceptable range
                    f1_ideal = 550
                    max_distance = 100  # Distance from ideal to edge
                    f1_distance = abs(f1_mean - f1_ideal)
                    
                    # Score decreases linearly with distance from ideal
                    clarity_score = int(100 * (1 - f1_distance / max_distance))
                else:
                    # Outside optimal range - score decreases more rapidly
                    if f1_mean < 450:
                        # Too low - calculate how far below minimum
                        below_amount = 450 - f1_mean
                        # Score drops more quickly the further below
                        clarity_score = max(0, int(70 - below_amount / 10))
                    else:
                        # Too high - calculate how far above maximum
                        above_amount = f1_mean - 650
                        # Score drops more quickly the further above
                        clarity_score = max(0, int(70 - above_amount / 15))
                
            # Ensure score is within valid range
            clarity_score = max(0, min(100, clarity_score))
            
        except Exception as e:
            f1_mean = 500
            clarity_score = 50
            f1_values = []
            sample_f1_values = []
        
        # Set clarity status text based on the calculated score
        if clarity_score >= 90:
            clarity = "Độ rõ xuất sắc"
        elif clarity_score >= 75:
            clarity = "Độ rõ tốt"
        elif clarity_score >= 50:
            clarity = "Độ rõ chấp nhận được"
        else:
            # Provide more actionable feedback based on F1 value
            if f1_mean < 450:
                clarity = "Cần mở rộng miệng hơn khi phát âm"
            elif f1_mean > 650:
                clarity = "Cần điều chỉnh vị trí lưỡi khi phát âm"
            else:
                clarity = "Cần cải thiện độ rõ khi phát âm"
        
        # ===== TEXT COMPARISON WITH ENHANCED TOKENIZATION =====
        
        # Get complete comparison structure with original sentence structure
        comparison_result = await compare_words_with_structure(reference_text, transcription)
        
        # Use the hybrid comparison result for text scoring
        hybrid_result = await compare_texts_hybrid(reference_text, transcription)
        text_score = int(hybrid_result['hybrid_similarity'] * 100)
        has_text_match = text_score > 0
        
        # Reset intonation and clarity scores if there's no text match
        if not has_text_match:
            intonation_score = 0
            intonation = "Không thể phân tích ngữ điệu (không khớp văn bản)"
            clarity_score = 0
            clarity = "Không thể phân tích độ rõ (không khớp văn bản)"

        # ===== FINAL SCORE CALCULATION =====

        try:
            # If text score is 0 (no match), the overall score should be zero
            if text_score == 0:
                score = 0
            else:
                # Regular score calculation when there's some text match
                # Calculate the final score based on the component scores - without rhythm
                weighted_score = (text_score * 0.7) + (intonation_score * 0.15) + (clarity_score * 0.15)
                
                # Allow exceptional performance to reach 100
                if text_score >= 95 and intonation_score >= 95 and clarity_score >= 95:
                    # Bonus for excellent performance across all metrics
                    bonus_factor = min(1.0, ((text_score + intonation_score + clarity_score) / 300) * 1.05)
                    score = int(weighted_score * bonus_factor)
                else:
                    score = int(weighted_score)
                
                # Ensure score is within valid range
                score = max(0, min(100, score))
            
        except Exception as e:
            score = 0
        
        # Generate AI-powered feedback (this will be the main feedback)
        ai_feedback = await generate_personalized_feedback(
            comparison_result["enhanced_words"], 
            reference_text, 
            transcription, 
            comparison_result["llm_analysis"],
            score,
            intonation_score,
            clarity_score,
            text_score
        )
        
        # Create optimized result structure
        result = {
            # Core analysis results
            "score": int(score),
            "feedback": ai_feedback,  # Use AI-generated feedback as main feedback
            "transcription": transcription,
            
            # Component analysis
            "analysis": {
                "intonation": {
                    "status": intonation,
                    "score": float(intonation_score)
                },
                "clarity": {
                    "status": clarity,
                    "score": float(clarity_score)
                },
                "text": {
                    "score": float(text_score),
                    "similarity_ratio": float(comparison_result["token_comparison"]["similarity_ratio"])
                }
            },
            
            # Raw measurements
            "measurements": {
                "pitchMean": float(pitch_mean),
                "f1Mean": float(f1_mean)
            },
            
            # Text analysis results
            "textAnalysis": {
                "originalStructure": comparison_result["original_structure"],
                "enhancedWords": comparison_result["enhanced_words"],
                "tokenComparison": {
                    "matched_tokens": comparison_result["token_comparison"]["matched_tokens"],
                    "original_tokens": comparison_result["token_comparison"]["original_tokens"],
                    "transcription_tokens": comparison_result["token_comparison"]["transcription_tokens"]
                },
                "llmAnalysis": comparison_result["llm_analysis"]
            }
        }
        
        if user_audio_path and os.path.exists(user_audio_path):
            os.unlink(user_audio_path)
            
        return result
        
    except Exception as e:
        if user_audio_path and os.path.exists(user_audio_path):
            try:
                os.unlink(user_audio_path)
            except:
                pass
        logger.error(f"Error in analyze_audio_enhanced: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Audio analysis failed: {str(e)}")

@app.post("/analyze-audio-enhanced", response_class=JSONResponse)
async def analyze_enhanced_endpoint(
    file: UploadFile = File(...),
    reference_text: str = Form(...),
    type: str = Form(None)
):
    try:
        content_type = file.content_type or ""
        if not content_type.startswith("audio/"):
            logger.warning(f"Suspicious content type: {content_type}, filename: {file.filename}")
        
        try:
            file_size = len(await file.read())
            await file.seek(0)
            if file_size < 1000:
                return JSONResponse(
                    status_code=400,
                    content={"detail": "File âm thanh quá nhỏ hoặc rỗng"}
                )
        except Exception as e:
            logger.error(f"Error checking file size: {str(e)}")
        
        result = await analyze_audio_enhanced(file, reference_text, type)
        return JSONResponse(content=result)
    except HTTPException as e:
        return JSONResponse(
            status_code=e.status_code,
            content={"detail": str(e.detail)}
        )
    except Exception as e:
        if DEBUG_MODE:
            return JSONResponse(
                status_code=500,
                content={
                    "detail": f"Enhanced analysis error: {str(e)}",
                    "traceback": traceback.format_exc()
                }
            )
        else:
            return JSONResponse(
                status_code=500,
                content={"detail": f"Enhanced analysis error: {str(e)}"}
            )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content={"detail": "Validation error", "errors": exc.errors()},
    )

@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    if DEBUG_MODE:
        return JSONResponse(
            status_code=500,
            content={
                "detail": f"Internal server error: {str(exc)}",
                "traceback": traceback.format_exc()
            },
        )
    else:
        return JSONResponse(
            status_code=500,
            content={"detail": "Internal server error - enable DEBUG_MODE for details"},
        )

async def normalize_width(text: str) -> str:
    """Normalize full-width and half-width characters using mojimoji."""
    try:
        # Convert half-width characters to full-width for Japanese text consistency
        return mojimoji.han_to_zen(text)
    except Exception as e:
        logger.warning(f"Error in mojimoji normalization: {e}, falling back to original text")
        return text

async def process_text_to_tokens(text: str) -> list:
    """
    Process text and return array of hiragana tokens instead of joined string.
    This preserves word boundaries for better comparison.
    """
    logger.info(f"Processing text to tokens: '{text}'")
    
    # Step 1: Strip whitespace
    text = text.strip()
    logger.info(f"After stripping: '{text}'")
    
    # Step 2: Normalize character width (full-width/half-width)
    text = await normalize_width(text)
    logger.info(f"After width normalization: '{text}'")
    
    # Step 3: Tokenize first to preserve semantic meaning
    tokens = await tokenize_japanese(text)
    logger.info(f"After tokenization: {tokens}")
    
    # Step 4: Clean each token individually to preserve word boundaries
    cleaned_tokens = []
    for token in tokens:
        cleaned_token = await clean_text(token)
        if cleaned_token.strip():  # Only keep non-empty tokens
            cleaned_tokens.append(cleaned_token)
    logger.info(f"After cleaning tokens: {cleaned_tokens}")
    
    # Step 5: Convert each cleaned token to hiragana
    hiragana_tokens = []
    for token in cleaned_tokens:
        hiragana_token = await to_hiragana(token)
        hiragana_tokens.append(hiragana_token)
    logger.info(f"Final hiragana tokens: {hiragana_tokens}")
    
    return hiragana_tokens

async def compare_words_enhanced(original: str, transcription: str) -> tuple:
    """Enhanced word comparison using hiragana normalization and SequenceMatcher algorithm."""
    
    # Get tokenized versions for comparison
    original_tokens = await process_text_to_tokens(original)
    transcription_tokens = await process_text_to_tokens(transcription)
    
    # Use token-level comparison instead of the old SequenceMatcher
    token_comparison = await compare_token_arrays(original_tokens, transcription_tokens)
    
    # Also get LLM analysis for additional insights (but don't use suggestions)
    llm_analysis = await analyze_with_llm(original, transcription)
    auxiliary_words = set(llm_analysis.get("auxiliary_words", []))
    
    # Create enhanced words based on token comparison only
    enhanced_words = []
    matched_tokens = set(token_comparison["matched_tokens"])
    
    for token in original_tokens:
        is_correct = token in matched_tokens
        
        # Don't use LLM suggestions - let generate_personalized_feedback handle this
        enhanced_words.append({
            "text": token,
            "isCorrect": is_correct,
            "suggestion": None  # Always None, feedback will be generated separately
        })
    
    # Create hiragana mapping for compatibility
    orig_hiragana = {}
    for token in original_tokens:
        orig_hiragana[token] = token  # Already in hiragana from process_text_to_tokens
    
    # Add token comparison metrics to LLM analysis (but remove suggestions)
    llm_analysis["token_similarity_ratio"] = token_comparison["similarity_ratio"]
    llm_analysis["matched_tokens"] = token_comparison["matched_tokens"]
    # Remove personalized_feedback to force using generate_personalized_feedback
    llm_analysis["personalized_feedback"] = ""
    
    logger.info(f"Token similarity ratio: {token_comparison['similarity_ratio']:.3f}")
    logger.info(f"Original tokens: {token_comparison['original_tokens']}")
    logger.info(f"Transcription tokens: {token_comparison['transcription_tokens']}")
    
    return enhanced_words, orig_hiragana, llm_analysis

async def compare_token_arrays(original_tokens: list, transcription_tokens: list) -> dict:
    """
    Compare two arrays of tokens using SequenceMatcher.
    This provides more accurate comparison than string-based comparison.
    """
    logger.info(f"Comparing token arrays:")
    logger.info(f"  Original: {original_tokens}")
    logger.info(f"  Transcription: {transcription_tokens}")
    
    # Use SequenceMatcher on token arrays
    matcher = difflib.SequenceMatcher(None, original_tokens, transcription_tokens)
    similarity_ratio = matcher.ratio()
    
    # Get matching blocks for detailed analysis
    matching_blocks = matcher.get_matching_blocks()
    
    # Calculate matched tokens
    matched_tokens = []
    for match in matching_blocks:
        if match.size > 0:  # Ignore the final dummy block
            matched_tokens.extend(original_tokens[match.a:match.a + match.size])
    
    # Get opcodes for detailed diff analysis
    opcodes = matcher.get_opcodes()
    
    result = {
        "similarity_ratio": similarity_ratio,
        "original_tokens": original_tokens,
        "transcription_tokens": transcription_tokens,
        "matched_tokens": matched_tokens,
        "matching_blocks": matching_blocks,
        "opcodes": opcodes,
        "total_original_tokens": len(original_tokens),
        "total_transcription_tokens": len(transcription_tokens),
        "total_matched_tokens": len(matched_tokens)
    }
    
    logger.info(f"Similarity ratio: {similarity_ratio:.3f}")
    logger.info(f"Matched tokens: {matched_tokens}")
    
    return result

async def compare_texts_hybrid(original: str, transcription: str) -> dict:
    """
    Hybrid comparison that combines token-level and character-level analysis.
    This provides more accurate results for Japanese text comparison.
    """
    logger.info(f"Hybrid comparison: '{original}' vs '{transcription}'")
    
    # Get tokens for both texts
    original_tokens = await process_text_to_tokens(original)
    transcription_tokens = await process_text_to_tokens(transcription)
    
    # Method 1: Token-level comparison (for structural similarity)
    token_result = await compare_token_arrays(original_tokens, transcription_tokens)
    token_similarity = token_result['similarity_ratio']
    
    # Method 2: Character-level comparison (for phonetic similarity)
    original_string = ''.join(original_tokens)
    transcription_string = ''.join(transcription_tokens)
    
    char_matcher = difflib.SequenceMatcher(None, original_string, transcription_string)
    char_similarity = char_matcher.ratio()
    
    # Method 3: Individual token similarity (for partial matches)
    token_similarities = []
    max_tokens = max(len(original_tokens), len(transcription_tokens))
    
    if max_tokens > 0:
        for i in range(max_tokens):
            orig_token = original_tokens[i] if i < len(original_tokens) else ""
            trans_token = transcription_tokens[i] if i < len(transcription_tokens) else ""
            
            if orig_token and trans_token:
                token_sim = difflib.SequenceMatcher(None, orig_token, trans_token).ratio()
                token_similarities.append(token_sim)
            else:
                token_similarities.append(0.0)
    
    avg_token_similarity = sum(token_similarities) / len(token_similarities) if token_similarities else 0.0
    
    # Weighted combination of all methods
    # Token structure: 40%, Character similarity: 40%, Individual tokens: 20%
    hybrid_similarity = (
        token_similarity * 0.4 + 
        char_similarity * 0.4 + 
        avg_token_similarity * 0.2
    )
    
    result = {
        "hybrid_similarity": hybrid_similarity,
        "token_similarity": token_similarity,
        "character_similarity": char_similarity,
        "average_token_similarity": avg_token_similarity,
        "original_tokens": original_tokens,
        "transcription_tokens": transcription_tokens,
        "original_string": original_string,
        "transcription_string": transcription_string,
        "individual_token_similarities": token_similarities,
        "analysis": {
            "structural_match": token_similarity > 0.8,
            "phonetic_match": char_similarity > 0.8,
            "partial_match": avg_token_similarity > 0.6,
            "overall_match": hybrid_similarity > 0.7
        }
    }
    
    logger.info(f"Hybrid similarity: {hybrid_similarity:.3f}")
    logger.info(f"  Token: {token_similarity:.3f}, Char: {char_similarity:.3f}, Avg Token: {avg_token_similarity:.3f}")
    
    return result

async def get_original_sentence_structure(original: str) -> dict:
    """
    Get detailed structure of the original sentence with tokenization info.
    Returns original tokens with their hiragana conversions and positions.
    """
    logger.info(f"Getting original sentence structure for: '{original}'")
    
    # Step 1: Get original tokens (before any processing)
    original_tokens = await tokenize_japanese(original)
    
    # Step 2: Create detailed structure for each token
    sentence_structure = []
    current_position = 0
    
    for i, token in enumerate(original_tokens):
        # Get hiragana conversion for this token
        hiragana = await to_hiragana(token)
        
        # Find position in original text
        token_start = original.find(token, current_position)
        token_end = token_start + len(token) if token_start != -1 else current_position + len(token)
        
        token_info = {
            "index": i,
            "original": token,
            "hiragana": hiragana,
            "position": {
                "start": token_start,
                "end": token_end
            },
            "isCorrect": True,  # Default, will be updated during comparison
            "suggestion": None  # No default suggestions
        }
        
        sentence_structure.append(token_info)
        current_position = token_end
    
    return {
        "original_text": original,
        "tokens": sentence_structure,
        "total_tokens": len(original_tokens)
    }

async def compare_words_with_structure(original: str, transcription: str) -> dict:
    """
    Enhanced comparison that returns both word analysis and original sentence structure.
    """
    logger.info(f"Comparing with structure: '{original}' vs '{transcription}'")
    
    # Get original sentence structure
    original_structure = await get_original_sentence_structure(original)
    
    # Get enhanced word comparison
    enhanced_words, orig_hiragana, llm_analysis = await compare_words_enhanced(original, transcription)
    
    # Get detailed token comparison
    original_tokens = await process_text_to_tokens(original)
    transcription_tokens = await process_text_to_tokens(transcription)
    token_comparison = await compare_token_arrays(original_tokens, transcription_tokens)
    
    # Update correctness in original structure based on comparison
    # Map enhanced_words back to original structure
    enhanced_word_map = {word["text"]: word for word in enhanced_words}
    
    for token_info in original_structure["tokens"]:
        hiragana_token = token_info["hiragana"]
        if hiragana_token in enhanced_word_map:
            word_result = enhanced_word_map[hiragana_token]
            token_info["isCorrect"] = word_result["isCorrect"]
            token_info["suggestion"] = None  # Don't use LLM suggestions
        else:
            # Check if this token appears in any of the enhanced words
            found_match = False
            for word in enhanced_words:
                if hiragana_token in word["text"] or word["text"] in hiragana_token:
                    token_info["isCorrect"] = word["isCorrect"]
                    token_info["suggestion"] = None  # Don't use LLM suggestions
                    found_match = True
                    break
            
            if not found_match:
                # Token not found in transcription - mark as incorrect
                token_info["isCorrect"] = False
                token_info["suggestion"] = None  # Don't use LLM suggestions
    
    return {
        "original_structure": original_structure,
        "enhanced_words": enhanced_words,
        "hiragana_mapping": orig_hiragana,
        "llm_analysis": llm_analysis,
        "token_comparison": {
            "similarity_ratio": token_comparison["similarity_ratio"],
            "original_tokens": token_comparison["original_tokens"],
            "transcription_tokens": token_comparison["transcription_tokens"],
            "matched_tokens": token_comparison["matched_tokens"]
        },
        "transcription_text": transcription
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)