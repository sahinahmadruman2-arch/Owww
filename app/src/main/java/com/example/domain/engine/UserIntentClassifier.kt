package com.example.domain.engine

import com.example.data.local.entity.ConversationMessageEntity
import com.example.domain.model.UserIntent
import java.util.Locale

object UserIntentClassifier {

    private val singleWordGreetings = setOf(
        "hi", "hello", "hey", "yo", "hiya", "howdy", "greetings", "sup", "salutations"
    )

    private val multiWordGreetings = listOf(
        "good morning", "good afternoon", "good evening", "good day",
        "hi there", "hello there", "hey there", "hey guys", "hello everyone"
    )

    private val conversationalQuestions = listOf(
        "how are you", "how are you doing", "how's it going", "hows it going",
        "how have you been", "how do you do", "what's up", "whats up",
        "what are you up to", "what're you up to", "what's your name", "whats your name",
        "who are you", "how are things", "how is your day", "how's your day",
        "hows your day", "how is it going", "what you doing", "what are you doing"
    )

    private val farewellPhrases = listOf(
        "bye", "goodbye", "see you", "see ya", "see you later", "see you soon",
        "have a good day", "have a nice day", "have a great day", "take care",
        "bye bye", "good night", "goodnight", "catch you later"
    )

    private val thanksPhrases = listOf(
        "thank you", "thanks", "thanks a lot", "thank you so much", "thx",
        "many thanks", "appreciate it", "much appreciated"
    )

    private val apologyPhrases = listOf(
        "sorry", "i'm sorry", "im sorry", "my bad", "apologies", "pardon me", "excuse me"
    )

    private val casualStatements = listOf(
        "i'm fine", "im fine", "i am fine", "i'm good", "im good", "i am good",
        "doing well", "doing fine", "pretty good", "all good", "not bad", "great",
        "feeling good", "i'm doing well", "im doing well", "i am doing well",
        "nothing", "nothing much", "not much", "just chilling", "just relaxing",
        "just hanging out", "chilling", "same old", "not a lot",
        "nice to meet you", "nice meeting you", "pleased to meet you", "glad to meet you",
        "good to meet you", "fine", "good",
        "ok", "okay", "cool", "sounds good", "awesome", "nice", "got it", "i see",
        "makes sense", "alright", "sure"
    )

    fun classify(
        query: String,
        recentMessages: List<ConversationMessageEntity> = emptyList()
    ): UserIntent {
        val trimmed = query.trim()
        val qLower = trimmed.lowercase(Locale.ROOT)
        // Clean text: strip punctuation but keep apostrophes and spaces
        val clean = qLower.replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val noApos = clean.replace("'", "")

        // 1. Check for explicit EXPLANATION or DEFINITION requests first
        // If the user asks "What does hi mean?" or "Tell me about greetings", it's LEARNING_EXPLANATION, NOT a greeting!
        if (isExplanationRequest(qLower, clean, noApos)) {
            return UserIntent.LEARNING_EXPLANATION
        }

        // 2. Check for "HOW TO REPLY / WHAT TO SAY" advice
        // e.g. "How should I reply to hi?", "Someone just said hello to me. What should I say?"
        if (isReplyAdviceRequest(qLower, clean, noApos)) {
            return UserIntent.REQUEST_FOR_INFORMATION
        }

        // 3. Check for GREETING (Rule 2)
        // e.g. "Hi", "Hello!", "Hey", "Good morning", "Hi, how are you?", "Hello! How are you?"
        if (isGreeting(clean, noApos)) {
            return UserIntent.GREETING
        }

        // 4. Check for pure CONVERSATIONAL QUESTIONS
        // e.g. "How are you?", "What's up?", "What's your name?", "Who are you?"
        if (isConversationalQuestion(clean, noApos)) {
            return UserIntent.NORMAL_CONVERSATION
        }

        // 5. Check for THANKS
        if (isThanks(clean, noApos)) {
            return UserIntent.THANKS
        }

        // 6. Check for FAREWELL
        if (isFarewell(clean, noApos)) {
            return UserIntent.FAREWELL
        }

        // 7. Check for APOLOGY
        if (isApology(clean, noApos)) {
            return UserIntent.APOLOGY
        }

        // 8. Check for CASUAL STATEMENT (Rule 4, 9)
        // e.g. "I'm fine.", "Nothing much.", "Nice to meet you."
        if (isCasualStatement(clean, noApos)) {
            return UserIntent.CASUAL_STATEMENT
        }

        // 9. Check for general INFORMATION QUESTION
        // (starts with what/how/why/when/where/who/can i/is it/does/do/should or ends with '?')
        if (isInformationQuestion(trimmed, qLower, clean)) {
            return UserIntent.QUESTION
        }

        // 10. Check for INSTRUCTION
        if (isInstruction(clean)) {
            return UserIntent.INSTRUCTION
        }

        // 11. AMBIGUOUS: prefer normal conversation if short or conversational speech
        val words = clean.split(" ").filter { it.isNotBlank() }
        return if (words.size <= 4) {
            UserIntent.NORMAL_CONVERSATION
        } else {
            UserIntent.AMBIGUOUS
        }
    }

