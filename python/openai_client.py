import json
import re
import logging
from typing import Dict, List, Any

from openai import OpenAI
from config import OPENAI_API_KEY
from nlp import process_text_to_tokens

logger = logging.getLogger(__name__)

client = OpenAI(api_key=OPENAI_API_KEY)


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
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=500,
        )
        content = response.choices[0].message.content.strip()
        match = re.search(r"({.*})", content.replace("\n", " "), re.DOTALL)
        if match:
            return json.loads(match.group(1))
        return {"incorrect_words": [], "auxiliary_words": [], "personalized_feedback": ""}
    except Exception as e:
        logger.error(f"analyze_with_llm failed: {e}")
        return {
            "incorrect_words": [],
            "auxiliary_words": ["ね", "よ", "な", "わ", "さ"],
            "personalized_feedback": "Cần cải thiện phát âm. Hãy thử lại nhé!",
        }


async def generate_llm_feedback(
    sentence: str,
    transcription: str,
    score: int,
    intonation_score: int,
    clarity_score: int,
    text_score: int,
    incorrect_words: List[str],
    missing_words: List[str],
) -> str:
    try:
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
2. Chỉ ra cụ thể 1-2 lỗi chính cần cải thiện
3. Đưa ra gợi ý thực tế để cải thiện
4. Tối đa 3 câu, bằng tiếng Việt
"""
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=400,
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        logger.error(f"generate_llm_feedback failed: {e}")
        return "Hãy tiếp tục luyện tập, bạn đang tiến bộ!"


async def generate_personalized_feedback(
    words: List[Dict],
    sentence: str,
    transcription: str,
    llm_result: Dict,
    score: int = 0,
    intonation_score: int = 0,
    clarity_score: int = 0,
    text_score: int = 0,
) -> str:
    try:
        incorrect_words = [w["text"] for w in words if not w.get("isCorrect", True)]
        correct_words = [w["text"] for w in words if w.get("isCorrect", True)]

        original_tokens = await process_text_to_tokens(sentence)
        transcription_tokens = await process_text_to_tokens(transcription)
        missing_words = [t for t in original_tokens if t not in transcription_tokens]

        total_words = len(original_tokens)
        issues, strengths, suggestions = [], [], []

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

        parts = []
        if score >= 90:
            parts.append("Xuất sắc!")
        elif score >= 75:
            parts.append("Rất tốt!")
        elif score >= 60:
            parts.append("Khá tốt!")
        elif score >= 40:
            parts.append("Bạn đã cố gắng!")
        else:
            parts.append("Đừng nản lòng!")

        if strengths:
            parts.append(f"{', '.join(strengths)}.")
        if issues and suggestions:
            parts.append(f"Cần cải thiện {issues[0]} - {suggestions[0]}.")
            if len(suggestions) > 1 and suggestions[1] != suggestions[0]:
                parts.append(f"{suggestions[1]}.")
        elif suggestions:
            parts.append(f"{suggestions[0]}.")

        if score >= 70:
            parts.append("Tiếp tục phát huy!")
        elif score >= 40:
            parts.append("Hãy luyện tập thêm!")
        else:
            parts.append("Đừng bỏ cuộc, bạn sẽ tiến bộ!")

        feedback = " ".join(parts)
        if len(feedback) < 30:
            return await generate_llm_feedback(
                sentence, transcription, score, intonation_score,
                clarity_score, text_score, incorrect_words, missing_words,
            )
        return feedback

    except Exception as e:
        logger.error(f"generate_personalized_feedback failed: {e}")
        return await generate_llm_feedback(
            sentence, transcription, score, intonation_score,
            clarity_score, text_score, [], [],
        )


async def summarize_feedback_with_llm(feedback_list: List[Dict], conversation_text: str) -> Dict:
    try:
        if not feedback_list:
            return {"summary": "Chưa có dữ liệu phản hồi nào.", "common_errors": [], "improvement_tips": []}

        scores = [item.get("score", 0) for item in feedback_list]
        avg_score = sum(scores) / len(scores)
        max_score = max(scores)
        feedbacks = [item.get("feedback", "") for item in feedback_list if item.get("feedback")]

        word_count: Dict[str, int] = {}
        for item in feedback_list:
            for word in (item.get("textAnalysis") or {}).get("enhancedWords") or []:
                if not word.get("isCorrect", True):
                    w = word.get("text", "")
                    word_count[w] = word_count.get(w, 0) + 1

        common_errors = sorted(word_count.items(), key=lambda x: x[1], reverse=True)[:5]

        prompt = f"""
Phân tích và tổng hợp kết quả luyện tập phát âm tiếng Nhật:

THÔNG TIN TỔNG QUAN:
- Câu gốc: '{conversation_text}'
- Số lần luyện tập: {len(feedback_list)}
- Điểm trung bình: {avg_score:.1f}/100
- Điểm cao nhất: {max_score}/100

CÁC LỖI THƯỜNG GẶP:
{', '.join([f"'{w}' ({c} lần)" for w, c in common_errors])}

PHẢN HỒI TỪ CÁC LẦN TRƯỚC:
{' | '.join(feedbacks[:5])}

YÊU CẦU:
1. Tóm tắt ngắn gọn kết quả luyện tập (1-2 câu)
2. Liệt kê 3-5 lỗi phát âm phổ biến nhất và cách sửa
3. Đưa ra 2-3 lời khuyên cụ thể để cải thiện
4. Viết bằng tiếng Việt, tối đa 200 từ

Trả về JSON:
{{
  "summary": "tóm tắt ngắn gọn",
  "common_errors": ["lỗi 1: cách sửa", ...],
  "improvement_tips": ["lời khuyên 1", ...]
}}
"""
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=500,
            response_format={"type": "json_object"},
        )
        result = json.loads(response.choices[0].message.content.strip())
        return {
            "summary": result.get("summary", "Không thể tạo tóm tắt."),
            "common_errors": result.get("common_errors", []),
            "improvement_tips": result.get("improvement_tips", []),
            "attempts": len(feedback_list),
            "avg_score": avg_score,
            "max_score": max_score,
        }
    except Exception as e:
        logger.error(f"summarize_feedback_with_llm failed: {e}")
        return {
            "summary": "Đã xảy ra lỗi khi tạo tóm tắt.",
            "common_errors": [],
            "improvement_tips": [],
            "error": str(e),
        }
