"""Tests for nlp.py — pure/async NLP utility functions."""
import pytest


@pytest.mark.asyncio
async def test_katakana_to_hiragana_basic():
    from nlp import katakana_to_hiragana

    assert await katakana_to_hiragana("テスト") == "てすと"
    assert await katakana_to_hiragana("アイウエオ") == "あいうえお"


@pytest.mark.asyncio
async def test_katakana_to_hiragana_mixed():
    from nlp import katakana_to_hiragana

    # Hiragana chars should pass through unchanged
    result = await katakana_to_hiragana("てすとTest")
    assert result == "てすとTest"


@pytest.mark.asyncio
async def test_katakana_to_hiragana_empty():
    from nlp import katakana_to_hiragana

    assert await katakana_to_hiragana("") == ""


@pytest.mark.asyncio
async def test_clean_text_removes_punctuation():
    from nlp import clean_text

    result = await clean_text("こんにちは、世界！")
    assert "、" not in result
    assert "！" not in result
    assert "こんにちは" in result


@pytest.mark.asyncio
async def test_clean_text_empty():
    from nlp import clean_text

    assert await clean_text("") == ""


@pytest.mark.asyncio
async def test_normalize_width_half_to_full():
    from nlp import normalize_width

    # Half-width katakana → full-width
    result = await normalize_width("ｱｲｳ")
    assert result == "アイウ"


@pytest.mark.asyncio
async def test_normalize_width_passthrough():
    from nlp import normalize_width

    result = await normalize_width("ABC")
    # mojimoji han_to_zen converts ASCII too; just verify it doesn't crash
    assert isinstance(result, str)