    private fun isExplanationRequest(qLower: String, clean: String, noApos: String): Boolean {
        if (clean.contains("what does") && clean.contains("mean")) return true
        if (clean.contains("what do") && clean.contains("mean")) return true
        if (clean.contains("what is the meaning of") || noApos.contains("what is the meaning of")) return true
        if (clean.contains("meaning of") || clean.contains("define") || clean.contains("definition of")) return true
        if (clean.startsWith("explain ") || clean.contains(" explain ")) return true
        if (clean.startsWith("tell me about") || clean.contains("tell me more about")) return true
        if (clean.contains("difference between") || clean.contains("compare") || clean.contains("versus") || clean.contains("vs")) return true
        if (clean.contains("what is") && (clean.contains("hi") || clean.contains("hello") || clean.contains("greeting"))) return true
        if (clean.contains("what does it refer to") || qLower.contains("\"it\" refer to")) return true
        return false
    }

    private fun isReplyAdviceRequest(qLower: String, clean: String, noApos: String): Boolean {
        if (clean.contains("how should i reply") || clean.contains("how do i reply") || clean.contains("how can i reply")) return true
        if (clean.contains("how to reply") || clean.contains("how should i respond") || clean.contains("how do i respond")) return true
        if (clean.contains("what should i say") || clean.contains("what can i say")) return true
        if (clean.contains("what do i say") || clean.contains("what to say")) return true
        if (clean.contains("someone said") || clean.contains("someone just said") || clean.contains("someone greeted me")) return true
        return false
    }

    private fun isGreeting(clean: String, noApos: String): Boolean {
        // Direct single-word match
        if (singleWordGreetings.contains(clean) || singleWordGreetings.contains(noApos)) return true

        // Direct multi-word match
        if (multiWordGreetings.any { clean == it || clean.startsWith("$it ") || noApos == it || noApos.startsWith("$it ") }) {
            return true
        }

        // Combined greetings e.g. "Hi, how are you?", "Hello! How are you?"
        val words = clean.split(" ")
        if (words.isNotEmpty() && singleWordGreetings.contains(words[0])) {
            val rest = words.drop(1).joinToString(" ")
            if (rest.isBlank() ||
                rest.startsWith("how are you") ||
                rest.startsWith("hows it going") ||
                rest.startsWith("how's it going") ||
                rest.startsWith("whats up") ||
                rest.startsWith("what's up") ||
                rest.startsWith("there") ||
                rest.startsWith("everyone") ||
                rest.startsWith("friend") ||
                rest.startsWith("nice to see you")
            ) {
                return true
            }
        }

        return false
    }

    private fun isConversationalQuestion(clean: String, noApos: String): Boolean {
        return conversationalQuestions.any { clean == it || noApos == it || clean.startsWith("$it ") || noApos.startsWith("$it ") }
    }

    private fun isThanks(clean: String, noApos: String): Boolean {
        return thanksPhrases.any { clean == it || noApos == it || clean.startsWith("$it ") }
    }

    private fun isFarewell(clean: String, noApos: String): Boolean {
        return farewellPhrases.any { clean == it || noApos == it || clean.startsWith("$it ") }
    }

    private fun isApology(clean: String, noApos: String): Boolean {
        return apologyPhrases.any { clean == it || noApos == it || clean.startsWith("$it ") }
    }

    private fun isCasualStatement(clean: String, noApos: String): Boolean {
        return casualStatements.any { clean == it || noApos == it }
    }

    private fun isInformationQuestion(trimmed: String, qLower: String, clean: String): Boolean {
        if (trimmed.endsWith("?")) return true
        val words = clean.split(" ")
        if (words.isEmpty()) return false
        val first = words[0]
        val questionStarters = setOf(
            "what", "how", "why", "when", "where", "who", "which",
            "can", "could", "is", "are", "do", "does", "should", "would", "will"
        )
        return questionStarters.contains(first)
    }

    private fun isInstruction(clean: String): Boolean {
        val instructionStarters = listOf("please ", "list ", "show me ", "tell me ", "give me ", "find ")
        return instructionStarters.any { clean.startsWith(it) }
    }
}
