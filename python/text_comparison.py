import difflib
import logging
from typing import Dict, List, Any, Tuple

from nlp import process_text_to_tokens, tokenize_japanese, to_hiragana
from openai_client import analyze_with_llm

logger = logging.getLogger(__name__)


async def compare_token_arrays(original_tokens: List[str], transcription_tokens: List[str]) -> Dict:
    matcher = difflib.SequenceMatcher(None, original_tokens, transcription_tokens)
    similarity_ratio = matcher.ratio()
    matching_blocks = matcher.get_matching_blocks()
    matched_tokens = []
    for m in matching_blocks:
        if m.size > 0:
            matched_tokens.extend(original_tokens[m.a : m.a + m.size])

    return {
        "similarity_ratio": similarity_ratio,
        "original_tokens": original_tokens,
        "transcription_tokens": transcription_tokens,
        "matched_tokens": matched_tokens,
        "matching_blocks": matching_blocks,
        "opcodes": matcher.get_opcodes(),
        "total_original_tokens": len(original_tokens),
        "total_transcription_tokens": len(transcription_tokens),
        "total_matched_tokens": len(matched_tokens),
    }


async def compare_texts_hybrid(original: str, transcription: str) -> Dict:
    original_tokens = await process_text_to_tokens(original)
    transcription_tokens = await process_text_to_tokens(transcription)

    token_result = await compare_token_arrays(original_tokens, transcription_tokens)
    token_similarity = token_result["similarity_ratio"]

    orig_str = "".join(original_tokens)
    trans_str = "".join(transcription_tokens)
    char_similarity = difflib.SequenceMatcher(None, orig_str, trans_str).ratio()

    max_len = max(len(original_tokens), len(transcription_tokens))
    token_sims = []
    for i in range(max_len):
        ot = original_tokens[i] if i < len(original_tokens) else ""
        tt = transcription_tokens[i] if i < len(transcription_tokens) else ""
        token_sims.append(difflib.SequenceMatcher(None, ot, tt).ratio() if ot and tt else 0.0)

    avg_token_sim = sum(token_sims) / len(token_sims) if token_sims else 0.0
    hybrid = token_similarity * 0.4 + char_similarity * 0.4 + avg_token_sim * 0.2

    logger.info(f"Hybrid: {hybrid:.3f} (token={token_similarity:.3f}, char={char_similarity:.3f}, avg={avg_token_sim:.3f})")
    return {
        "hybrid_similarity": hybrid,
        "token_similarity": token_similarity,
        "character_similarity": char_similarity,
        "average_token_similarity": avg_token_sim,
        "original_tokens": original_tokens,
        "transcription_tokens": transcription_tokens,
        "original_string": orig_str,
        "transcription_string": trans_str,
        "individual_token_similarities": token_sims,
        "analysis": {
            "structural_match": token_similarity > 0.8,
            "phonetic_match": char_similarity > 0.8,
            "partial_match": avg_token_sim > 0.6,
            "overall_match": hybrid > 0.7,
        },
    }


async def get_original_sentence_structure(original: str) -> Dict:
    tokens = await tokenize_japanese(original)
    structure = []
    pos = 0
    for i, token in enumerate(tokens):
        hiragana = await to_hiragana(token)
        start = original.find(token, pos)
        end = start + len(token) if start != -1 else pos + len(token)
        structure.append({
            "index": i,
            "original": token,
            "hiragana": hiragana,
            "position": {"start": start, "end": end},
            "isCorrect": True,
            "suggestion": None,
        })
        pos = end
    return {"original_text": original, "tokens": structure, "total_tokens": len(tokens)}


async def compare_words_enhanced(original: str, transcription: str) -> Tuple[List[Dict], Dict, Dict]:
    original_tokens = await process_text_to_tokens(original)
    transcription_tokens = await process_text_to_tokens(transcription)

    token_comparison = await compare_token_arrays(original_tokens, transcription_tokens)
    llm_analysis = await analyze_with_llm(original, transcription)

    matched = set(token_comparison["matched_tokens"])
    enhanced_words = [
        {"text": t, "isCorrect": t in matched, "suggestion": None}
        for t in original_tokens
    ]

    llm_analysis["token_similarity_ratio"] = token_comparison["similarity_ratio"]
    llm_analysis["matched_tokens"] = token_comparison["matched_tokens"]
    llm_analysis["personalized_feedback"] = ""

    orig_hiragana = {t: t for t in original_tokens}
    return enhanced_words, orig_hiragana, llm_analysis


async def compare_words_with_structure(original: str, transcription: str) -> Dict:
    original_structure = await get_original_sentence_structure(original)
    enhanced_words, orig_hiragana, llm_analysis = await compare_words_enhanced(original, transcription)

    original_tokens = await process_text_to_tokens(original)
    transcription_tokens = await process_text_to_tokens(transcription)
    token_comparison = await compare_token_arrays(original_tokens, transcription_tokens)

    enhanced_map = {w["text"]: w for w in enhanced_words}
    for token_info in original_structure["tokens"]:
        hira = token_info["hiragana"]
        if hira in enhanced_map:
            token_info["isCorrect"] = enhanced_map[hira]["isCorrect"]
        else:
            match = next(
                (w for w in enhanced_words if hira in w["text"] or w["text"] in hira),
                None,
            )
            token_info["isCorrect"] = match["isCorrect"] if match else False
        token_info["suggestion"] = None

    return {
        "original_structure": original_structure,
        "enhanced_words": enhanced_words,
        "hiragana_mapping": orig_hiragana,
        "llm_analysis": llm_analysis,
        "token_comparison": {
            "similarity_ratio": token_comparison["similarity_ratio"],
            "original_tokens": token_comparison["original_tokens"],
            "transcription_tokens": token_comparison["transcription_tokens"],
            "matched_tokens": token_comparison["matched_tokens"],
        },
        "transcription_text": transcription,
    }
