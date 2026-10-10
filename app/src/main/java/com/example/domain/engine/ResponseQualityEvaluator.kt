package com.example.domain.engine

import com.example.data.local.dao.SystemLearningDao
import com.example.data.local.entity.LearningEventEntity
import com.example.data.local.entity.ResponseEvaluationEntity
import com.example.domain.model.GeneratedAnswerResult
import com.example.domain.model.MessageSemanticAnalysis
import java.util.Locale

data class EvaluationReport(
    val passed: Boolean,
    val overallScore: Float,
    val relevance: Float,
    val contextAccuracy: Float,
    val factualReliability: Float,
    val taskCompletion: Float,
    val instructionFollow: Float,
    val failureReason: String? = null,
    val suggestedRetry: Boolean = false
)

object ResponseQualityEvaluator {

    suspend fun evaluateAndRecord(
        query: String,
        result: GeneratedAnswerResult,
        analysis: MessageSemanticAnalysis,
        learningDao: SystemLearningDao? = null,
        messageId: String = ""
    ): EvaluationReport {
        val answer = result.answerText.trim()
        val qLower = query.lowercase(Locale.ROOT)
        val ansLower = answer.lowercase(Locale.ROOT)

        var relevance = 1.0f
        var contextAccuracy = 1.0f
        var factualReliability = 1.0f
        var taskCompletion = 1.0f
        var instructionFollow = 1.0f
        val issues = mutableListOf<String>()

        // 1. Instruction & Constraint Check (e.g., "(say in one word)")
        val isOneWordRequested = analysis.constraints.any { it.contains("one word") } || qLower.contains("one word")
        if (isOneWordRequested) {
            val words = answer.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size > 1) {
                instructionFollow = 0.5f
                issues.add("Failed one-word constraint: response had ${words.size} words.")
            }
        }

        // 2. Relevance Check
        if (answer.isBlank()) {
            relevance = 0.0f
            taskCompletion = 0.0f
            issues.add("Empty response generated.")
        } else if (ansLower.contains("hello") && (qLower.contains("football") || qLower.contains("math") || qLower.contains("code"))) {
            relevance = 0.3f
            issues.add("Mismatched conversational greeting returned for factual query.")
        }

        // 3. Factual Reliability & Evidence Consistency
        if (result.hasConflict) {
            factualReliability = 0.7f
        }

        val overallScore = (relevance + contextAccuracy + factualReliability + taskCompletion + instructionFollow) / 5f
        val passed = overallScore >= 0.75f && issues.isEmpty()
        val failureReason = if (issues.isNotEmpty()) issues.joinToString("; ") else null

        val report = EvaluationReport(
            passed = passed,
            overallScore = overallScore,
            relevance = relevance,
            contextAccuracy = contextAccuracy,
            factualReliability = factualReliability,
            taskCompletion = taskCompletion,
            instructionFollow = instructionFollow,
            failureReason = failureReason,
            suggestedRetry = !passed && (result.retryCount ?: 0) < 1
        )

        learningDao?.let { dao ->
            val entity = ResponseEvaluationEntity(
                messageId = messageId,
                query = query,
                response = answer.take(200),
                relevanceScore = relevance,
                contextAccuracyScore = contextAccuracy,
                factualReliabilityScore = factualReliability,
                taskCompletionScore = taskCompletion,
                instructionFollowScore = instructionFollow,
                passed = passed,
                failureReason = failureReason,
                retryCount = result.retryCount ?: 0
            )
            dao.insertResponseEvaluation(entity)

            dao.insertLearningEvent(
                LearningEventEntity(
                    eventType = "QUALITY_EVALUATED",
                    summary = if (passed) "Response Quality Verified (${(overallScore * 100).toInt()}%)" else "Quality Issue Detected",
                    details = "Query: '${query.take(30)}...' -> Passed: $passed. Issues: ${failureReason ?: "None"}"
                )
            )
        }

        return report
    }
}
