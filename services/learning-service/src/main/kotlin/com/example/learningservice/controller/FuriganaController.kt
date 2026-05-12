package com.example.learningservice.controller

import com.atilika.kuromoji.ipadic.Tokenizer
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/learning/furigana")
@Tag(name = "Furigana", description = "API to generate furigana for Japanese text")
class FuriganaController {

    private val tokenizer = Tokenizer()

    // Cache for frequently accessed readings
    private val readingCache = mutableMapOf<String, String?>()

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Generate furigana",
        description = "Analyzes Japanese text and returns tokens with kanji and furigana readings",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully generated furigana",
                content = [Content(mediaType = "application/json")],
            ),
        ],
    )
    fun generateFurigana(
        @Parameter(description = "Japanese text to analyze", required = true)
        @RequestParam text: String,
    ): List<FuriganaToken> {
        return analyzeSentence(text)
    }

    private fun analyzeSentence(sentence: String): List<FuriganaToken> {
        val tokens = tokenizer.tokenize(sentence)
        return tokens.map { token ->
            val surface = token.surface // Original text (kanji or not)
            val reading = getReading(token)
            val isKanji = surface.any { it.isKanji() }

            FuriganaToken(
                text = surface,
                reading = if (isKanji && reading != null && reading != surface) reading else null,
                isKanji = isKanji,
            )
        }
    }

    private fun getReading(token: com.atilika.kuromoji.ipadic.Token): String? {
        val surface = token.surface

        // Check cache first
        if (readingCache.containsKey(surface)) {
            return readingCache[surface]
        }

        // Get reading from token and convert to hiragana
        val reading = token.reading?.let { katakanaToHiragana(it) }

        // Cache the result
        readingCache[surface] = reading

        return reading
    }

    // Convert katakana to hiragana
    private fun katakanaToHiragana(katakana: String): String {
        return katakana.map { char ->
            if (char in '\u30A1'..'\u30FA') { // Katakana range
                (char.code - 0x60).toChar() // Convert to hiragana
            } else {
                char
            }
        }.joinToString("")
    }

    // Check if character is kanji
    private fun Char.isKanji(): Boolean = this in '\u4E00'..'\u9FFF'
}

data class FuriganaToken(
    val text: String,
    val reading: String?,
    val isKanji: Boolean,
)
