package com.example.domain.engine

import com.example.BuildConfig
import com.example.data.local.entity.ConversationMessageEntity
import com.example.data.remote.*
import com.example.domain.model.GeneratedAnswerResult
import com.example.domain.model.MessageSemanticAnalysis
import com.example.domain.model.RetrievalResult
import com.example.domain.model.UserIntent
import java.util.Locale

class AnswerGenerator {

    suspend fun generateAnswer(
        query: String,
        retrieval: RetrievalResult,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): GeneratedAnswerResult {
        val analysis = retrieval.semanticAnalysis ?: SemanticUnderstandingEngine.analyze(query, recentMessages)
        val intent = analysis.intent

        var candidateAnswer: String

        // 1. HELP / PROBLEM REPORTING INTENTS (Rule 1, 3, 20)
        // Must NEVER generate unrelated knowledge definitions or "I'm doing well"!
        if (intent.isHelpOrProblem) {
            val localDraft = synthesizeProblemOrHelpAnswer(analysis, recentMessages)
            candidateAnswer = localDraft

            // Try Gemini if available
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val geminiAnswer = callGemini(query, retrieval, analysis, recentMessages, apiKey)
                    if (geminiAnswer.isNotBlank()) {
                        candidateAnswer = geminiAnswer.trim()
                    }
                } catch (_: Exception) {
                    // Retain localDraft
                }
            }

