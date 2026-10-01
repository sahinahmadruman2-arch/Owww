package com.example.domain.engine

import com.example.domain.model.ManualQaPair
import com.example.domain.model.ManualTrainingParseResult
import com.example.domain.model.ManualTrainingVerificationReport
import java.util.regex.Pattern

object ManualTrainingParser {

    private val MARKER_PATTERN = Pattern.compile(
        """(?im)(?:^|\n|\r)\s*(?:[-*•]\s*)?(Question|Answer)\s*:\s*|\b(Question|Answer)\s*:\s*"""
    )

    fun isManualTrainingFormat(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 15) return false
        val matcher = MARKER_PATTERN.matcher(trimmed)
        var hasQ = false
        var hasA = false
        while (matcher.find()) {
            val type = (matcher.group(1) ?: matcher.group(2)).lowercase()
            if (type == "question") hasQ = true
            if (type == "answer") hasA = true
            if (hasQ && hasA) return true
        }
        return false
    }

    fun parse(text: String): ManualTrainingParseResult {
        val matcher = MARKER_PATTERN.matcher(text)

        data class MarkerMatch(
            val type: String, // "question" or "answer"
            val contentStartIndex: Int,
            val markerStartIndex: Int
        )

        val markers = mutableListOf<MarkerMatch>()
        while (matcher.find()) {
            val type = (matcher.group(1) ?: matcher.group(2)).lowercase()
            markers.add(
                MarkerMatch(
                    type = type,
                    contentStartIndex = matcher.end(),
                    markerStartIndex = matcher.start()
                )
            )
        }

        if (markers.isEmpty()) {
            return ManualTrainingParseResult(
                totalQuestionsDetected = 0,
                totalAnswersDetected = 0,
                pairedEntries = emptyList(),
                incompleteEntriesCount = 0,
                invalidEntriesCount = 0
            )
        }

        var totalQuestions = 0
        var totalAnswers = 0
        var incompleteCount = 0
        var invalidCount = 0
        val incompleteDetails = mutableListOf<String>()
        val pairedEntries = mutableListOf<ManualQaPair>()

        var pendingQuestion: String? = null
        var pairIndex = 1

        for (i in markers.indices) {
            val current = markers[i]
            val contentEnd = if (i + 1 < markers.size) markers[i + 1].markerStartIndex else text.length
            val rawContent = text.substring(current.contentStartIndex, contentEnd).trim()

            if (current.type == "question") {
                totalQuestions++
                // If there was an earlier question without an answer:
                if (pendingQuestion != null) {
                    incompleteCount++
                    incompleteDetails.add("Question without answer: '$pendingQuestion'")
                }
                pendingQuestion = rawContent
            } else if (current.type == "answer") {
                totalAnswers++
                if (pendingQuestion != null) {
                    if (pendingQuestion.isNotBlank() && rawContent.isNotBlank()) {
                        pairedEntries.add(
                            ManualQaPair(
                                index = pairIndex++,
                                question = pendingQuestion,
                                answer = rawContent
                            )
                        )
                    } else {
                        incompleteCount++
                        incompleteDetails.add("Empty question or answer detected")
                    }
                    pendingQuestion = null
                } else {
                    // Answer without a question
                    invalidCount++
                    incompleteDetails.add("Answer without matching question: '${rawContent.take(40)}...'")
                }
            }
        }

        // Check if last question was left without an answer
        if (pendingQuestion != null) {
            incompleteCount++
            incompleteDetails.add("Incomplete entry at end of text: '$pendingQuestion'")
        }

        return ManualTrainingParseResult(
            totalQuestionsDetected = totalQuestions,
            totalAnswersDetected = totalAnswers,
            pairedEntries = pairedEntries,
            incompleteEntriesCount = incompleteCount,
            invalidEntriesCount = invalidCount,
            incompleteDetails = incompleteDetails
        )
    }

    fun buildVerificationReport(
        parseResult: ManualTrainingParseResult,
        savedCount: Int,
        unprocessedCount: Int,
        documentTitle: String = "Manual Training Session"
    ): ManualTrainingVerificationReport {
        return ManualTrainingVerificationReport(
            totalQuestionsDetected = parseResult.totalQuestionsDetected,
            totalAnswersDetected = parseResult.totalAnswersDetected,
            successfullyPairedEntries = parseResult.pairedEntries.size,
            incompleteOrInvalidEntries = parseResult.incompleteEntriesCount + parseResult.invalidEntriesCount,
            entriesSavedToPersistentStorage = savedCount,
            entriesNeedingProcessing = unprocessedCount,
            documentTitle = documentTitle,
            isSuccess = unprocessedCount == 0 && savedCount == parseResult.pairedEntries.size
        )
    }
}
