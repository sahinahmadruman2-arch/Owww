package com.example.data.repository

import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.entity.*
import com.example.domain.engine.AnswerGenerator
import com.example.domain.engine.KnowledgePipelineProcessor
import com.example.domain.engine.SemanticRetrievalEngine
import com.example.domain.model.GeneratedAnswerResult
import com.example.domain.model.PipelineStageProgress
import com.example.domain.model.VerificationTestItem
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class UrBotsRepository(
    private val dao: KnowledgeDao
) {
    private val retrievalEngine = SemanticRetrievalEngine(dao)
    private val pipelineProcessor = KnowledgePipelineProcessor(dao)
    private val answerGenerator = AnswerGenerator()

    val allDocuments: Flow<List<KnowledgeDocumentEntity>> = dao.getAllDocuments()
    val allConcepts: Flow<List<KnowledgeConceptEntity>> = dao.getAllConcepts()
    val allFacts: Flow<List<KnowledgeFactEntity>> = dao.getAllFacts()
    val allRelationships: Flow<List<KnowledgeRelationshipEntity>> = dao.getAllRelationships()
    val allTrainingRuns: Flow<List<TrainingRunEntity>> = dao.getAllTrainingRuns()
    val allVersions: Flow<List<KnowledgeVersionEntity>> = dao.getAllVersions()

    val documentCount: Flow<Int> = dao.getDocumentCount()
    val chunkCount: Flow<Int> = dao.getChunkCount()
    val conceptCount: Flow<Int> = dao.getConceptCount()
    val factCount: Flow<Int> = dao.getFactCount()
    val questionCount: Flow<Int> = dao.getQuestionCount()
    val relationshipCount: Flow<Int> = dao.getRelationshipCount()

    fun getConversationMessages(sessionId: String = "default_session"): Flow<List<ConversationMessageEntity>> {
        return dao.getMessagesForSession(sessionId)
    }

    suspend fun processDocument(
        title: String,
        content: String,
        sourceType: String = "BOOK",
        onProgress: (PipelineStageProgress) -> Unit = {}
    ): KnowledgeDocumentEntity {
        return pipelineProcessor.processDocument(title, content, sourceType, onProgress)
    }

    suspend fun manualTeach(
        question: String,
        answer: String
    ): LearnedAnswerEntity {
        return pipelineProcessor.processManualTeach(question, answer)
    }

    suspend fun askQuestion(
        query: String,
        sessionId: String = "default_session"
    ): GeneratedAnswerResult {
        // Fetch recent messages for context before inserting new turn
        val recentMessages = dao.getRecentMessages(sessionId, limit = 6)

        // Record user message
        val userMsg = ConversationMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            message = query
        )
        dao.insertMessage(userMsg)

        // 1. Semantic Retrieval with Intent Detection & Anaphora Resolution
        val retrieval = retrievalEngine.retrieve(query, sessionId, recentMessages)

        // 2. Answer Generation & Evidence Grounding
        val result = answerGenerator.generateAnswer(query, retrieval, recentMessages)

        // 3. Convert citations to JSON (citations only apply to knowledge retrieval)
        val citationsJson = if (retrieval.userIntent.isConversational) {
            "[]"
        } else {
            JSONArray().apply {
                result.sources.forEach { src ->
                    put(JSONObject().apply {
                        put("documentTitle", src.documentTitle)
                        put("sectionTitle", src.sectionTitle)
                        put("statement", src.statement ?: "")
                        put("conceptName", src.conceptName ?: "")
                    })
                }
            }.toString()
        }

        // 4. Record assistant message
        val assistantMsg = ConversationMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "assistant",
            message = result.answerText,
            usedSourcesJson = citationsJson,
            resolvedContext = result.resolvedContext
        )
        dao.insertMessage(assistantMsg)

        return result
    }

    suspend fun deleteDocument(id: String) {
        dao.deleteDocumentById(id)
        dao.deleteChunksByDocumentId(id)
        dao.deleteConceptsByDocumentId(id)
        dao.deleteFactsByDocumentId(id)
    }

    suspend fun clearConversation(sessionId: String = "default_session") {
        dao.clearSession(sessionId)
    }

    suspend fun preloadSampleBook(onProgress: (PipelineStageProgress) -> Unit = {}): KnowledgeDocumentEntity {
        val sampleTitle = "Basic Conversation"
        val sampleContent = """
            Hi and hello are common greetings.
            
            Hi is generally casual and friendly.
            
            Hello can be used in both casual and more polite situations.
            
            When someone says 'Hi', a person can reply with 'Hi', 'Hello', or another friendly greeting.
            
            When meeting someone for the first time, people may also say 'Nice to meet you.'
        """.trimIndent()

        return processDocument(sampleTitle, sampleContent, "BOOK", onProgress)
    }

    suspend fun runVerificationSuite(): List<VerificationTestItem> {
        val testQueries = listOf(
            VerificationTestItem(1, "Hi", "Natural greeting response (conversational)"),
            VerificationTestItem(2, "Hello", "Natural greeting response (conversational)"),
            VerificationTestItem(3, "How are you?", "Natural conversational answer"),
            VerificationTestItem(4, "I'm fine.", "Natural response (conversational)"),
            VerificationTestItem(5, "What does hi mean?", "Explanation of 'hi' (knowledge)"),
            VerificationTestItem(6, "How should I reply to hi?", "Reply suggestions for 'hi' (knowledge)"),
            VerificationTestItem(7, "What is the difference between hi and hello?", "Explanation of casual vs polite (knowledge)"),
            VerificationTestItem(8, "Hi, how are you?", "Natural conversational response"),
            VerificationTestItem(9, "Someone just said hello to me. What should I say?", "Natural reply suggestions"),
            VerificationTestItem(10, "Tell me about greetings.", "Educational explanation of greetings")
        )

        val results = mutableListOf<VerificationTestItem>()
        val verificationSession = "verification_${System.currentTimeMillis()}"

        // Ensure sample knowledge exists
        preloadSampleBook()

        // Test 1: Hi
        for (item in testQueries) {
            val answerResult = askQuestion(item.query, verificationSession)
            val answerLower = answerResult.answerText.lowercase()

            val passed = when (item.id) {
                1 -> {
                    // TEST 1: User: Hi -> Expected: Natural greeting response (NOT definition)
                    (answerLower.contains("how are you") || answerLower.contains("hi") || answerLower.contains("hello")) &&
                            !answerLower.contains("is a common informal greeting") &&
                            !answerLower.contains("definition")
                }
                2 -> {
                    // TEST 2: User: Hello -> Expected: Natural greeting response
                    (answerLower.contains("hello") || answerLower.contains("hi")) &&
                            (answerLower.contains("how are you") || answerLower.contains("nice to meet you") || answerLower.contains("how can i help")) &&
                            !answerLower.contains("is a common greeting used in casual")
                }
                3 -> {
                    // TEST 3: User: How are you? -> Expected: Natural conversational answer
                    (answerLower.contains("good") || answerLower.contains("doing well") || answerLower.contains("great")) &&
                            !answerLower.contains("is a question used to ask")
                }
                4 -> {
                    // TEST 4: User: I'm fine. -> Expected: Natural response
                    (answerLower.contains("great to hear") || answerLower.contains("glad") || answerLower.contains("what are you up to")) &&
                            !answerLower.contains("means that the speaker is")
                }
                5 -> {
                    // TEST 5: User: What does hi mean? -> Expected: Explanation
                    answerLower.contains("greeting") && (answerLower.contains("casual") || answerLower.contains("friendly"))
                }
                6 -> {
                    // TEST 6: User: How should I reply to hi? -> Expected: Reply suggestions
                    (answerLower.contains("reply") || answerLower.contains("say")) &&
                            (answerLower.contains("hi") || answerLower.contains("hello"))
                }
                7 -> {
                    // TEST 7: User: What is the difference between hi and hello? -> Expected: Explanation based on knowledge
                    answerLower.contains("casual") && answerLower.contains("polite")
                }
                8 -> {
                    // TEST 8: User: Hi, how are you? -> Expected: Natural conversational response
                    (answerLower.contains("doing great") || answerLower.contains("good") || answerLower.contains("great")) &&
                            (answerLower.contains("how are you") || answerLower.contains("how about you") || answerLower.contains("today"))
                }
                9 -> {
                    // TEST 9: User: Someone just said hello to me. What should I say? -> Expected: Natural reply suggestions
                    (answerLower.contains("reply") || answerLower.contains("say")) &&
                            (answerLower.contains("hello") || answerLower.contains("hi"))
                }
                10 -> {
                    // TEST 10: User: Tell me about greetings. -> Expected: Educational explanation
                    answerLower.contains("greetings") &&
                            (answerLower.contains("hi") || answerLower.contains("hello")) &&
                            answerLower.contains("casual")
                }
                else -> true
            }

            results.add(
                item.copy(
                    actualAnswer = answerResult.answerText,
                    isPassed = passed,
                    sources = answerResult.sources.map { "${it.documentTitle} (${it.sectionTitle})" }
                )
            )
        }

        return results
    }
}
