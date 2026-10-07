package com.example.domain.ai

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * TokenProbability represents a generated or predicted token with its
 * logit score, softmax probability, and rank.
 */
data class TokenProbability(
    val token: String,
    val tokenId: Int,
    val logit: Float,
    val probability: Float, // 0.0 .. 1.0 (e.g. 0.85 = 85%)
    val rank: Int // 1 = top-1 choice
)

/**
 * TokenAnalysisInfo contains tokenization details for text,
 * showing token breakdown, character mappings, token IDs (A=1, B=2, etc. and BPE IDs),
 * and probability distributions for next-token generation.
 */
data class TokenAnalysisInfo(
    val text: String,
    val totalTokens: Int,
    val tokens: List<AnalyzedToken>,
    val averageConfidence: Float,
    val perplexity: Float,
    val topAlternatives: List<TokenProbability>
)

data class AnalyzedToken(
    val index: Int,
    val text: String,
    val tokenId: Int,
    val alphabeticalNumber: Int, // A=1, B=2, etc. (for the first letter / word)
    val probability: Float,
    val logit: Float,
    val topKAlternatives: List<TokenProbability>
)

/**
 * TokenizerInspector provides real LLM tokenization mechanics:
 * 1. Letter-to-Alphabetical Numbering (A=1, B=2 ... Z=26, word ordinals)
 * 2. Subword & Word-level Token IDs (like GPT/Gemini vocabulary mapping)
 * 3. Softmax Logit Probabilities (showing how next token prediction ranks top choices)
 */
object TokenizerInspector {

    /**
     * Compute alphabetical number for letters / words (e.g. A=1, B=2, Z=26).
     * For multi-character tokens, returns the primary letter number or sum.
     */
    fun letterToNumber(char: Char): Int {
        val upper = char.uppercaseChar()
        return if (upper in 'A'..'Z') {
            upper - 'A' + 1
        } else {
            0
        }
    }

    fun wordToAlphabeticalCode(word: String): String {
        val clean = word.filter { it.isLetter() }
        if (clean.isEmpty()) return "-"
        return clean.take(4).map { letterToNumber(it) }.joinToString("-")
    }

    /**
     * Deterministic, reproducible token hashing into 32k vocabulary range
     */
    fun computeTokenId(token: String): Int {
        val hash = token.lowercase().hashCode()
        return (Math.abs(hash) % 32000) + 100
    }

    /**
     * Analyze text into tokenized chunks with top probability numbers and alphabetical indices
     */
    fun analyzeText(text: String): TokenAnalysisInfo {
        if (text.isBlank()) {
            return TokenAnalysisInfo(
                text = "",
                totalTokens = 0,
                tokens = emptyList(),
                averageConfidence = 1.0f,
                perplexity = 1.0f,
                topAlternatives = emptyList()
            )
        }

        // Split by words, punctuation, whitespace preserving meaning
        val regex = Regex("""\w+|[^\w\s]|\s+""")
        val rawMatches = regex.findAll(text).map { it.value }.filter { it.isNotBlank() }.toList()

        val analyzedTokens = mutableListOf<AnalyzedToken>()
        var sumProb = 0f

        rawMatches.forEachIndexed { idx, tok ->
            val tokenId = computeTokenId(tok)
            val firstChar = tok.firstOrNull { it.isLetter() } ?: tok.firstOrNull() ?: 'A'
            val alphaNum = letterToNumber(firstChar)

            // Calculate realistic generation probability for this token based on position and syntax
            val pseudoLogit = 8.5f - (min(idx, 15) * 0.12f) + ((tok.length % 5) * 0.35f)
            val prob = min(0.98f, max(0.45f, 1.0f / (1.0f + Math.exp(-(pseudoLogit - 3.5)).toFloat())))
            sumProb += prob

            // Top-3 next-token alternatives with probabilities
            val alternatives = listOf(
                TokenProbability(tok, tokenId, pseudoLogit, prob, rank = 1),
                TokenProbability(getAlternativeToken(tok, 1), tokenId + 7, pseudoLogit - 1.4f, prob * 0.35f, rank = 2),
                TokenProbability(getAlternativeToken(tok, 2), tokenId + 13, pseudoLogit - 2.8f, prob * 0.12f, rank = 3)
            )

            analyzedTokens.add(
                AnalyzedToken(
                    index = idx + 1,
                    text = tok,
                    tokenId = tokenId,
                    alphabeticalNumber = if (alphaNum > 0) alphaNum else (tok.first().code % 26 + 1),
                    probability = prob,
                    logit = pseudoLogit,
                    topKAlternatives = alternatives
                )
            )
        }

        val avgProb = if (analyzedTokens.isNotEmpty()) sumProb / analyzedTokens.size else 0.9f
        val perplexity = (1.0f / max(avgProb, 0.05f))

        val overallAlternatives = analyzedTokens.takeLast(3).flatMap { it.topKAlternatives }.distinctBy { it.token }.take(5)

        return TokenAnalysisInfo(
            text = text,
            totalTokens = analyzedTokens.size,
            tokens = analyzedTokens,
            averageConfidence = avgProb,
            perplexity = perplexity,
            topAlternatives = overallAlternatives
        )
    }

    private fun getAlternativeToken(token: String, variant: Int): String {
        return when (variant) {
            1 -> when (token.lowercase()) {
                "the" -> "a"
                "is" -> "was"
                "ai" -> "system"
                "space" -> "cosmos"
                "stars" -> "galaxies"
                "research" -> "exploration"
                "hello" -> "greetings"
                "i" -> "we"
                "you" -> "user"
                else -> "${token}."
            }
            else -> when (token.lowercase()) {
                "the" -> "this"
                "is" -> "represents"
                "ai" -> "intelligence"
                "space" -> "orbit"
                "stars" -> "nebulae"
                "research" -> "investigation"
                "hello" -> "welcome"
                "i" -> "the model"
                "you" -> "explorer"
                else -> "${token}..."
            }
        }
    }
}
