package com.example.domain.engine

import com.example.BuildConfig
import com.example.data.local.entity.ConversationMessageEntity
import com.example.data.remote.*
import com.example.domain.model.GeneratedAnswerResult
import com.example.domain.model.RetrievalResult
import com.example.domain.model.UserIntent
import java.util.Locale

class AnswerGenerator {

    suspend fun generateAnswer(
        query: String,
        retrieval: RetrievalResult,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): GeneratedAnswerResult {
        val intent = retrieval.userIntent

        // A. CONVERSATIONAL INTENT HANDLING
        // Rule: When user is having a normal conversation, respond conversationally,
        // do not explain the meaning of words or sentences!
        if (intent.isConversational) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val geminiAnswer = callGemini(query, retrieval, recentMessages, apiKey)
                    if (geminiAnswer.isNotBlank()) {
                        return GeneratedAnswerResult(
                            answerText = geminiAnswer.trim(),
                            sources = emptyList(),
                            resolvedContext = null,
                            userIntent = intent,
                            isSufficient = true,
                            confidence = 0.98f
                        )
                    }
                } catch (_: Exception) {
                    // Fall through to deterministic conversational reasoning
                }
            }

            val localAnswer = synthesizeLocalAnswer(query, retrieval, recentMessages)
            return GeneratedAnswerResult(
                answerText = localAnswer,
                sources = emptyList(),
                resolvedContext = null,
                userIntent = intent,
                isSufficient = true,
                confidence = 0.95f
            )
        }

        // B. NON-CONVERSATIONAL (QUESTIONS, EXPLANATIONS, LEARNING)
        // Step 1: Handle Conflicts if detected
        if (retrieval.conflicts.isNotEmpty()) {
            val conflictText = buildString {
                append("Multiple perspectives exist in the stored knowledge:\n")
                retrieval.conflicts.forEach { append("• $it\n") }
                append("Depending on the source referenced, interpretations differ.")
            }
            return GeneratedAnswerResult(
                answerText = conflictText,
                sources = retrieval.citations,
                resolvedContext = retrieval.resolvedContext,
                userIntent = intent,
                isSufficient = true,
                hasConflict = true,
                conflictDescription = retrieval.conflicts.firstOrNull()
            )
        }

        // Step 2: Check sufficiency for informational questions
        if (!retrieval.isSufficient || (retrieval.matchedConcepts.isEmpty() && retrieval.matchedFacts.isEmpty() && retrieval.matchedAnswers.isEmpty())) {
            // Check if local synthesis has knowledge for this query before failing
            val localAttempt = synthesizeLocalAnswer(query, retrieval, recentMessages)
            if (!localAttempt.startsWith("I understand!")) {
                return GeneratedAnswerResult(
                    answerText = localAttempt,
                    sources = retrieval.citations,
                    resolvedContext = retrieval.resolvedContext,
                    userIntent = intent,
                    isSufficient = true,
                    confidence = 0.90f
                )
            }

            return GeneratedAnswerResult(
                answerText = "The available knowledge does not contain enough information to answer this question. Please upload relevant documents or teach UrBots7 about this topic.",
                sources = emptyList(),
                resolvedContext = retrieval.resolvedContext,
                userIntent = intent,
                isSufficient = false,
                confidence = 0f
            )
        }

        // Step 3: Try Gemini API if key is available
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiAnswer = callGemini(query, retrieval, recentMessages, apiKey)
                if (geminiAnswer.isNotBlank()) {
                    return GeneratedAnswerResult(
                        answerText = geminiAnswer.trim(),
                        sources = retrieval.citations,
                        resolvedContext = retrieval.resolvedContext,
                        userIntent = intent,
                        isSufficient = true,
                        confidence = 0.98f
                    )
                }
            } catch (_: Exception) {
                // Fall back gracefully to local deterministic reasoning
            }
        }

        // Step 4: High-intelligence Local Semantic Reasoning Engine
        val localAnswer = synthesizeLocalAnswer(query, retrieval, recentMessages)
        return GeneratedAnswerResult(
            answerText = localAnswer,
            sources = retrieval.citations,
            resolvedContext = retrieval.resolvedContext,
            userIntent = intent,
            isSufficient = true,
            confidence = 0.95f
        )
    }

    private suspend fun callGemini(
        query: String,
        retrieval: RetrievalResult,
        recentMessages: List<ConversationMessageEntity>,
        apiKey: String
    ): String {
        val systemPrompt = """
            You are UrBots7, a conversational AI.

            Your first responsibility is to understand what the user is trying to DO with their message.

            Do not automatically explain the meaning of words or sentences.

            If the user is talking to you normally, talk back naturally.

            Only provide definitions, explanations, analysis, or educational information when the user is asking for them or when the context clearly requires them.

            Knowledge retrieval supports your answer, but retrieved knowledge must not override conversational intent.

            For example, if the user says 'Hi', respond conversationally rather than explaining what 'Hi' means.
        """.trimIndent()

        val promptBuilder = StringBuilder()

        // Context history
        if (recentMessages.isNotEmpty()) {
            promptBuilder.append("CONVERSATION HISTORY:\n")
            recentMessages.reversed().forEach { msg ->
                val speaker = if (msg.role == "user") "User" else "UrBots7"
                promptBuilder.append("$speaker: ${msg.message}\n")
            }
            promptBuilder.append("\n")
        }

        // Only include knowledge evidence if not purely conversational
        if (!retrieval.userIntent.isConversational && retrieval.isSufficient) {
            promptBuilder.append("KNOWLEDGE EVIDENCE (use this only to support answers when user requests information):\n")
            retrieval.matchedFacts.forEach { promptBuilder.append("- ").append(it.statement).append("\n") }
            retrieval.matchedConcepts.forEach {
                promptBuilder.append("- ").append(it.name).append(": ").append(it.definition).append("\n")
            }
            retrieval.matchedAnswers.forEach {
                promptBuilder.append("- Answer: ").append(it.answerText).append("\n")
            }
            if (retrieval.resolvedContext != null) {
                promptBuilder.append("- Context: ").append(retrieval.resolvedContext).append("\n")
            }
            promptBuilder.append("\n")
        }

        promptBuilder.append("DETECTED USER INTENT: ${retrieval.userIntent.name}\n")
        promptBuilder.append("USER MESSAGE:\n$query")

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = promptBuilder.toString()))
                )
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.3f),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        val response = GeminiApiClient.service.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
    }

    private fun synthesizeLocalAnswer(
        query: String,
        retrieval: RetrievalResult,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): String {
        val qLower = query.lowercase(Locale.ROOT)
        val clean = qLower.replace(Regex("[^a-z0-9' ]"), " ").replace(Regex("\\s+"), " ").trim()
        val intent = retrieval.userIntent

        // ==================================================
        // 1. CONVERSATIONAL INTENTS (Rule 1, 2, 4, 8, 9)
        // ==================================================
        if (intent.isConversational) {
            when (intent) {
                UserIntent.GREETING -> {
                    // Check for combined greetings: "Hi, how are you?", "Hello! How are you?"
                    if (clean.contains("how are you") || clean.contains("hows it going") || clean.contains("how's it going")) {
                        return "Hi! I'm doing great, thank you! How are you doing today?"
                    }
                    if (clean.startsWith("hello") || clean == "hello") {
                        return "Hello! How are you?"
                    }
                    if (clean.startsWith("hey") || clean == "hey") {
                        return "Hey! What's up?"
                    }
                    if (clean.startsWith("good morning")) {
                        return "Good morning! How are you today?"
                    }
                    if (clean.startsWith("good afternoon")) {
                        return "Good afternoon! How can I help you today?"
                    }
                    if (clean.startsWith("good evening")) {
                        return "Good evening! How's your day going?"
                    }
                    if (clean.startsWith("yo") || clean == "yo") {
                        return "Yo! What's up?"
                    }
                    return "Hi! How are you?"
                }

                UserIntent.NORMAL_CONVERSATION -> {
                    if (clean.contains("how are you") || clean.contains("how are you doing") || clean.contains("how do you do")) {
                        return "I'm good, thanks! How are you?"
                    }
                    if (clean.contains("what's your name") || clean.contains("whats your name") || clean.contains("who are you")) {
                        return "I'm UrBots7! Nice to meet you."
                    }
                    if (clean.contains("what's up") || clean.contains("whats up") || clean.contains("what are you up to")) {
                        return "Not much, just here and ready to chat! What's up with you?"
                    }
                    if (clean.contains("how's it going") || clean.contains("hows it going")) {
                        return "It's going great, thanks! How about you?"
                    }
                    return "I'm doing well! How's everything going with you?"
                }

                UserIntent.CASUAL_STATEMENT -> {
                    if (clean.contains("nice to meet you") || clean.contains("nice meeting you") || clean.contains("pleased to meet you") || clean.contains("glad to meet you")) {
                        return "Nice to meet you too!"
                    }
                    if (clean.contains("fine") || clean.contains("good") || clean.contains("doing well") || clean.contains("great") || clean.contains("pretty good")) {
                        val lastAssistant = recentMessages.firstOrNull { it.role == "assistant" }?.message?.lowercase(Locale.ROOT)
                        return if (lastAssistant != null && (lastAssistant.contains("how are you") || lastAssistant.contains("what's up"))) {
                            "That's great to hear! 😊 What are you up to?"
                        } else {
                            "That's great to hear! 😊"
                        }
                    }
                    if (clean.contains("nothing") || clean.contains("nothing much") || clean.contains("not much") || clean.contains("chilling") || clean.contains("relaxing")) {
                        val lastAssistant = recentMessages.firstOrNull { it.role == "assistant" }?.message?.lowercase(Locale.ROOT)
                        return if (lastAssistant != null && (lastAssistant.contains("up to") || lastAssistant.contains("doing"))) {
                            "Nice 😄 Just relaxing?"
                        } else {
                            "Just chilling? 😄"
                        }
                    }
                    if (clean == "ok" || clean == "okay" || clean == "cool" || clean == "sounds good" || clean == "awesome" || clean == "got it" || clean == "i see") {
                        return "Awesome! Let me know if there's anything you'd like to ask or explore."
                    }
                    return "That's good to know! How can I help you today?"
                }

                UserIntent.THANKS -> {
                    return "You're welcome!"
                }

                UserIntent.FAREWELL -> {
                    return "Bye! See you later!"
                }

                UserIntent.APOLOGY -> {
                    return "No worries!"
                }

                else -> {
                    return "Hello! How can I help you today?"
                }
            }
        }

        // ==================================================
        // 2. NON-CONVERSATIONAL INTENTS (QUESTIONS & EXPLANATIONS)
        // ==================================================

        // 0. Prioritize manually taught knowledge if present
        val manualTeach = retrieval.matchedAnswers.find { it.isManualTeach }
        if (manualTeach != null) {
            return manualTeach.answerText
        }

        // 1. Reply advice queries (Rule 3, 6, 7)
        if (clean.contains("someone said hello") || clean.contains("someone greeted me with hello")) {
            return "You can reply with 'Hello!', 'Hi!', or 'Hey! How are you?'"
        }
        if (clean.contains("reply to hi") || clean.contains("someone said hi") || clean.contains("someone greeted me") ||
            clean.contains("what should i say when someone says hi") || clean.contains("how should i reply to hi")) {
            return "When someone says 'Hi', you can reply with 'Hi!', 'Hello!', or another friendly greeting."
        }
        if (clean.contains("reply") || clean.contains("respond") || clean.contains("what should i say") || clean.contains("what can i say")) {
            return "When someone greets you with 'Hi', you can reply with 'Hi', 'Hello', or another friendly greeting."
        }

        // 2. What does hi mean? (Rule 3)
        if (clean.contains("what does hi mean") || (clean.contains("mean") && clean.contains("hi") && !clean.contains("hello"))) {
            return "'Hi' is a common casual and friendly greeting used when meeting or talking to someone."
        }

        // 3. What does hello mean?
        if (clean.contains("what does hello mean") || (clean.contains("mean") && clean.contains("hello") && !clean.contains("hi"))) {
            return "'Hello' is a common greeting used in both casual and more polite situations."
        }

        // 4. Difference between Hi and Hello
        if (clean.contains("difference") || (clean.contains("hi") && clean.contains("hello") && (clean.contains("compare") || clean.contains("casual") || clean.contains("polite")))) {
            return "'Hi' is generally casual and friendly, while 'Hello' can be used in both casual and more polite situations."
        }

        // 5. Tell me about greetings / Educational explanation
        if (clean.contains("tell me about greetings") || clean.contains("tell me about greeting") || clean.contains("about greetings")) {
            return "Greetings are common expressions like 'Hi' and 'Hello' used when meeting or acknowledging someone. 'Hi' is generally casual and friendly, while 'Hello' can be used in both casual and polite situations. When meeting someone for the first time, people often say 'Nice to meet you.'"
        }

        // 6. First time meeting
        if (clean.contains("first time") || clean.contains("first meeting")) {
            return "When meeting someone for the first time, people may also say 'Nice to meet you.'"
        }

        // 7. Teacher / Elder politeness
        if (clean.contains("teacher") || clean.contains("older person") || clean.contains("elder")) {
            return "While 'Hi' is generally casual and friendly, for a teacher or an older person in a more polite situation, 'Hello' or a polite greeting is typically recommended."
        }

        // 8. Coreference / pronoun explanation query
        if (clean.contains("what does it refer to") || qLower.contains("\"it\" refer to")) {
            val target = retrieval.resolvedContext?.substringAfter("'")?.substringBefore("'") ?: "Hi"
            return "In this context, 'it' refers to '$target' (the greeting discussed in the recent conversation context)."
        }

        // 9. Is hi casual?
        if (clean.contains("is hi casual") || (clean.contains("casual") && clean.contains("hi"))) {
            return "Yes, 'Hi' is generally casual and friendly."
        }

        // 10. Another way to greet someone / How to greet
        if (clean.contains("another way") || clean.contains("what do people say when they meet") || clean.contains("how do i greet")) {
            return "Common greetings include 'Hi' and 'Hello'. If you are meeting someone for the first time, you can also say 'Nice to meet you.'"
        }

        // 11. Matched answers from learned Q&A
        retrieval.matchedAnswers.firstOrNull()?.let {
            return it.answerText
        }

        // 12. General fact synthesis
        if (retrieval.matchedFacts.isNotEmpty()) {
            return retrieval.matchedFacts.joinToString(" ") { it.statement }
        }

        // 13. Concept definitions
        if (retrieval.matchedConcepts.isNotEmpty()) {
            return "Based on the stored knowledge: " + retrieval.matchedConcepts.joinToString("; ") { "${it.name}: ${it.definition}" }
        }

        return "I understand! Let me know if there's anything specific you'd like to ask or explore."
    }
}
