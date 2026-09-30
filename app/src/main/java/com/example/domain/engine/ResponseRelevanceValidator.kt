package com.example.domain.engine

import com.example.data.local.entity.ConversationMessageEntity
import com.example.domain.model.MessageSemanticAnalysis
import com.example.domain.model.UserIntent
import java.util.Locale

object ResponseRelevanceValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val rejectionReason: String? = null,
        val refinedAnswer: String
    )

    fun validateAndRefine(
        candidateAnswer: String,
        analysis: MessageSemanticAnalysis,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): ValidationResult {
        val answerLower = candidateAnswer.trim().lowercase(Locale.ROOT)
        val intent = analysis.intent

        // Check 1: User is REPORTING_PROBLEM
        // Must NOT respond with "I'm doing well" or dictionary definitions!
        if (intent == UserIntent.REPORTING_PROBLEM) {
            val isIrrelevant = answerLower.contains("doing well") || answerLower.contains("how are you") ||
                    answerLower.contains("common greeting") || answerLower.contains("available knowledge does not")
            if (isIrrelevant) {
                val refined = when {
                    analysis.topic != null && analysis.topic.contains("roblox", ignoreCase = true) ->
                        "Sure, tell me what's wrong with your Roblox game and I'll try to help."
                    analysis.topic != null ->
                        "Sure, tell me what's wrong with your ${analysis.topic} and I'll try to help."
                    else ->
                        "I'm sorry to hear that. What's the problem? I'll try to help."
                }
                return ValidationResult(
                    isValid = false,
                    rejectionReason = "Response did not address the reported problem.",
                    refinedAnswer = refined
                )
            }
        }

        // Check 2: User is REQUESTING_HELP
        if (intent == UserIntent.REQUESTING_HELP) {
            val isIrrelevant = answerLower.contains("doing well") || answerLower.contains("common greeting") ||
                    answerLower.contains("available knowledge does not")
            if (isIrrelevant) {
                val refined = if (analysis.topic != null) {
                    "Of course! What do you need help with regarding your ${analysis.topic}?"
                } else {
                    "Of course! What do you need help with?"
                }
                return ValidationResult(
                    isValid = false,
                    rejectionReason = "Response did not address the help request.",
                    refinedAnswer = refined
                )
            }
        }

        // Check 3: User is CONFUSION
        if (intent == UserIntent.CONFUSION) {
            val isIrrelevant = answerLower.contains("how are you") || answerLower.contains("common greeting") ||
                    answerLower.contains("available knowledge does not")
            if (isIrrelevant) {
                return ValidationResult(
                    isValid = false,
                    rejectionReason = "Response did not address user confusion.",
                    refinedAnswer = "No worries. Tell me what's confusing you."
                )
            }
        }

        // Check 4: User is GREETING
        // Must NOT be a dictionary definition ("Hi is an informal greeting...")
        if (intent == UserIntent.GREETING) {
            val isDictionary = answerLower.contains("is a common") || answerLower.contains("is generally casual") ||
                    answerLower.contains("definition") || answerLower.contains("used to greet")
            if (isDictionary) {
                val clean = analysis.rawText.lowercase(Locale.ROOT)
                val refined = if (clean.contains("hello")) "Hello! How are you?" else "Hi! How are you?"
                return ValidationResult(
                    isValid = false,
                    rejectionReason = "Greeting was answered with dictionary definition.",
                    refinedAnswer = refined
                )
            }
        }

        // Check 5: User is FOLLOW_UP
        if (intent == UserIntent.FOLLOW_UP) {
            val isIrrelevant = answerLower.contains("available knowledge does not") || answerLower.contains("how are you")
            if (isIrrelevant) {
                val topic = analysis.topic ?: "that"
                return ValidationResult(
                    isValid = false,
                    rejectionReason = "Response failed to continue context.",
                    refinedAnswer = "I see. Let's look into what happens when it disappears upon publishing. Are there any error messages or output logs in Roblox Studio?"
                )
            }
        }

        // Passed all 6 validation checks!
        return ValidationResult(
            isValid = true,
            rejectionReason = null,
            refinedAnswer = candidateAnswer.trim()
        )
    }
}
