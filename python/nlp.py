import re
import logging
from string import punctuation
from typing import List
import mojimoji
from sudachipy import tokenizer, dictionary

logger = logging.getLogger(__name__)

JAPANESE_PUNCTUATION = "、。！？；：「」『』・〜"
CLEAN_REGEX = re.compile(
    f"[{re.escape(punctuation + JAPANESE_PUNCTUATION)}]"
    r"|[^぀-ゟ゠-ヿ一-鿿\sA-Za-z0-9]"
)

try:
    _sudachi = dictionary.Dictionary().create()
    logger.info("SudachiPy tokenizer initialized")
except Exception as e:
    logger.error(f"Failed to initialize SudachiPy: {e}")
    raise RuntimeError("SudachiPy tokenizer failed to initialize") from e


async def katakana_to_hiragana(text: str) -> str:
    result = []
    for char in text:
        result.append(chr(ord(char) - 0x60) if "ァ" <= char <= "ヺ" else char)
    return "".join(result)


async def to_hiragana(text: str) -> str:
    try:
        mode = tokenizer.Tokenizer.SplitMode.C
        tokens = _sudachi.tokenize(text, mode)
        parts = [await katakana_to_hiragana(t.reading_form()) for t in tokens]
        result = "".join(parts)
        logger.debug(f"to_hiragana: '{text}' -> '{result}'")
        return result
    except Exception as e:
        logger.error(f"to_hiragana failed: {e}")
        return text


async def clean_text(text: str) -> str:
    return CLEAN_REGEX.sub("", text)


async def normalize_width(text: str) -> str:
    try:
        return mojimoji.han_to_zen(text)
    except Exception as e:
        logger.warning(f"normalize_width failed: {e}")
        return text


async def tokenize_japanese(text: str) -> List[str]:
    cleaned = await clean_text(text)
    if not cleaned:
        return []
    try:
        mode = tokenizer.Tokenizer.SplitMode.C
        tokens = _sudachi.tokenize(cleaned, mode)
        words = [
            t.surface() for t in tokens
            if t.surface().strip()
            and not re.match(r"^[^぀-ゟ゠-ヿ一-鿿]+$", t.surface())
        ]
        logger.debug(f"tokenize_japanese: '{cleaned}' -> {words}")
        return words
    except Exception as e:
        logger.error(f"tokenize_japanese failed: {e}")
        return [c for c in cleaned if c.strip()]


async def process_text_to_tokens(text: str) -> List[str]:
    text = text.strip()
    text = await normalize_width(text)
    tokens = await tokenize_japanese(text)
    cleaned_tokens = [ct for t in tokens if (ct := await clean_text(t)).strip()]
    return [await to_hiragana(t) for t in cleaned_tokens]
