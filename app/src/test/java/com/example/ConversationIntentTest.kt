package com.example

import com.example.domain.engine.AnswerGenerator
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
