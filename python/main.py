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
import aubio
import numpy as np
from scipy.spatial.distance import euclidean
from fastdtw import fastdtw
from string import punctuation
import soundfile as sf
import tempfile
from dotenv import load_dotenv
import pykakasi
import re
from typing import Optional, Dict, List, Any, Union

# Load environment variables from .env file
load_dotenv()

# Debug mode (set to True for detailed error responses)
DEBUG_MODE = os.getenv("DEBUG_MODE", "False").lower() in ("true", "1", "t")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")

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

# Khởi tạo pykakasi cho tokenization tiếng Nhật
kakasi = pykakasi.kakasi()

# Regex để loại bỏ dấu câu và ký tự đặc biệt tiếng Nhật
JAPANESE_PUNCTUATION = '、。！？；：「」『』・〜'
CLEAN_REGEX = re.compile(f'[{re.escape(punctuation + JAPANESE_PUNCTUATION)}]|[^\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF\sA-Za-z0-9]')

# Flag for Kaldi availability (will be simulated since not installed)
KALDI_AVAILABLE = False

async def count_syllables(text: str) -> int:
    """Đếm số âm tiết dựa trên hiragana."""
    if not text or not text.strip():
        return 0
        
    # Loại bỏ dấu câu
    text = re.sub(r'[、。！？,.!?]', '', text)
    
    try:
        # Chuyển sang hiragana
        hira_text = await to_hiragana(text)
        
        # Quy tắc đếm âm tiết trong tiếng Nhật:
        # 1. Mỗi kana (hiragana/katakana) là một âm tiết
        # 2. Trừ các kí tự đặc biệt (ゃ, ゅ, ょ, っ) không được tính riêng
        # 3. Các chữ kanji được tính dựa trên cách đọc (đã được chuyển sang hiragana)
        special_chars = {'ゃ', 'ゅ', 'ょ', 'ッ', 'っ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ'}
        
        # Đếm số âm tiết
        syllables = 0
        skip_next = False
        for i, char in enumerate(hira_text):
            if skip_next:
                skip_next = False
                continue
                
            if char in special_chars:
                continue
                
            # Kiểm tra nếu kí tự hiện tại là n (ん) cuối câu hoặc trước phụ âm (không phải y, w)
            if char == 'ん' and i < len(hira_text) - 1:
                next_char = hira_text[i+1]
                # ん không phải một âm tiết riêng nếu nó đứng trước các phụ âm thuộc nhóm n, m, p, b (na, ma, pa, ba...)
                if next_char in {'な', 'に', 'ぬ', 'ね', 'の', 'ま', 'み', 'む', 'め', 'も', 'ぱ', 'ぴ', 'ぷ', 'ぺ', 'ぽ', 'ば', 'び', 'ぶ', 'べ', 'ぼ'}:
                    skip_next = True
                    
            syllables += 1
            
        return max(1, syllables)  # Ensure at least 1 syllable
    except Exception as e:
        # Fallback: rough estimate based on character count
        return max(1, len(text.strip()) // 3)

async def to_hiragana(text: str) -> str:
    """Convert text to hiragana for normalization."""
    try:
        result = kakasi.convert(text)
        return ''.join(item['hira'] for item in result if 'hira' in item)
    except Exception as e:
        return text

async def clean_text(text: str) -> str:
    """Loại bỏ dấu câu và ký tự đặc biệt."""
    return CLEAN_REGEX.sub('', text)

async def tokenize_japanese(text: str) -> List[str]:
    """Chia câu tiếng Nhật thành danh sách từ."""
    cleaned_text = await clean_text(text)
    if not cleaned_text:
        return []
    
    result = kakasi.convert(cleaned_text)
    words = []
    
    for item in result:
        if 'orig' in item and item['orig'].strip():
            words.append(item['orig'])
    
    return words

async def analyze_with_llm(original: str, transcription: str, phoneme_errors: List[Dict[str, str]] = None) -> Dict[str, Any]:
    """Use LLM to analyze semantic differences and identify auxiliary words with phoneme error details."""
    if not OPENAI_API_KEY:
        return {
            "incorrect_words": [],
            "auxiliary_words": ["ね", "よ", "な", "わ", "さ"],  # Common auxiliary particles
            "personalized_feedback": "Cần cải thiện phát âm của một số từ. Hãy luyện tập thêm nhé!"
        }
    
    try:
        # Prepare phoneme error information for LLM
        phoneme_info = ""
        if phoneme_errors and len(phoneme_errors) > 0:
            phoneme_info = "Danh sách lỗi phoneme:\n"
            for error in phoneme_errors:
                if "error_type" in error and "phonetic_error" in error:
                    phoneme_info += f"- {error['phonetic_error']} (loại: {error['error_type']})\n"
                else:
                    phoneme_info += f"- Vị trí {error.get('index', '?')}: Mong muốn '{error.get('expected', '?')}', thực tế '{error.get('actual', '?')}'\n"

        prompt = f"""
Analyze these two Japanese sentences:
- Original: '{original}'
- Transcription: '{transcription}'

{phoneme_info}

Identify:
1. Words that are incorrect or different
2. Auxiliary words/particles (like ね, よ) that don't affect the core meaning
3. Brief pronunciation suggestions in Vietnamese, incorporating phoneme error details when relevant

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
            model="gpt-3.5-turbo",  # Can use gpt-4 for better results if available
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

async def get_expected_phonemes(text: str) -> List[str]:
    """Get expected phonemes from text (simulated using hiragana)."""
    result = kakasi.convert(text)
    phonemes = []
    for item in result:
        hira = item['hira']
        phonemes.extend(list(hira))  # Each hiragana character is treated as a phoneme (simplified)
    return phonemes

async def simulate_phoneme_analysis(audio_path: str, original_text: str, transcription: str) -> List[Dict[str, str]]:
    """Simulate phoneme analysis based on transcription and original text with improved phonetic awareness."""
    
    # Chuyển văn bản gốc và transcription về hiragana
    orig_hira = await to_hiragana(original_text)
    trans_hira = await to_hiragana(transcription)
    
    phoneme_errors = []
    
    # Thực hiện so sánh theo chuỗi phoneme với các đặc điểm ngữ âm
    # Sử dụng giải thuật Longest Common Subsequence (LCS) để căn chỉnh
    m, n = len(orig_hira), len(trans_hira)
    
    # Tạo ma trận LCS
    lcs = [[0 for _ in range(n+1)] for _ in range(m+1)]
    for i in range(1, m+1):
        for j in range(1, n+1):
            if orig_hira[i-1] == trans_hira[j-1]:
                lcs[i][j] = lcs[i-1][j-1] + 1
            else:
                lcs[i][j] = max(lcs[i-1][j], lcs[i][j-1])
    
    # Lấy chuỗi căn chỉnh từ ma trận LCS
    i, j = m, n
    alignment = []
    
    while i > 0 and j > 0:
        if orig_hira[i-1] == trans_hira[j-1]:
            alignment.append((i-1, j-1, orig_hira[i-1], trans_hira[j-1], True))  # Match
            i -= 1
            j -= 1
        elif lcs[i-1][j] >= lcs[i][j-1]:
            alignment.append((i-1, None, orig_hira[i-1], None, False))  # Deletion
            i -= 1
        else:
            alignment.append((None, j-1, None, trans_hira[j-1], False))  # Insertion
            j -= 1
            
    # Xử lý phần đầu còn lại
    while i > 0:
        alignment.append((i-1, None, orig_hira[i-1], None, False))
        i -= 1
        
    while j > 0:
        alignment.append((None, j-1, None, trans_hira[j-1], False))
        j -= 1
        
    # Đảo ngược alignment để có thứ tự từ đầu đến cuối
    alignment.reverse()
    
    # Tạo danh sách các lỗi phoneme
    for idx, (orig_idx, trans_idx, orig_ph, trans_ph, is_match) in enumerate(alignment):
        if not is_match:
            # Xác định loại lỗi
            if orig_ph is None:
                # Thêm phoneme (trong transcription mà không có trong bản gốc)
                phoneme_errors.append({
                    "index": idx,
                    "expected": "none",
                    "actual": trans_ph,
                    "error_type": "addition"
                })
            elif trans_ph is None:
                # Thiếu phoneme (trong bản gốc nhưng không có trong transcription)
                phoneme_errors.append({
                    "index": idx,
                    "expected": orig_ph,
                    "actual": "missing",
                    "error_type": "omission"
                })
            else:
                # Thay thế phoneme (khác nhau giữa bản gốc và transcription)
                phoneme_errors.append({
                    "index": idx,
                    "expected": orig_ph,
                    "actual": trans_ph,
                    "error_type": "substitution",
                    # Phân loại cặp nhầm lẫn phoneme dựa trên các đặc điểm ngữ âm
                    "phonetic_error": categorize_phonetic_error(orig_ph, trans_ph)
                })
    
    return phoneme_errors

def categorize_phonetic_error(expected: str, actual: str) -> str:
    """Phân loại lỗi phoneme dựa trên đặc điểm ngữ âm."""
    # Các nhóm phoneme tiếng Nhật dễ nhầm lẫn
    similar_groups = {
        "vowel_confusion": [
            {"a": "あ", "i": "い", "u": "う", "e": "え", "o": "お"},
        ],
        "consonant_pairs": [
            {"k-g": ["か", "が"], "s-z": ["さ", "ざ"], "t-d": ["た", "だ"], "h-b": ["は", "ば"], "h-p": ["は", "ぱ"]},
        ],
        "small_ya_yu_yo": [
            {"ya": "ゃ", "yu": "ゅ", "yo": "ょ"},
        ],
        "long_short_vowel": [
            {"a": "あ", "aa": "ああ"}, {"i": "い", "ii": "いい"}, 
            {"u": "う", "uu": "うう"}, {"e": "え", "ee": "ええ"}, 
            {"o": "お", "oo": "おお"},
        ],
        "n_confusion": [
            {"n": "ん"},
        ],
        "tsu_confusion": [
            {"tsu": "つ", "small_tsu": "っ"},
        ]
    }
    
    # Xác định lỗi dựa trên so sánh phoneme
    if expected == "し" and actual == "す" or expected == "す" and actual == "し":
        return "Nhầm lẫn giữa 'shi' và 'su'"
    elif expected in "しゃしゅしょ" and actual in "しゃしゅしょ" and expected != actual:
        return f"Nhầm lẫn trong nhóm 'sha/shu/sho': '{expected}' và '{actual}'"
    elif expected in "ちゃちゅちょ" and actual in "ちゃちゅちょ" and expected != actual:
        return f"Nhầm lẫn trong nhóm 'cha/chu/cho': '{expected}' và '{actual}'"
    elif expected in "じゃじゅじょ" and actual in "じゃじゅじょ" and expected != actual:
        return f"Nhầm lẫn trong nhóm 'ja/ju/jo': '{expected}' và '{actual}'"
    elif expected == "つ" and actual == "す" or expected == "す" and actual == "つ":
        return "Nhầm lẫn giữa 'tsu' và 'su'"
    elif expected == "ふ" and actual == "は" or expected == "は" and actual == "ふ":
        return "Nhầm lẫn giữa 'fu' và 'ha'"
    elif expected == "お" and actual == "を" or expected == "を" and actual == "お":
        return "Nhầm lẫn giữa 'o' và 'wo'"
    elif expected == "へ" and actual == "え" or expected == "え" and actual == "へ":
        return "Nhầm lẫn giữa 'he' và 'e'"
    elif "ゃ" in expected and "ゃ" not in actual:
        return "Thiếu âm 'ya' nhỏ (ゃ)"
    elif "ゅ" in expected and "ゅ" not in actual:
        return "Thiếu âm 'yu' nhỏ (ゅ)"
    elif "ょ" in expected and "ょ" not in actual:
        return "Thiếu âm 'yo' nhỏ (ょ)"
    elif "っ" in expected and "っ" not in actual:
        return "Thiếu âm 'tsu' nhỏ (っ)"
    elif "ん" in expected and "ん" not in actual:
        return "Thiếu âm 'n' (ん)"
    else:
        return f"Phát âm sai: Mong muốn '{expected}', đã phát âm '{actual}'"

async def combine_phoneme_errors_with_words(words: List[Dict], 
                                          phoneme_errors: List[Dict],
                                          hiragana_map: Dict[str, str]) -> List[Dict]:
    """Combine phoneme error information with word analysis results."""
    if not phoneme_errors:
        return words
    
    # Build phoneme to word mapping
    word_phoneme_map = {}
    current_pos = 0
    
    for word, hira in hiragana_map.items():
        word_phoneme_map[word] = {
            "start": current_pos,
            "end": current_pos + len(hira) - 1,
            "hiragana": hira
        }
        current_pos += len(hira)
    
    # Process each phoneme error and associate with words
    for error in phoneme_errors:
        idx = error.get("index", -1)
        if idx < 0:
            continue
            
        # Find which word contains this phoneme position
        for word in words:
            word_text = word["text"]
            if word_text not in word_phoneme_map:
                continue
                
            word_range = word_phoneme_map[word_text]
            if word_range["start"] <= idx <= word_range["end"]:
                # This word contains the phoneme error
                error_details = ""
                
                # Get specific error details based on error type
                if "error_type" in error and "phonetic_error" in error:
                    error_details = error["phonetic_error"]
                else:
                    expected = error.get("expected", "?")
                    actual = error.get("actual", "?")
                    if actual == "missing":
                        error_details = f"Thiếu âm '{expected}'"
                    elif expected == "none":
                        error_details = f"Thêm âm thừa '{actual}'"
                    else:
                        error_details = f"Phát âm '{expected}' thành '{actual}'"
                
                # Enhance suggestion with phoneme error info
                base_suggestion = word.get("suggestion", "")
                if base_suggestion:
                    if error_details not in base_suggestion:  # Avoid duplication
                        word["suggestion"] = f"{error_details}. {base_suggestion}"
                else:
                    word["suggestion"] = f"{error_details}. Hãy luyện tập phát âm '{word_text}' chuẩn hơn."
                
                # Mark as incorrect
                word["isCorrect"] = False
                break
    
    return words

async def generate_personalized_feedback(words: List[Dict], 
                                       sentence: str, 
                                       transcription: str, 
                                       llm_result: Dict) -> str:
    """Generate personalized feedback based on LLM analysis."""
    
    # If LLM already provided personalized feedback, use it
    if "personalized_feedback" in llm_result and llm_result["personalized_feedback"]:
        return llm_result["personalized_feedback"]
        
    # Otherwise, build a simple feedback message
    incorrect_words = [w["text"] for w in words if not w.get("isCorrect", True)]
    auxiliary_words = llm_result.get("auxiliary_words", [])
    
    if not incorrect_words:
        return "Phát âm rất tốt! Tiếp tục luyện tập để duy trì khả năng phát âm tốt nhé!"
    elif len(incorrect_words) == 1:
        return f"Phát âm của bạn khá tốt. Chỉ cần chú ý cách phát âm từ '{incorrect_words[0]}' là hoàn hảo!"
    else:
        words_to_improve = ", ".join([f"'{w}'" for w in incorrect_words[:3]])
        return f"Hãy tập trung vào cách phát âm các từ: {words_to_improve}. Tiếp tục luyện tập nhé!"

async def compare_words(original: str, transcription: str) -> List[Dict]:
    """Basic word comparison between original and transcribed text."""
    
    orig_words = await tokenize_japanese(original)
    trans_words = await tokenize_japanese(transcription)
    
    orig_hiragana = {}
    for word in orig_words:
        orig_hiragana[word] = await to_hiragana(word)
    
    trans_hiragana = {}
    for word in trans_words:
        trans_hiragana[word] = await to_hiragana(word)
    
    result_words = []
    
    for i, orig_word in enumerate(orig_words):
        orig_hira = orig_hiragana[orig_word]
        
        found = False
        for trans_word, trans_hira in trans_hiragana.items():
            if orig_hira == trans_hira:
                found = True
                break
        
        result_words.append({
            "text": orig_word,
            "isCorrect": found,
            "suggestion": None if found else f"Cần chú ý phát âm '{orig_word}' rõ ràng hơn"
        })
    
    return result_words

async def compare_words_enhanced(original: str, transcription: str) -> tuple:
    """Enhanced word comparison using hiragana normalization."""
    orig_words = await tokenize_japanese(original)
    trans_words = await tokenize_japanese(transcription)
    
    orig_hiragana = {}
    for word in orig_words:
        orig_hiragana[word] = await to_hiragana(word)
    
    trans_hiragana = {}
    for word in trans_words:
        trans_hiragana[word] = await to_hiragana(word)
    
    llm_analysis = await analyze_with_llm(original, transcription)
    auxiliary_words = set(llm_analysis.get("auxiliary_words", []))
    incorrect_map = {item["word"]: item for item in llm_analysis.get("incorrect_words", [])}
    
    result_words = []
    i, j = 0, 0
    
    while i < len(orig_words):
        orig_word = orig_words[i]
        orig_hira = orig_hiragana[orig_word]
        
        if j >= len(trans_words):
            suggestion = None
            if orig_word in incorrect_map:
                suggestion = incorrect_map[orig_word].get("suggestion")
            result_words.append({
                "text": orig_word, 
                "isCorrect": False, 
                "suggestion": suggestion or f"Từ này không được phát âm. Hãy chú ý phát âm '{orig_word}'."
            })
            i += 1
            continue
        
        trans_word = trans_words[j]
        trans_hira = trans_hiragana[trans_word]
        
        if orig_hira == trans_hira:
            result_words.append({
                "text": orig_word,
                "isCorrect": True,
                "suggestion": None
            })
            i += 1
            j += 1
        else:
            if trans_word in auxiliary_words:
                j += 1
                continue
            
            found_match = False
            for look_ahead in range(1, min(3, len(trans_words) - j)):
                if orig_hira == trans_hiragana[trans_words[j + look_ahead]]:
                    for k in range(look_ahead):
                        result_words.append({
                            "text": trans_words[j + k],
                            "isCorrect": False,
                            "suggestion": f"Từ này không cần thiết trong câu."
                        })
                    result_words.append({
                        "text": orig_word,
                        "isCorrect": True,
                        "suggestion": None
                    })
                    i += 1
                    j += look_ahead + 1
                    found_match = True
                    break
            
            if not found_match:
                suggestion = None
                if orig_word in incorrect_map:
                    suggestion = incorrect_map[orig_word].get("suggestion")
                result_words.append({
                    "text": orig_word,
                    "isCorrect": False,
                    "suggestion": suggestion or f"Cần phát âm rõ '{orig_word}'."
                })
                i += 1
                j += 1
    
    while j < len(trans_words):
        trans_word = trans_words[j]
        if trans_word in auxiliary_words:
            j += 1
            continue
        
        result_words.append({
            "text": trans_word,
            "isCorrect": False,
            "suggestion": f"Từ '{trans_word}' không cần thiết trong câu."
        })
        j += 1
    
    return result_words, orig_hiragana, llm_analysis

async def analyze_audio_features(user_y, sr, sample_y, sentence, transcription):
    """Analyze audio features and return basic analysis results with improved scoring."""
    try:
        
        # Check audio energy first to validate audio quality
        audio_rms = np.sqrt(np.mean(user_y**2))
        audio_energy = np.sum(user_y**2)
        
        # Initialize default values
        intonation_score = 0
        intonation = "Không thể phân tích ngữ điệu"
        pitch_mean = 0
        user_pitch_data = []
        sample_pitch_data = []
        
        # Check if audio has enough energy to analyze
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
                user_pitch_contour = None
                sample_pitch_contour = None
                
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
            else:
                intonation_score = 0
                intonation = "Không thể phân tích ngữ điệu (không phát hiện cao độ)"
        
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

        # Phân tích formant với Parselmouth
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
        
        # Set status text based on the calculated score
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
        
        # Phân tích nhịp điệu (REMOVED)
        rhythm_score = 0
        rhythm = "Không áp dụng"
        onsets = []
        expected_syllables = 0

        # So sánh văn bản
        try:
            # First check if we have valid transcription text
            if not transcription or transcription.strip() == "":
                text_score = 0
                has_text_match = False
            else:
                # Clean texts for comparison
                transcription_clean = await clean_text(transcription)
                original_clean = await clean_text(sentence)
                
                if len(original_clean) > 0 and len(transcription_clean) > 0:
                    # Calculate similarity using character-by-character comparison
                    common_length = min(len(transcription_clean), len(original_clean))
                    matches = sum(a == b for a, b in zip(transcription_clean[:common_length], original_clean[:common_length]))
                    
                    # Calculate additional penalty for length difference
                    length_diff = abs(len(transcription_clean) - len(original_clean))
                    max_length = max(len(transcription_clean), len(original_clean))
                    
                    # Calculate final similarity score with length penalty
                    if max_length > 0:
                        text_similarity = (matches / max_length) * (1 - 0.5 * (length_diff / max_length))
                        text_score = int(text_similarity * 100)
                        has_text_match = text_score > 0
                    else:
                        text_score = 0
                        has_text_match = False
                    
                else:
                    text_score = 0
                    has_text_match = False
                
        except Exception as e:
            text_score = 0
            has_text_match = False
        
        # Reset intonation and clarity scores if there's no text match
        if not has_text_match:
            intonation_score = 0
            intonation = "Không thể phân tích ngữ điệu (không khớp văn bản)"
            clarity_score = 0
            clarity = "Không thể phân tích độ rõ (không khớp văn bản)"

        # So sánh với âm thanh mẫu nếu có
        audio_similarity = 0
        # Removed audio similarity calculation here

        # Tính điểm cuối cùng với công thức cải tiến - without rhythm or audio
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
                
            # Generate appropriate feedback based on score
            if score == 0:
                feedback = "Không thể đánh giá - không nhận diện được văn bản khớp với câu mẫu."
            elif score < 10:
                feedback = "Hãy thử tập đọc câu mẫu và ghi âm lại."
            elif score < 30:
                feedback = "Cần cải thiện phát âm nhiều hơn."
            elif score < 50:
                feedback = "Phát âm trung bình, cần cải thiện nhiều mặt."
            elif score < 70:
                feedback = "Phát âm khá tốt, cần cải thiện một số chi tiết."
            elif score < 85:
                feedback = "Phát âm tốt!"
            elif score < 95:
                feedback = "Phát âm rất tốt!"
            else:
                feedback = "Phát âm xuất sắc!"
            
        except Exception as e:
            score = 0
            feedback = "Không thể tính điểm - hãy thử lại."

        return {
            "score": score,
            "feedback": feedback,
            "intonation": intonation,
            "clarity": clarity,
            "rhythm": rhythm,
            "transcription": transcription,
            "words": [],  # Will be filled by the enhanced analysis
            # Add component scores with precise values
            "intonationScore": intonation_score,
            "clarityScore": clarity_score,
            "rhythmScore": rhythm_score, 
            "textScore": text_score,
            # Add raw measurements for reference
            "pitchMean": round(float(pitch_mean), 2),
            "f1Mean": round(float(f1_mean), 2),
            # Include formula weights for frontend display
            "formulaWeights": {
                "withoutAudio": {
                    "text": 0.7,
                    "intonation": 0.15,
                    "clarity": 0.15
                }
            },
            "userPitchData": [],
            "samplePitchData": sample_pitch_data,
            # Add formant data for visualization
            "userFormantData": [],
            "sampleFormantData": sample_f1_values if sample_f1_values and len(sample_f1_values) > 0 else []
        }

    except Exception as e:
        f1_mean = 500
        clarity_score = 50
        f1_values = []
        sample_f1_values = []
        
        # Set status text based on the calculated score
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
        
        # Phân tích nhịp điệu (REMOVED)
        rhythm_score = 0
        rhythm = "Không áp dụng"
        onsets = []
        expected_syllables = 0

        # So sánh văn bản
        try:
            # First check if we have valid transcription text
            if not transcription or transcription.strip() == "":
                text_score = 0
                has_text_match = False
            else:
                # Clean texts for comparison
                transcription_clean = await clean_text(transcription)
                original_clean = await clean_text(sentence)
                
                if len(original_clean) > 0 and len(transcription_clean) > 0:
                    # Calculate similarity using character-by-character comparison
                    common_length = min(len(transcription_clean), len(original_clean))
                    matches = sum(a == b for a, b in zip(transcription_clean[:common_length], original_clean[:common_length]))
                    
                    # Calculate additional penalty for length difference
                    length_diff = abs(len(transcription_clean) - len(original_clean))
                    max_length = max(len(transcription_clean), len(original_clean))
                    
                    # Calculate final similarity score with length penalty
                    if max_length > 0:
                        text_similarity = (matches / max_length) * (1 - 0.5 * (length_diff / max_length))
                        text_score = int(text_similarity * 100)
                        has_text_match = text_score > 0
                    else:
                        text_score = 0
                        has_text_match = False
                    
                else:
                    text_score = 0
                    has_text_match = False
                
        except Exception as e:
            text_score = 0
            has_text_match = False
        
        # Reset intonation and clarity scores if there's no text match
        if not has_text_match:
            intonation_score = 0
            intonation = "Không thể phân tích ngữ điệu (không khớp văn bản)"
            clarity_score = 0
            clarity = "Không thể phân tích độ rõ (không khớp văn bản)"

        # So sánh với âm thanh mẫu nếu có
        audio_similarity = 0
        # Removed audio similarity calculation here

        # Tính điểm cuối cùng với công thức cải tiến - without rhythm or audio
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
                
            # Generate appropriate feedback based on score
            if score == 0:
                feedback = "Không thể đánh giá - không nhận diện được văn bản khớp với câu mẫu."
            elif score < 10:
                feedback = "Hãy thử tập đọc câu mẫu và ghi âm lại."
            elif score < 30:
                feedback = "Cần cải thiện phát âm nhiều hơn."
            elif score < 50:
                feedback = "Phát âm trung bình, cần cải thiện nhiều mặt."
            elif score < 70:
                feedback = "Phát âm khá tốt, cần cải thiện một số chi tiết."
            elif score < 85:
                feedback = "Phát âm tốt!"
            elif score < 95:
                feedback = "Phát âm rất tốt!"
            else:
                feedback = "Phát âm xuất sắc!"
            
        except Exception as e:
            score = 0
            feedback = "Không thể tính điểm - hãy thử lại."

        return {
            "score": score,
            "feedback": feedback,
            "intonation": intonation,
            "clarity": clarity,
            "rhythm": rhythm,
            "transcription": transcription,
            "words": [],  # Will be filled by the enhanced analysis
            # Add component scores with precise values
            "intonationScore": intonation_score,
            "clarityScore": clarity_score,
            "rhythmScore": rhythm_score, 
            "textScore": text_score,
            # Add raw measurements for reference
            "pitchMean": round(float(pitch_mean), 2),
            "f1Mean": round(float(f1_mean), 2),
            # Include formula weights for frontend display
            "formulaWeights": {
                "withoutAudio": {
                    "text": 0.7,
                    "intonation": 0.15,
                    "clarity": 0.15
                }
            },
            "userPitchData": user_pitch_data,
            "samplePitchData": sample_pitch_data,
            # Add formant data for visualization
            "userFormantData": f1_values if f1_values and len(f1_values) > 0 else [],
            "sampleFormantData": sample_f1_values if sample_f1_values and len(sample_f1_values) > 0 else []
        }

async def analyze_audio_enhanced(user_audio: UploadFile, 
                            sample_id: str = Form(None),
                            reference_text: str = Form(None)):
    """Enhanced audio analysis with LLM, hiragana normalization, and phoneme analysis."""
    
    user_audio_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as user_temp:
            user_content = await user_audio.read()
            user_temp.write(user_content)
            user_audio_path = user_temp.name
            user_audio.file.seek(0)
            
        sample_audio_path = None
        if sample_id:
            sample_path = f"../nihongo-it-backend/src/main/resources/samples/{sample_id}.wav"
            if os.path.exists(sample_path):
                sample_audio_path = sample_path
            else:
                sample_audio_path = None
                
        if not reference_text:
            reference_text = ""
            
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
                
        result = await analyze_audio_features(user_y, sr, sample_y, reference_text, transcription)
            
        orig_words = await tokenize_japanese(reference_text)
        hiragana_map = {}
        for word in orig_words:
            hiragana_map[word] = await to_hiragana(word)
        
        phoneme_errors = await simulate_phoneme_analysis(user_audio_path, reference_text, transcription)
        
        llm_result = await analyze_with_llm(reference_text, transcription, phoneme_errors)
        
        words, _, _ = await compare_words_enhanced(reference_text, transcription)
        
        enhanced_words = await combine_phoneme_errors_with_words(words, phoneme_errors, hiragana_map)
        
        personalized_feedback = await generate_personalized_feedback(
            enhanced_words, reference_text, transcription, llm_result)
        
        result["words"] = enhanced_words
        result["personalizedFeedback"] = personalized_feedback
        
        if user_audio_path and os.path.exists(user_audio_path):
            os.unlink(user_audio_path)
            
        return result
    except Exception as e:
        if user_audio_path and os.path.exists(user_audio_path):
            try:
                os.unlink(user_audio_path)
            except:
                pass
                
        raise HTTPException(status_code=500, detail=f"Enhanced analysis error: {str(e)}")

async def analyze_audio(user_audio: UploadFile, sample_audio: UploadFile, sentence: str):
    try:
        filename = user_audio.filename or "speech.webm"
        content_type = user_audio.content_type or "audio/webm"
        
        try:
            size = await user_audio.tell()
        except (AttributeError, NotImplementedError):
            user_audio_content = await user_audio.read()
            size = len(user_audio_content)
            user_audio.file.seek(0)
        
        file_extension = os.path.splitext(filename)[1].lower()
        if not file_extension:
            file_extension = ".webm"
        
        suffix = file_extension if file_extension in ['.mp3', '.wav', '.webm'] else '.webm'
        
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as user_temp:
            user_content = await user_audio.read()
            
            if not user_content:
                raise HTTPException(status_code=400, detail="File âm thanh rỗng")
                
            user_temp.write(user_content)
            user_audio_path = user_temp.name

        user_wav_path = user_audio_path
        if suffix != '.wav':
            user_wav_path = user_audio_path.replace(suffix, '.wav')
            try:
                import subprocess
                
                process = subprocess.run([
                    "ffmpeg", "-y", "-i", user_audio_path, 
                    "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                    "-v", "info", user_wav_path
                ], check=True, capture_output=True, text=True)
                
            except Exception as e:
                user_wav_path = user_audio_path

        sample_audio_path = ""
        if sample_audio and sample_audio.filename:
            try:
                sample_extension = os.path.splitext(sample_audio.filename)[1].lower()
                sample_suffix = sample_extension if sample_extension in ['.mp3', '.wav', '.webm'] else '.wav'
                
                with tempfile.NamedTemporaryFile(delete=False, suffix=sample_suffix) as sample_temp:
                    sample_content = await sample_audio.read()
                    sample_temp.write(sample_content)
                    sample_audio_path = sample_temp.name
                
            except Exception as e:
                sample_audio_path = ""

        try:
            with open(user_wav_path, "rb") as audio_file:
                response = client.audio.transcriptions.create(
                    model="whisper-1",
                    file=audio_file,
                    language="ja"
                )
                transcription = response.text
        except Exception as e:
            transcription = sentence

        await compare_words(sentence, transcription)

        try:
            user_y, sr = librosa.load(user_wav_path, sr=16000)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Không thể đọc file âm thanh: {str(e)}")

        result = await analyze_audio_features(user_y, sr, None, sentence, transcription)
            
        if user_wav_path and os.path.exists(user_wav_path):
            os.unlink(user_wav_path)

        return result

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi phân tích âm thanh: {str(e)}")

@app.post("/analyze", response_class=JSONResponse)
async def analyze_endpoint(
    audio: UploadFile = File(...),
    sentence: str = Form(...),
    sample: UploadFile = File(None)
):
    try:
        result = await analyze_audio(audio, sample, sentence)
        return JSONResponse(content=result)
    except Exception as e:
        if DEBUG_MODE:
            return JSONResponse(
                status_code=500,
                content={
                    "detail": f"Basic analysis error: {str(e)}",
                    "traceback": traceback.format_exc()
                }
            )
        else:
            return JSONResponse(
                status_code=500,
                content={"detail": f"Basic analysis error: {str(e)}"}
            )

@app.post("/analyze-audio-enhanced", response_class=JSONResponse)
async def analyze_enhanced_endpoint(
    file: UploadFile = File(...),
    reference_text: str = Form(...),
    sample_id: str = Form(None)
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
        
        result = await analyze_audio_enhanced(file, sample_id, reference_text)
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

@app.get("/analyze")
async def analyze_get():
    """Provide information about the analyze endpoint"""
    return JSONResponse(
        content={
            "message": "Sử dụng phương thức POST cho endpoint này. Tham số yêu cầu: 'audio' (file), 'sentence' (text)",
            "endpoint": "/analyze",
            "method": "POST",
            "required_params": ["audio", "sentence"],
            "optional_params": ["sample"]
        }
    )

@app.get("/test-libraries")
async def test_libraries():
    """Test all libraries to verify they're working properly"""
    
    results = {}
    
    try:
        results["openai"] = {"status": "ok", "version": getattr(OpenAI, "__version__", "unknown")}
    except Exception as e:
        results["openai"] = {"status": "error", "message": str(e)}
    
    try:
        results["librosa"] = {"status": "ok", "version": librosa.__version__}
    except Exception as e:
        results["librosa"] = {"status": "error", "message": str(e)}
    
    try:
        results["parselmouth"] = {"status": "ok", "version": parselmouth.__version__}
    except Exception as e:
        results["parselmouth"] = {"status": "error", "message": str(e)}
    
    try:
        results["aubio"] = {"status": "ok", "version": aubio.__version__}
    except Exception as e:
        results["aubio"] = {"status": "error", "message": str(e)}
    
    try:
        results["numpy"] = {"status": "ok", "version": np.__version__}
    except Exception as e:
        results["numpy"] = {"status": "error", "message": str(e)}
    
    try:
        sample_path = "../speech-java/src/main/resources/samples/hello_friend.wav"
        if os.path.exists(sample_path):
            y, sr = librosa.load(sample_path)
            results["audio_loading"] = {
                "status": "ok", 
                "file": sample_path,
                "length": len(y),
                "sr": sr
            }
        else:
            results["audio_loading"] = {"status": "error", "message": f"File not found: {sample_path}"}
    except Exception as e:
        results["audio_loading"] = {"status": "error", "message": str(e), "traceback": traceback.format_exc()}
    
    return results

@app.get("/sample-audio/{sample_id}")
async def get_sample_audio(sample_id: str, format: str = "wav"):
    """Serve a sample audio file for playback in the frontend."""
    
    if format not in ["wav", "mp3"]:
        format = "wav"
    
    sample_path = f"../speech-java/src/main/resources/samples/{sample_id}.{format}"
    
    if not os.path.exists(sample_path):
        raise HTTPException(status_code=404, detail=f"Sample audio file not found: {sample_id}")
    
    content_type = "audio/wav" if format == "wav" else "audio/mpeg"
    
    return FileResponse(
        path=sample_path,
        media_type=content_type,
        filename=f"{sample_id}.{format}"
    )

@app.get("/analyze-direct-sample/{sample_id}")
async def analyze_direct_sample(sample_id: str):
    """Analyze a specific sample file directly"""
    
    sample_path = f"../speech-java/src/main/resources/samples/{sample_id}.wav"
    
    if not os.path.exists(sample_path):
        raise HTTPException(status_code=404, detail=f"Sample file not found: {sample_id}")
    
    sentences = {
        "today_library": "今日、図書館で本を借りました。",
        "hello_friend": "こんにちは、友達！",
        "weather_good": "今日の天気はとても良いですね。",
        "japanese_study": "日本語を勉強することは楽しいです。"
    }
    
    sentence = sentences.get(sample_id)
    if not sentence:
        raise HTTPException(status_code=404, detail=f"No sentence mapping for sample ID: {sample_id}")
    
    try:
        class MockUploadFile:
            def __init__(self, path, filename):
                self.path = path
                self.filename = filename
                self.content_type = "audio/wav"
                self.size = os.path.getsize(path)
                self.file = None
                
            async def read(self):
                with open(self.path, "rb") as f:
                    return f.read()
                
            async def tell(self):
                return self.size
                
        mock_file = MockUploadFile(sample_path, f"{sample_id}.wav")
        
        result = await analyze_audio(mock_file, None, sentence)
        return result
        
    except Exception as e:
        if DEBUG_MODE:
            raise HTTPException(status_code=500, detail={
                "message": f"Analysis error: {str(e)}",
                "traceback": traceback.format_exc()
            })
        else:
            raise HTTPException(status_code=500, detail=f"Analysis error: {str(e)}")

@app.get("/health", response_class=JSONResponse)
async def health_check():
    """Simple health check endpoint to verify the API is working."""
    try:
        lib_checks = {
            "librosa": librosa.__version__,
            "parselmouth": parselmouth.__version__,
            "aubio": getattr(aubio, "__version__", "unknown"),
            "openai": getattr(OpenAI, "__version__", "unknown")
        }
        
        openai_status = "unavailable"
        try:
            if OPENAI_API_KEY:
                response = client.chat.completions.create(
                    model="gpt-3.5-turbo",
                    messages=[{"role": "user", "content": "Say 'ok' in one word"}],
                    max_tokens=1
                )
                if response.choices and response.choices[0].message:
                    openai_status = "available"
                else:
                    openai_status = "connected but no valid response"
            else:
                openai_status = "no API key"
        except Exception as e:
            openai_status = f"error: {str(e)}"
            
        return JSONResponse(
            content={
                "status": "healthy",
                "libraries": lib_checks,
                "openai_status": openai_status,
                "debug_mode": DEBUG_MODE
            }
        )
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"status": "unhealthy", "detail": str(e)}
        )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)