package com.example.domain.engine

import com.example.data.local.entity.ConversationMessageEntity
import com.example.domain.model.MessageSemanticAnalysis
import com.example.domain.model.UserIntent
import java.util.Locale

object SemanticUnderstandingEngine {

    fun analyze(
        rawQuery: String,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): MessageSemanticAnalysis {
        val trimmed = rawQuery.trim()
        val qLower = trimmed.lowercase(Locale.ROOT)
        val clean = qLower.replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val noApos = clean.replace("'", "")

        // 1. Resolve coreference and pronoun context from recent turns
        val (referencedEntities, contextSubject) = resolveReferencedEntities(clean, noApos, recentMessages)

        // 2. Extract potential entities and topic
        val extractedEntities = extractEntities(clean, contextSubject)
        val detectedTopic = extractTopic(clean, contextSubject, extractedEntities)

        // 3. Detect Intent semantically (whole meaning, not simple token counting)

        // Category A: REQUESTING_EXPLANATION (Rule 3)
        if (isExplanationRequest(clean, noApos)) {
            val subject = detectedTopic ?: "requested topic"
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.REQUESTING_EXPLANATION,
                topic = subject,
                entities = extractedEntities,
                action = "request_explanation",
                speechAct = "asking_for_definition",
                sentiment = "neutral",
                conversationGoal = "Learn definition or explanation of $subject",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is asking for an educational explanation or definition of '$subject'."
            )
        }

