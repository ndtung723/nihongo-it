"""Tests for text_comparison.py — deterministic comparison algorithms."""
import pytest
from unittest.mock import AsyncMock, patch


@pytest.mark.asyncio
async def test_compare_token_arrays_identical():
    from text_comparison import compare_token_arrays

    tokens = ["こんにちは", "世界"]
    result = await compare_token_arrays(tokens, tokens)

    assert result["similarity_ratio"] == 1.0
    assert result["total_matched_tokens"] == 2


@pytest.mark.asyncio
async def test_compare_token_arrays_empty():
    from text_comparison import compare_token_arrays

    result = await compare_token_arrays([], [])

    assert result["similarity_ratio"] == 1.0
    assert result["total_original_tokens"] == 0
    assert result["total_transcription_tokens"] == 0


@pytest.mark.asyncio
async def test_compare_token_arrays_completely_different():
    from text_comparison import compare_token_arrays

    result = await compare_token_arrays(["ABC"], ["XYZ"])

    assert result["similarity_ratio"] == 0.0
    assert result["total_matched_tokens"] == 0


@pytest.mark.asyncio
async def test_compare_token_arrays_partial_match():
    from text_comparison import compare_token_arrays

    original = ["A", "B", "C"]
    transcription = ["A", "X", "C"]
    result = await compare_token_arrays(original, transcription)

    # 2 out of 3 tokens match (A and C)
    assert result["similarity_ratio"] > 0.0
    assert result["similarity_ratio"] < 1.0
    assert result["total_original_tokens"] == 3


@pytest.mark.asyncio
async def test_compare_texts_hybrid_identical(monkeypatch):
    """Hybrid comparison returns 1.0 for identical texts."""
    from text_comparison import compare_texts_hybrid

    # Mock process_text_to_tokens and analyze_with_llm to avoid SudachiPy/OpenAI calls
    mock_tokens = ["こんにちは", "世界"]
    with patch("text_comparison.process_text_to_tokens", new=AsyncMock(return_value=mock_tokens)):
        result = await compare_texts_hybrid("こんにちは世界", "こんにちは世界")

    assert result["hybrid_similarity"] == pytest.approx(1.0, abs=0.01)


@pytest.mark.asyncio
async def test_compare_texts_hybrid_different(monkeypatch):
    """Hybrid comparison returns < 1.0 for different texts."""
    from text_comparison import compare_texts_hybrid

    with patch("text_comparison.process_text_to_tokens", new=AsyncMock(side_effect=[
        ["ABC", "DEF"],
        ["XYZ", "QRS"],
    ])):
        result = await compare_texts_hybrid("text A", "text B")

    assert result["hybrid_similarity"] < 1.0
    assert "token_similarity" in result
    assert "character_similarity" in result
    assert "average_token_similarity" in result