            // Semantic Relevance Validation
            val validation = ResponseRelevanceValidator.validateAndRefine(candidateAnswer, analysis, recentMessages)
            return GeneratedAnswerResult(
                answerText = validation.refinedAnswer,
                sources = emptyList(),
                resolvedContext = if (analysis.referencedEntities.isNotEmpty()) analysis.referencedEntities.first() else null,
                userIntent = intent,
                semanticAnalysis = analysis,
                isSufficient = true,
                confidence = 0.98f
            )
        }

        // 2. CONVERSATIONAL INTENTS (Greetings, Casual Chat, Farewells, Thanks)
        if (intent.isConversational) {
            candidateAnswer = synthesizeLocalAnswer(query, retrieval, analysis, recentMessages)

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val geminiAnswer = callGemini(query, retrieval, analysis, recentMessages, apiKey)
                    if (geminiAnswer.isNotBlank()) {
                        candidateAnswer = geminiAnswer.trim()
                    }
                } catch (_: Exception) {
                    // Fall back to candidateAnswer
                }
            }

            // Semantic Relevance Validation
            val validation = ResponseRelevanceValidator.validateAndRefine(candidateAnswer, analysis, recentMessages)
            return GeneratedAnswerResult(
                answerText = validation.refinedAnswer,
                sources = emptyList(),
                resolvedContext = null,
                userIntent = intent,
                semanticAnalysis = analysis,
                isSufficient = true,
                confidence = 0.98f
            )
        }

        // 3. FOLLOW UP / CONTINUATION
        if (intent == UserIntent.FOLLOW_UP) {
            candidateAnswer = synthesizeFollowUpAnswer(analysis, recentMessages)
            val validation = ResponseRelevanceValidator.validateAndRefine(candidateAnswer, analysis, recentMessages)
            return GeneratedAnswerResult(
                answerText = validation.refinedAnswer,
                sources = emptyList(),
                resolvedContext = if (analysis.referencedEntities.isNotEmpty()) analysis.referencedEntities.first() else null,
                userIntent = intent,
                semanticAnalysis = analysis,
                isSufficient = true,
                confidence = 0.95f
            )
        }

        // 4. KNOWLEDGE QUESTIONS, EXPLANATIONS & INSTRUCTIONS
        // Check for knowledge conflicts
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
                semanticAnalysis = analysis,
                isSufficient = true,
                hasConflict = true,
                conflictDescription = retrieval.conflicts.firstOrNull()
            )
        }

        // Check sufficiency for informational queries
        val localAttempt = synthesizeLocalAnswer(query, retrieval, analysis, recentMessages)
        val hasLocalKnowledge = !localAttempt.startsWith("I understand! Let me know")

        if (!retrieval.isSufficient && (retrieval.matchedConcepts.isEmpty() && retrieval.matchedFacts.isEmpty() && retrieval.matchedAnswers.isEmpty())) {
            if (hasLocalKnowledge) {
                return GeneratedAnswerResult(
                    answerText = localAttempt,
                    sources = retrieval.citations,
                    resolvedContext = retrieval.resolvedContext,
                    userIntent = intent,
                    semanticAnalysis = analysis,
                    isSufficient = true,
                    confidence = 0.90f
                )
            }

            return GeneratedAnswerResult(
                answerText = "The available knowledge does not contain enough information to answer this question. Please upload relevant documents or teach UrBots7 about this topic.",
                sources = emptyList(),
                resolvedContext = retrieval.resolvedContext,
                userIntent = intent,
                semanticAnalysis = analysis,
                isSufficient = false,
                confidence = 0f
            )
        }

        candidateAnswer = localAttempt

        // Try Gemini API if key is available
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiAnswer = callGemini(query, retrieval, analysis, recentMessages, apiKey)
                if (geminiAnswer.isNotBlank()) {
                    candidateAnswer = geminiAnswer.trim()
                }
            } catch (_: Exception) {
                // Fall back gracefully to local deterministic reasoning
            }
        }

        // Final semantic relevance check
        val validation = ResponseRelevanceValidator.validateAndRefine(candidateAnswer, analysis, recentMessages)
        return GeneratedAnswerResult(
            answerText = validation.refinedAnswer,
            sources = retrieval.citations,
            resolvedContext = retrieval.resolvedContext,
            userIntent = intent,
            semanticAnalysis = analysis,
            isSufficient = true,
            confidence = 0.95f
        )
    }

    private fun synthesizeProblemOrHelpAnswer(
        analysis: MessageSemanticAnalysis,
        recentMessages: List<ConversationMessageEntity>
    ): String {
        return when (analysis.intent) {
            UserIntent.REPORTING_PROBLEM -> {
                val topic = analysis.topic
                when {
                    topic != null && topic.contains("roblox", ignoreCase = true) ->
                        "Sure, tell me what's wrong with your Roblox game and I'll try to help."
                    topic != null ->
                        "Sure, tell me what's wrong with your $topic and I'll try to help."
                    else ->
                        "I'm sorry to hear that. What's the problem? I'll try to help."
                }
            }
            UserIntent.REQUESTING_HELP -> {
                val topic = analysis.topic
                if (topic != null) {
                    "Of course! What do you need help with regarding your $topic?"
                } else {
                    "Of course! What do you need help with?"
                }
            }
            UserIntent.CONFUSION -> {
                "No worries. Tell me what's confusing you."
            }
            else -> "I'm here to help. What's going on?"
        }
    }

    private fun synthesizeFollowUpAnswer(
        analysis: MessageSemanticAnalysis,
        recentMessages: List<ConversationMessageEntity>
    ): String {
        val clean = analysis.rawText.lowercase(Locale.ROOT)
        if (clean.contains("disappear") || clean.contains("vanish")) {
            return "When objects disappear upon publishing in Roblox Studio, it's often caused by Archivable being set to false, streaming enabled issues, or parts not being anchored. Have you checked if the parts are anchored or if any scripts run on startup?"
        }
        val topic = analysis.topic ?: "that"
        return "I understand. Let's look closer at $topic. Can you share more details about what happens?"
    }

    private suspend fun callGemini(
        query: String,
        retrieval: RetrievalResult,
        analysis: MessageSemanticAnalysis,
        recentMessages: List<ConversationMessageEntity>,
        apiKey: String
    ): String {
        val systemPrompt = """
            You are UrBots7, a conversational AI with semantic reasoning and knowledge capabilities.

            Your first responsibility is to understand what the user is trying to DO with their message.
            Never behave like a keyword-matching chatbot. Prioritize whole semantic meaning.

            Do not automatically explain the meaning of words or sentences.
            If the user is talking to you normally, talk back naturally.
            If the user reports a problem or asks for help, be empathetic and ask what the problem is to help them.
            Only provide definitions, explanations, analysis, or educational information when the user is asking for them or when the context clearly requires them.

            Knowledge retrieval supports your answer, but retrieved knowledge must not override conversational intent.
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

        // Only include knowledge evidence if not purely conversational or problem reporting
        if (!analysis.intent.isConversational && !analysis.intent.isHelpOrProblem && retrieval.isSufficient) {
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

        promptBuilder.append("SEMANTIC ANALYSIS:\n")
        promptBuilder.append("- Detected Intent: ${analysis.intent.name}\n")
        promptBuilder.append("- Detected Meaning: ${analysis.detectedMeaning}\n")
        if (analysis.topic != null) promptBuilder.append("- Topic: ${analysis.topic}\n")
        promptBuilder.append("\nUSER MESSAGE:\n$query")

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
        analysis: MessageSemanticAnalysis,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): String {
        val qLower = query.lowercase(Locale.ROOT)
        val clean = qLower.replace(Regex("[^a-z0-9' ]"), " ").replace(Regex("\\s+"), " ").trim()
        val intent = analysis.intent

        // 1. CONVERSATIONAL INTENTS
        if (intent.isConversational) {
            when (intent) {
                UserIntent.GREETING -> {
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

                UserIntent.CASUAL_CONVERSATION, UserIntent.NORMAL_CONVERSATION -> {
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
                    if (clean.contains("nice to meet you") || clean.contains("pleased to meet you")) {
                        return "Nice to meet you too!"
                    }
                    if (clean.contains("tired") || clean.contains("exhausted") || clean.contains("eyes open") || clean.contains("sleepy") || clean.contains("falling asleep")) {
                        return "I'm sorry to hear that you're so exhausted! Please make sure to get some rest and take care of yourself."
                    }
                    if (clean.contains("full") && (clean.contains("feel") || clean.contains("ate") || clean.contains("dinner") || clean.contains("food"))) {
                        return "Sounds like you had a very satisfying meal! Take it easy and relax 😊"
                    }
                    if (clean.contains("fine") || clean.contains("good") || clean.contains("doing well") || clean.contains("great")) {
                        val lastAssistant = recentMessages.firstOrNull { it.role == "assistant" }?.message?.lowercase(Locale.ROOT)
                        return if (lastAssistant != null && (lastAssistant.contains("how are you") || lastAssistant.contains("what's up"))) {
                            "That's great to hear! 😊 What are you up to?"
                        } else {
                            "That's great to hear! 😊"
                        }
                    }
                    if (clean.contains("nothing") || clean.contains("nothing much") || clean.contains("not much") || clean.contains("chilling")) {
                        return "Nice 😄 Just relaxing?"
                    }
                    return "I'm doing well! How's everything going with you?"
                }

                UserIntent.CASUAL_STATEMENT -> {
                    if (clean.contains("fine") || clean.contains("good") || clean.contains("doing well") || clean.contains("great")) {
                        return "That's great to hear! 😊"
                    }
                    if (clean.contains("nothing") || clean.contains("not much")) {
                        return "Nice 😄 Just relaxing?"
                    }
                    return "Got it! Let me know if there's anything you'd like to chat about."
                }

                UserIntent.THANKING, UserIntent.THANKS -> {
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

        // 2. KNOWLEDGE RETRIEVAL QUESTIONS & EXPLANATIONS
        // Prioritize manually taught knowledge if present
        val manualTeach = retrieval.matchedAnswers.find { it.isManualTeach }
        if (manualTeach != null) {
            return manualTeach.answerText
        }

        // Reply advice queries (Rule 4, 7, 17)
        if (clean.contains("someone said hello") || clean.contains("someone greeted me with hello")) {
            return "You can reply with 'Hello!', 'Hi!', or 'Hey! How are you?'"
        }
        if (clean.contains("reply to hi") || clean.contains("someone said hi") || clean.contains("someone greeted me") ||
            clean.contains("what should i say when someone says hi") || clean.contains("how should i reply to hi") ||
            clean.contains("how do i reply to hi")) {
            return "When someone says 'Hi', you can reply with 'Hi!', 'Hello!', or another friendly greeting."
        }
        if (clean.contains("reply to hello") || clean.contains("how should i reply to hello") || clean.contains("how do i reply to hello")) {
            return "When someone says 'Hello', you can reply with 'Hello!', 'Hi!', or 'Hey! How are you?'"
        }
        if (clean.contains("reply") || clean.contains("respond") || clean.contains("what should i say") || clean.contains("what can i say")) {
            return "When someone greets you with 'Hi', you can reply with 'Hi', 'Hello', or another friendly greeting."
        }

        // What does hi mean? (Rule 3)
        if (clean.contains("what does hi mean") || (clean.contains("mean") && clean.contains("hi") && !clean.contains("hello"))) {
            return "'Hi' is a common casual and friendly greeting used when meeting or talking to someone."
        }

        // What does hello mean?
        if (clean.contains("what does hello mean") || (clean.contains("mean") && clean.contains("hello") && !clean.contains("hi"))) {
            return "'Hello' is a common greeting used in both casual and more polite situations."
        }

        // Difference between Hi and Hello
        if (clean.contains("difference") || (clean.contains("hi") && clean.contains("hello") && (clean.contains("compare") || clean.contains("casual") || clean.contains("polite")))) {
            return "'Hi' is generally casual and friendly, while 'Hello' can be used in both casual and more polite situations."
        }

        // Tell me about greetings / Educational explanation
        if (clean.contains("tell me about greetings") || clean.contains("tell me about greeting") || clean.contains("about greetings")) {
            return "Greetings are common expressions like 'Hi' and 'Hello' used when meeting or acknowledging someone. 'Hi' is generally casual and friendly, while 'Hello' can be used in both casual and polite situations. When meeting someone for the first time, people often say 'Nice to meet you.'"
        }

        // First time meeting
        if (clean.contains("first time") || clean.contains("first meeting")) {
            return "When meeting someone for the first time, people may also say 'Nice to meet you.'"
        }

        // Teacher / Elder politeness
        if (clean.contains("teacher") || clean.contains("older person") || clean.contains("elder")) {
            return "While 'Hi' is generally casual and friendly, for a teacher or an older person in a more polite situation, 'Hello' or a polite greeting is typically recommended."
        }

        // Coreference query
        if (clean.contains("what does it refer to") || qLower.contains("\"it\" refer to")) {
            val target = retrieval.resolvedContext?.substringAfter("'")?.substringBefore("'") ?: "Hi"
            return "In this context, 'it' refers to '$target' (the greeting discussed in the recent conversation context)."
        }

        // Is hi casual?
        if (clean.contains("is hi casual") || (clean.contains("casual") && clean.contains("hi"))) {
            return "Yes, 'Hi' is generally casual and friendly."
        }

        // Another way to greet someone / How to greet
        if (clean.contains("another way") || clean.contains("what do people say when they meet") || clean.contains("how do i greet")) {
            return "Common greetings include 'Hi' and 'Hello'. If you are meeting someone for the first time, you can also say 'Nice to meet you.'"
        }

        // Matched answers from learned Q&A
        retrieval.matchedAnswers.firstOrNull()?.let {
            return it.answerText
        }

        // General fact synthesis
        if (retrieval.matchedFacts.isNotEmpty()) {
            return retrieval.matchedFacts.joinToString(" ") { it.statement }
        }

        // Concept definitions
        if (retrieval.matchedConcepts.isNotEmpty()) {
            return "Based on the stored knowledge: " + retrieval.matchedConcepts.joinToString("; ") { "${it.name}: ${it.definition}" }
        }

        return "I understand! Let me know if there's anything specific you'd like to ask or explore."
    }
}
