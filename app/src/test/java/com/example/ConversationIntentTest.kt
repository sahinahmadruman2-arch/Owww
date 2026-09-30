package com.example

import com.example.domain.engine.AnswerGenerator
import com.example.domain.engine.SemanticUnderstandingEngine
import com.example.domain.engine.UserIntentClassifier
import com.example.domain.model.RetrievalResult
import com.example.domain.model.UserIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ConversationIntentTest {

    private val answerGenerator = AnswerGenerator()

    @Test
    fun testIntentClassification() {
        // TEST 1
        assertEquals(UserIntent.GREETING, UserIntentClassifier.classify("Hi"))

        // TEST 2
        assertEquals(UserIntent.GREETING, UserIntentClassifier.classify("Hello"))

        // TEST 3
        assertEquals(UserIntent.NORMAL_CONVERSATION, UserIntentClassifier.classify("How are you?"))

        // TEST 4
        assertEquals(UserIntent.CASUAL_STATEMENT, UserIntentClassifier.classify("I'm fine."))

        // TEST 5
        assertEquals(UserIntent.LEARNING_EXPLANATION, UserIntentClassifier.classify("What does hi mean?"))

        // TEST 6
        assertEquals(UserIntent.REQUEST_FOR_INFORMATION, UserIntentClassifier.classify("How should I reply to hi?"))

        // TEST 7
        assertEquals(UserIntent.LEARNING_EXPLANATION, UserIntentClassifier.classify("What is the difference between hi and hello?"))

        // TEST 8
        assertEquals(UserIntent.GREETING, UserIntentClassifier.classify("Hi, how are you?"))

        // TEST 9
        assertEquals(UserIntent.REQUEST_FOR_INFORMATION, UserIntentClassifier.classify("Someone just said hello to me. What should I say?"))

        // TEST 10
        assertEquals(UserIntent.LEARNING_EXPLANATION, UserIntentClassifier.classify("Tell me about greetings."))
    }

    @Test
    fun testSemanticProblemAndHelpUnderstanding() = runBlocking {
        // 1. "I have a problem."
        val a1 = SemanticUnderstandingEngine.analyze("I have a problem.")
        assertEquals(UserIntent.REPORTING_PROBLEM, a1.intent)
        val r1 = answerGenerator.generateAnswer("I have a problem.", RetrievalResult(query = "I have a problem.", userIntent = a1.intent, semanticAnalysis = a1))
        assertTrue(r1.answerText.contains("I'm sorry to hear that. What's the problem?"))

        // 2. "I'm having an issue." & "Something is wrong."
        val a2 = SemanticUnderstandingEngine.analyze("I'm having an issue.")
        assertEquals(UserIntent.REPORTING_PROBLEM, a2.intent)
        val a3 = SemanticUnderstandingEngine.analyze("Something is wrong.")
        assertEquals(UserIntent.REPORTING_PROBLEM, a3.intent)

        // 3. "I need help."
        val a4 = SemanticUnderstandingEngine.analyze("I need help.")
        assertEquals(UserIntent.REQUESTING_HELP, a4.intent)
        val r4 = answerGenerator.generateAnswer("I need help.", RetrievalResult(query = "I need help.", userIntent = a4.intent, semanticAnalysis = a4))
        assertTrue(r4.answerText.contains("Of course! What do you need help with?"))

        // 4. "I'm confused."
        val a5 = SemanticUnderstandingEngine.analyze("I'm confused.")
        assertEquals(UserIntent.CONFUSION, a5.intent)
        val r5 = answerGenerator.generateAnswer("I'm confused.", RetrievalResult(query = "I'm confused.", userIntent = a5.intent, semanticAnalysis = a5))
        assertTrue(r5.answerText.contains("No worries. Tell me what's confusing you."))

        // 5. "My Roblox game isn't working."
        val a6 = SemanticUnderstandingEngine.analyze("My Roblox game isn't working.")
        assertEquals(UserIntent.REPORTING_PROBLEM, a6.intent)
        assertEquals("Roblox game", a6.topic)
        val r6 = answerGenerator.generateAnswer("My Roblox game isn't working.", RetrievalResult(query = "My Roblox game isn't working.", userIntent = a6.intent, semanticAnalysis = a6))
        assertTrue(r6.answerText.contains("Roblox game") && r6.answerText.contains("help"))

        // 6. "What does hello mean?"
        val a7 = SemanticUnderstandingEngine.analyze("What does hello mean?")
        assertEquals(UserIntent.REQUESTING_EXPLANATION, a7.intent)
        val r7 = answerGenerator.generateAnswer("What does hello mean?", RetrievalResult(query = "What does hello mean?", userIntent = a7.intent, semanticAnalysis = a7))
        assertTrue(r7.answerText.contains("common greeting") && r7.answerText.contains("polite"))

        // 7. "How should I reply to hello?"
        val a8 = SemanticUnderstandingEngine.analyze("How should I reply to hello?")
        assertEquals(UserIntent.REQUESTING_INSTRUCTIONS, a8.intent)
        val r8 = answerGenerator.generateAnswer("How should I reply to hello?", RetrievalResult(query = "How should I reply to hello?", userIntent = a8.intent, semanticAnalysis = a8))
        assertTrue(r8.answerText.contains("reply with") && r8.answerText.contains("Hello"))
    }

    @Test
    fun testAllTenConversationalAndKnowledgeResponses() = runBlocking {
        // TEST 1: User: Hi -> Expected: Natural greeting response
        val r1 = answerGenerator.generateAnswer("Hi", RetrievalResult(query = "Hi", userIntent = UserIntent.GREETING))
        assertEquals("Hi! How are you?", r1.answerText)
        assertFalse(r1.answerText.contains("common informal greeting used to greet"))

        // TEST 2: User: Hello -> Expected: Natural greeting response
        val r2 = answerGenerator.generateAnswer("Hello", RetrievalResult(query = "Hello", userIntent = UserIntent.GREETING))
        assertEquals("Hello! How are you?", r2.answerText)
        assertFalse(r2.answerText.contains("is a common greeting used in casual"))

        // TEST 3: User: How are you? -> Expected: Natural conversational answer
        val r3 = answerGenerator.generateAnswer("How are you?", RetrievalResult(query = "How are you?", userIntent = UserIntent.NORMAL_CONVERSATION))
        assertEquals("I'm good, thanks! How are you?", r3.answerText)
        assertFalse(r3.answerText.contains("is a question used to ask"))

        // TEST 4: User: I'm fine. -> Expected: Natural response
        val r4 = answerGenerator.generateAnswer("I'm fine.", RetrievalResult(query = "I'm fine.", userIntent = UserIntent.CASUAL_STATEMENT))
        assertTrue(r4.answerText.contains("great to hear") || r4.answerText.contains("glad"))
        assertFalse(r4.answerText.contains("means that the speaker is"))

        // TEST 5: User: What does hi mean? -> Expected: Explanation
        val r5 = answerGenerator.generateAnswer("What does hi mean?", RetrievalResult(query = "What does hi mean?", userIntent = UserIntent.LEARNING_EXPLANATION))
        assertTrue(r5.answerText.contains("greeting") && (r5.answerText.contains("casual") || r5.answerText.contains("friendly")))

        // TEST 6: User: How should I reply to hi? -> Expected: Reply suggestions
        val r6 = answerGenerator.generateAnswer("How should I reply to hi?", RetrievalResult(query = "How should I reply to hi?", userIntent = UserIntent.REQUEST_FOR_INFORMATION))
        assertTrue(r6.answerText.contains("reply with") && (r6.answerText.contains("Hi") || r6.answerText.contains("Hello")))

        // TEST 7: User: What is the difference between hi and hello? -> Expected: Explanation based on knowledge
        val r7 = answerGenerator.generateAnswer("What is the difference between hi and hello?", RetrievalResult(query = "What is the difference between hi and hello?", userIntent = UserIntent.LEARNING_EXPLANATION))
        assertTrue(r7.answerText.contains("casual") && r7.answerText.contains("polite"))

        // TEST 8: User: Hi, how are you? -> Expected: Natural conversational response
        val r8 = answerGenerator.generateAnswer("Hi, how are you?", RetrievalResult(query = "Hi, how are you?", userIntent = UserIntent.GREETING))
        assertTrue(r8.answerText.contains("doing great") || r8.answerText.contains("good"))
        assertTrue(r8.answerText.contains("how are you") || r8.answerText.contains("today"))

        // TEST 9: User: Someone just said hello to me. What should I say? -> Expected: Natural reply suggestions
        val r9 = answerGenerator.generateAnswer("Someone just said hello to me. What should I say?", RetrievalResult(query = "Someone just said hello to me. What should I say?", userIntent = UserIntent.REQUEST_FOR_INFORMATION))
        assertTrue(r9.answerText.contains("reply with") && r9.answerText.contains("Hello"))

        // TEST 10: User: Tell me about greetings. -> Expected: Educational explanation
        val r10 = answerGenerator.generateAnswer("Tell me about greetings.", RetrievalResult(query = "Tell me about greetings.", userIntent = UserIntent.LEARNING_EXPLANATION))
        assertTrue(r10.answerText.contains("Greetings") && r10.answerText.contains("casual") && r10.answerText.contains("polite"))
    }
}