        // Category B: REQUESTING_INSTRUCTIONS (Rule 4 & 7)
        if (isReplyAdviceRequest(clean, noApos)) {
            val subject = detectedTopic ?: "greeting"
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.REQUESTING_INSTRUCTIONS,
                topic = subject,
                entities = extractedEntities,
                action = "request_advice",
                speechAct = "asking_for_instructions",
                sentiment = "neutral",
                conversationGoal = "Learn how to reply or what to say",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is asking what to say or how to respond regarding '$subject'."
            )
        }

        // Category C: REPORTING_PROBLEM (Rule 1 & 4)
        if (isReportingProblem(clean, noApos)) {
            val topicDesc = detectedTopic ?: "general issue"
            val meaning = if (detectedTopic != null) {
                "User is reporting a problem with '$detectedTopic' and is seeking troubleshooting assistance."
            } else {
                "User is reporting an issue or difficulty and wants assistance."
            }
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.REPORTING_PROBLEM,
                topic = detectedTopic,
                entities = extractedEntities,
                action = "report_problem",
                speechAct = "reporting_problem",
                sentiment = "concerned",
                conversationGoal = "Get troubleshooting assistance",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = meaning
            )
        }

        // Category D: REQUESTING_HELP (Rule 1 & 4)
        if (isRequestingHelp(clean, noApos)) {
            val topicDesc = detectedTopic ?: "general task"
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.REQUESTING_HELP,
                topic = detectedTopic,
                entities = extractedEntities,
                action = "request_help",
                speechAct = "requesting_help",
                sentiment = "concerned",
                conversationGoal = "Request assistance from UrBots7",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is requesting help with $topicDesc."
            )
        }

        // Category E: CONFUSION (Rule 3)
        if (isExpressingConfusion(clean, noApos)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.CONFUSION,
                topic = detectedTopic ?: contextSubject,
                entities = extractedEntities,
                action = "express_confusion",
                speechAct = "expressing_confusion",
                sentiment = "confused",
                conversationGoal = "Seek clarification or simplified explanation",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is confused and needs guidance or clarification."
            )
        }

        // Category F: GREETING (Rule 2)
        if (isGreeting(clean, noApos)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.GREETING,
                topic = "Greeting",
                entities = extractedEntities,
                action = "greet",
                speechAct = "greeting",
                sentiment = "positive",
                conversationGoal = "Initiate friendly conversation",
                referencedEntities = emptyList(),
                confidence = "High",
                detectedMeaning = "User is saying hello in a conversational manner."
            )
        }

        // Category G: FAREWELL
        if (isFarewell(clean, noApos)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.FAREWELL,
                topic = "Farewell",
                entities = emptyList(),
                action = "farewell",
                speechAct = "parting",
                sentiment = "neutral",
                conversationGoal = "Conclude conversation",
                referencedEntities = emptyList(),
                confidence = "High",
                detectedMeaning = "User is saying goodbye."
            )
        }

        // Category H: THANKING
        if (isThanks(clean, noApos)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.THANKING,
                topic = "Gratitude",
                entities = emptyList(),
                action = "thank",
                speechAct = "expressing_gratitude",
                sentiment = "positive",
                conversationGoal = "Acknowledge assistance",
                referencedEntities = emptyList(),
                confidence = "High",
                detectedMeaning = "User is expressing gratitude."
            )
        }

        // Category I: APOLOGY
        if (isApology(clean, noApos)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.APOLOGY,
                topic = "Apology",
                entities = emptyList(),
                action = "apologize",
                speechAct = "apology",
                sentiment = "neutral",
                conversationGoal = "Apologize politely",
                referencedEntities = emptyList(),
                confidence = "High",
                detectedMeaning = "User is offering a polite apology."
            )
        }

        // Category J: CASUAL_CONVERSATION
        if (isCasualConversation(clean, noApos)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.CASUAL_CONVERSATION,
                topic = detectedTopic ?: "Casual chat",
                entities = extractedEntities,
                action = "converse",
                speechAct = "casual_statement",
                sentiment = detectSentiment(clean),
                conversationGoal = "Social interaction",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is conversing casually."
            )
        }

        // Category K: FOLLOW_UP
        if (isFollowUp(clean, noApos, recentMessages, referencedEntities)) {
            val subject = contextSubject ?: detectedTopic ?: "the ongoing topic"
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.FOLLOW_UP,
                topic = subject,
                entities = extractedEntities,
                action = "continue_conversation",
                speechAct = "continuing",
                sentiment = detectSentiment(clean),
                conversationGoal = "Elaborate on $subject",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is continuing previous discussion regarding '$subject'."
            )
        }

        // Category L: GENERAL QUESTION
        if (trimmed.endsWith("?") || isQuestionStarter(clean)) {
            return MessageSemanticAnalysis(
                rawText = rawQuery,
                intent = UserIntent.QUESTION,
                topic = detectedTopic ?: "Inquiry",
                entities = extractedEntities,
                action = "ask_question",
                speechAct = "question",
                sentiment = "neutral",
                conversationGoal = "Inquire for information",
                referencedEntities = referencedEntities,
                confidence = "High",
                detectedMeaning = "User is asking an informational question."
            )
        }

        // Fallback: STATEMENT / OTHER
        return MessageSemanticAnalysis(
            rawText = rawQuery,
            intent = UserIntent.STATEMENT,
            topic = detectedTopic,
            entities = extractedEntities,
            action = "state_information",
            speechAct = "statement",
            sentiment = detectSentiment(clean),
            conversationGoal = "Share statement or message",
            referencedEntities = referencedEntities,
            confidence = "Medium",
            detectedMeaning = "User is sharing a statement with UrBots7."
        )
    }

    // --- Semantic Matchers ---

    private fun isReportingProblem(clean: String, noApos: String): Boolean {
        // "I have a problem", "I'm having an issue", "Something is wrong", "I'm facing a difficulty",
        // "Something isn't working", "My Roblox game isn't working", "There's an issue", "I'm stuck"
        if (clean.contains("have a problem") || clean.contains("having a problem") || clean.contains("got a problem")) return true
        if (clean.contains("have an issue") || clean.contains("having an issue") || clean.contains("there is an issue") || clean.contains("there's an issue")) return true
        if (clean.contains("something is wrong") || clean.contains("somethings wrong") || clean.contains("something went wrong")) return true
        if (clean.contains("facing a difficulty") || clean.contains("facing difficulties") || clean.contains("having trouble")) return true
        if (clean.contains("is not working") || clean.contains("isn't working") || clean.contains("isnt working")) return true
        if (clean.contains("does not work") || clean.contains("doesn't work") || clean.contains("doesnt work")) return true
        if (clean.contains("im stuck") || clean.contains("i'm stuck") || clean.contains("i am stuck")) return true
        if (clean.contains("broken") || clean.contains("failed") || clean.contains("failing")) return true
        if (clean.contains("disappears when") || clean.contains("crashes when") || clean.contains("won't start") || clean.contains("wont start")) return true
        return false
    }

    private fun isRequestingHelp(clean: String, noApos: String): Boolean {
        if (clean.contains("need help") || clean.contains("need some help") || clean.contains("need assistance")) return true
        if (clean.contains("can you help") || clean.contains("could you help") || clean.contains("please help")) return true
        if (clean == "help" || clean == "help me" || clean.startsWith("help me with")) return true
        return false
    }

    private fun isExpressingConfusion(clean: String, noApos: String): Boolean {
        if (clean.contains("confused") || clean.contains("confusing")) return true
        if (clean.contains("dont understand") || clean.contains("don't understand") || clean.contains("do not understand")) return true
        if (clean.contains("dont get it") || clean.contains("don't get it") || clean.contains("do not get it")) return true
        return false
    }

    private fun isFollowUp(
        clean: String,
        noApos: String,
        recentMessages: List<ConversationMessageEntity>,
        referencedEntities: List<String>
    ): Boolean {
        if (recentMessages.isEmpty()) return false
        val lastAssistant = recentMessages.firstOrNull { it.role == "assistant" } ?: return false

        // A follow-up should NEVER be a greeting, farewell, thanks, apology, casual phrase, or advice/explanation request
        if (isGreeting(clean, noApos) || isFarewell(clean, noApos) || isThanks(clean, noApos) ||
            isApology(clean, noApos) || isCasualConversation(clean, noApos) ||
            isExplanationRequest(clean, noApos) || isReplyAdviceRequest(clean, noApos)) {
            return false
        }

        // If assistant asked a clarifying question ("What's wrong with it?", "What are you up to?")
        val lastText = lastAssistant.message.lowercase(Locale.ROOT)
        val assistantAsked = lastText.endsWith("?") || lastText.contains("what's wrong") ||
                lastText.contains("tell me") || lastText.contains("what happens")

        // If user uses pronouns referencing previous subject:
        val hasPronoun = clean.startsWith("it ") || clean.startsWith("it's ") || clean.startsWith("its ") ||
                clean.startsWith("that ") || clean.startsWith("this ") || clean.contains(" it ") || clean.contains(" that ") ||
                clean.contains("the problem") || clean.contains("the game") || clean.contains("the book")

        val hasDiagnosticDetail = clean.contains("disappear") || clean.contains("vanish") || clean.contains("crash") ||
                clean.contains("freeze") || clean.contains("error") || clean.contains("publish") || clean.contains("anchor") ||
                clean.contains("script") || clean.contains("saving")

        return (hasPronoun || referencedEntities.isNotEmpty() || hasDiagnosticDetail) && (assistantAsked || recentMessages.isNotEmpty())
    }

    private fun isExplanationRequest(clean: String, noApos: String): Boolean {
        if (clean.contains("what does") && clean.contains("mean")) return true
        if (clean.contains("what do") && clean.contains("mean")) return true
        if (clean.contains("what is the meaning of") || noApos.contains("what is the meaning of")) return true
        if (clean.contains("meaning of") || clean.contains("define") || clean.contains("definition of")) return true
        if (clean.startsWith("explain ") || clean.contains(" explain ")) return true
        if (clean.startsWith("tell me about") || clean.contains("tell me more about")) return true
        if (clean.contains("difference between") || clean.contains("compare") || clean.contains("versus") || clean.contains("vs")) return true
        if (clean.contains("what is") && (clean.contains("hi") || clean.contains("hello") || clean.contains("greeting"))) return true
        return false
    }

    private fun isReplyAdviceRequest(clean: String, noApos: String): Boolean {
        if (clean.contains("how should i reply") || clean.contains("how do i reply") || clean.contains("how can i reply")) return true
        if (clean.contains("how to reply") || clean.contains("how should i respond") || clean.contains("how do i respond")) return true
        if (clean.contains("what should i say") || clean.contains("what can i say")) return true
        if (clean.contains("what do i say") || clean.contains("what to say")) return true
        if (clean.contains("someone said") || clean.contains("someone just said") || clean.contains("someone greeted me")) return true
        if (clean.contains("reply to a greeting") || clean.contains("respond to a greeting")) return true
        return false
    }

    private fun isGreeting(clean: String, noApos: String): Boolean {
        if (isExplanationRequest(clean, noApos) || isReplyAdviceRequest(clean, noApos)) return false
        val greetings = setOf(
            "hi", "hello", "hey", "yo", "hiya", "howdy", "greetings", "good morning",
            "good afternoon", "good evening", "hi there", "hello there", "hey there"
        )
        if (greetings.contains(clean) || greetings.contains(noApos)) return true
        if (clean.startsWith("hi ") || clean.startsWith("hello ") || clean.startsWith("hey ")) {
            val rest = clean.substringAfter(" ").trim()
            if (rest.startsWith("how are you") || rest.startsWith("whats up") || rest.startsWith("what's up") ||
                rest.startsWith("there") || rest.isBlank()) {
                return true
            }
        }
        return false
    }

    private fun isCasualConversation(clean: String, noApos: String): Boolean {
        // Questions asking for definitions or instructions are NOT casual chit-chat
        if (clean.contains("what does") || clean.contains("what do") || clean.contains("what is") ||
            clean.contains("how should i") || clean.contains("how can i") || clean.contains("how do i")) {
            return false
        }
        val phrases = listOf(
            "how are you", "how are you doing", "how's it going", "hows it going",
            "what's up", "whats up", "what are you up to", "what's your name",
            "whats your name", "who are you", "i'm fine", "im fine", "i am fine",
            "i'm good", "im good", "doing well", "nothing much", "not much",
            "nice to meet you", "pleased to meet you", "just chilling", "all good",
            "barely keep my eyes open", "so tired", "i'm so tired", "im so tired",
            "i am so tired", "i feel exhausted", "so sleepy", "falling asleep", "need sleep",
            "i feel full", "i ate so much", "so full"
        )
        return phrases.any { clean == it || noApos == it || clean.contains(it) }
    }

    private fun isFarewell(clean: String, noApos: String): Boolean {
        val list = listOf("bye", "goodbye", "see you", "see ya", "see you later", "take care", "have a nice day")
        return list.any { clean == it || noApos == it || clean.startsWith("$it ") }
    }

    private fun isThanks(clean: String, noApos: String): Boolean {
        val list = listOf("thanks", "thank you", "thanks a lot", "thank you so much", "thx", "appreciate it")
        return list.any { clean == it || noApos == it || clean.startsWith("$it ") }
    }

    private fun isApology(clean: String, noApos: String): Boolean {
        val list = listOf("sorry", "i'm sorry", "im sorry", "my bad", "apologies")
        return list.any { clean == it || noApos == it }
    }

    private fun isQuestionStarter(clean: String): Boolean {
        val starters = setOf("what", "how", "why", "when", "where", "who", "which", "can", "is", "are", "do", "does", "should")
        val first = clean.split(" ").firstOrNull() ?: ""
        return starters.contains(first)
    }

    private fun detectSentiment(clean: String): String {
        if (clean.contains("happy") || clean.contains("great") || clean.contains("awesome") || clean.contains("good") || clean.contains("love")) return "positive"
        if (clean.contains("sad") || clean.contains("bad") || clean.contains("terrible") || clean.contains("frustrated") || clean.contains("annoyed")) return "negative"
        if (clean.contains("problem") || clean.contains("issue") || clean.contains("wrong") || clean.contains("trouble") || clean.contains("stuck")) return "concerned"
        if (clean.contains("confused") || clean.contains("don't understand")) return "confused"
        return "neutral"
    }

    // --- Entity and Topic Extraction ---

    private fun extractTopic(clean: String, contextSubject: String?, entities: List<String>): String? {
        if (clean.contains("roblox")) return "Roblox game"
        if (clean.contains("game")) return "Game"
        if (clean.contains("greeting") || clean.contains("greetings")) return "Greetings"
        if (clean.contains("hello") && (clean.contains("mean") || clean.contains("reply"))) return "Hello"
        if (clean.contains("hi") && (clean.contains("mean") || clean.contains("reply"))) return "Hi"
        if (entities.isNotEmpty()) return entities.first()
        return contextSubject
    }

    private fun extractEntities(clean: String, contextSubject: String?): List<String> {
        val list = mutableListOf<String>()
        if (clean.contains("roblox")) list.add("Roblox")
        if (clean.contains("game")) list.add("Game")
        if (clean.contains("hi")) list.add("Hi")
        if (clean.contains("hello")) list.add("Hello")
        if (clean.contains("teacher")) list.add("Teacher")
        if (clean.contains("elder") || clean.contains("older person")) list.add("Elder")
        if (contextSubject != null && !list.contains(contextSubject)) list.add(contextSubject)
        return list
    }

    private fun resolveReferencedEntities(
        clean: String,
        noApos: String,
        recentMessages: List<ConversationMessageEntity>
    ): Pair<List<String>, String?> {
        val hasPronoun = clean.matches(Regex(".*\\b(it|this|that|they|the problem|the game|the book|that thing)\\b.*"))
        if (!hasPronoun || recentMessages.isEmpty()) {
            return Pair(emptyList(), null)
        }

        // Search recent turns backwards for active subject
        for (msg in recentMessages) {
            val text = msg.message.lowercase(Locale.ROOT)
            if (text.contains("roblox")) {
                return Pair(listOf("it -> Roblox game"), "Roblox game")
            }
            if (text.contains("game")) {
                return Pair(listOf("it -> game"), "game")
            }
            if (text.contains("hi") || text.contains("hello")) {
                return Pair(listOf("it -> greeting"), "greeting")
            }
        }
        return Pair(listOf("it -> previous topic"), null)
    }
}
