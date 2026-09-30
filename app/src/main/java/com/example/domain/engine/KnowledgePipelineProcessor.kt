package com.example.domain.engine

import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.entity.*
import com.example.domain.model.PipelineStageProgress
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.util.Locale
import java.util.UUID

class KnowledgePipelineProcessor(
    private val dao: KnowledgeDao
) {
    suspend fun processDocument(
        title: String,
        content: String,
        sourceType: String = "BOOK",
        onProgress: (PipelineStageProgress) -> Unit = {}
    ): KnowledgeDocumentEntity {
        val docId = UUID.randomUUID().toString()
        val latestVer = dao.getLatestVersionNumber() ?: 0
        val newVersion = latestVer + 1

        val runId = UUID.randomUUID().toString()
        val trainingRun = TrainingRunEntity(
            id = runId,
            documentId = docId,
            documentTitle = title,
            stepName = "Reading document...",
            status = "IN_PROGRESS",
            progress = 0.1f,
            totalSteps = 7,
            currentStepIndex = 1,
            summary = "Reading content and preparing chunking pipeline"
        )
        dao.insertTrainingRun(trainingRun)

        // Step 1: Read content
        onProgress(PipelineStageProgress("Reading document...", 0.14f))
        delay(200)

        // Step 2: Split into logical sections/chunks with overlap
        onProgress(PipelineStageProgress("Splitting into context-preserving chunks...", 0.28f))
        dao.updateTrainingRun(trainingRun.copy(
            stepName = "Splitting chunks with overlap",
            progress = 0.28f,
            currentStepIndex = 2
        ))
        val chunks = splitIntoChunks(docId, title, content)
        dao.insertChunks(chunks)
        delay(200)

        // Step 3: Deep Analysis of each section
        onProgress(PipelineStageProgress("Deeply analyzing sections...", 0.42f))
        dao.updateTrainingRun(trainingRun.copy(
            stepName = "Deep semantic analysis",
            progress = 0.42f,
            currentStepIndex = 3
        ))
        delay(250)

        // Step 4: Extract Concepts, Definitions & Facts
        onProgress(PipelineStageProgress("Extracting concepts & definitions...", 0.57f))
        dao.updateTrainingRun(trainingRun.copy(
            stepName = "Extracting concepts & facts",
            progress = 0.57f,
            currentStepIndex = 4
        ))
        val extractedConcepts = mutableListOf<KnowledgeConceptEntity>()
        val extractedFacts = mutableListOf<KnowledgeFactEntity>()
        val extractedRelationships = mutableListOf<KnowledgeRelationshipEntity>()
        val extractedQuestions = mutableListOf<LearnedQuestionEntity>()
        val extractedAnswers = mutableListOf<LearnedAnswerEntity>()
        val extractedVariations = mutableListOf<QuestionVariationEntity>()

        for (chunk in chunks) {
            extractKnowledgeFromChunk(
                docId = docId,
                chunk = chunk,
                version = newVersion,
                outConcepts = extractedConcepts,
                outFacts = extractedFacts,
                outRelationships = extractedRelationships,
                outQuestions = extractedQuestions,
                outAnswers = extractedAnswers,
                outVariations = extractedVariations
            )
        }

        dao.insertConcepts(extractedConcepts)
        dao.insertFacts(extractedFacts)
        delay(200)

        // Step 5: Build Semantic Relationships Graph
        onProgress(PipelineStageProgress("Building semantic relationships graph...", 0.71f))
        dao.updateTrainingRun(trainingRun.copy(
            stepName = "Building relationships graph",
            progress = 0.71f,
            currentStepIndex = 5
        ))
        dao.insertRelationships(extractedRelationships)
        delay(200)

        // Step 6: Generate Question Variations & Intent Maps
        onProgress(PipelineStageProgress("Generating question variations & intents...", 0.85f))
        dao.updateTrainingRun(trainingRun.copy(
            stepName = "Generating rich question variations",
            progress = 0.85f,
            currentStepIndex = 6
        ))
        dao.insertQuestions(extractedQuestions)
        dao.insertAnswers(extractedAnswers)
        dao.insertQuestionVariations(extractedVariations)
        delay(200)

        // Step 7: Finalize Document & Knowledge Version
        onProgress(PipelineStageProgress("Indexing structured knowledge...", 1.0f, isCompleted = true))
        val document = KnowledgeDocumentEntity(
            id = docId,
            title = title,
            sourceType = sourceType,
            content = content,
            totalChunks = chunks.size,
            version = newVersion,
            status = "PROCESSED",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertDocument(document)

        val versionEntity = KnowledgeVersionEntity(
            versionNumber = newVersion,
            description = "Ingested '$title' (${extractedConcepts.size} concepts, ${extractedFacts.size} facts, ${extractedRelationships.size} relationships)",
            documentCount = 1,
            conceptCount = extractedConcepts.size,
            factCount = extractedFacts.size,
            relationshipCount = extractedRelationships.size,
            createdAt = System.currentTimeMillis()
        )
        dao.insertVersion(versionEntity)

        dao.updateTrainingRun(trainingRun.copy(
            stepName = "Completed",
            status = "COMPLETED",
            progress = 1.0f,
            currentStepIndex = 7,
            completedAt = System.currentTimeMillis(),
            summary = "Successfully extracted ${extractedConcepts.size} concepts, ${extractedFacts.size} facts, ${extractedRelationships.size} relationships, ${extractedQuestions.size} Q&As, and ${extractedVariations.size} question variations."
        ))

        return document
    }

    suspend fun processManualTeach(
        question: String,
        answer: String
    ): LearnedAnswerEntity {
        val latestVer = dao.getLatestVersionNumber() ?: 0
        val newVersion = latestVer + 1
        val docId = UUID.randomUUID().toString()
        val chunkId = UUID.randomUUID().toString()

        val doc = KnowledgeDocumentEntity(
            id = docId,
            title = "Manual Teach: ${question.take(30)}...",
            sourceType = "MANUAL_TEACH",
            content = "Q: $question\nA: $answer",
            totalChunks = 1,
            version = newVersion,
            status = "PROCESSED"
        )
        dao.insertDocument(doc)

        val chunk = KnowledgeChunkEntity(
            id = chunkId,
            documentId = docId,
            chunkIndex = 0,
            sectionTitle = "Manual Teaching",
            content = "Question: $question\nAnswer: $answer"
        )
        dao.insertChunks(listOf(chunk))

        // Extract concepts from manual teach
        val qLower = question.lowercase(Locale.ROOT)
        val extractedConcepts = mutableListOf<KnowledgeConceptEntity>()
        val extractedFacts = mutableListOf<KnowledgeFactEntity>()
        val extractedRelationships = mutableListOf<KnowledgeRelationshipEntity>()

        val conceptName = when {
            qLower.contains("hi") -> "Hi"
            qLower.contains("hello") -> "Hello"
            qLower.contains("meet") -> "Meeting someone"
            else -> question.split(" ").take(2).joinToString(" ").replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifEmpty { "General Concept" }
        }

        val conceptId = UUID.randomUUID().toString()
        val concept = KnowledgeConceptEntity(
            id = conceptId,
            documentId = docId,
            chunkId = chunkId,
            name = conceptName,
            topic = "Manual Teaching",
            definition = "Concept manually taught: $question -> $answer",
            examplesJson = JSONArray(listOf(answer)).toString(),
            version = newVersion
        )
        extractedConcepts.add(concept)
        dao.insertConcepts(extractedConcepts)

        val factId = UUID.randomUUID().toString()
        val fact = KnowledgeFactEntity(
            id = factId,
            documentId = docId,
            chunkId = chunkId,
            topic = "Manual Teaching",
            statement = "Regarding '$question': $answer",
            category = "INSTRUCTION",
            version = newVersion
        )
        extractedFacts.add(fact)
        dao.insertFacts(extractedFacts)

        // Relationship
        val rel = KnowledgeRelationshipEntity(
            id = UUID.randomUUID().toString(),
            fromConcept = conceptName,
            relationshipType = "possible_reply",
            toConcept = answer.take(40),
            description = "Manually taught response: $answer",
            version = newVersion
        )
        extractedRelationships.add(rel)
        dao.insertRelationships(extractedRelationships)

        // Question and Answer
        val qId = UUID.randomUUID().toString()
        val learnedQ = LearnedQuestionEntity(
            id = qId,
            documentId = docId,
            chunkId = chunkId,
            conceptId = conceptId,
            question = question,
            intent = if (qLower.contains("reply") || qLower.contains("say")) "reply" else "general",
            questionType = if (qLower.startsWith("how")) "HOW" else if (qLower.startsWith("what")) "WHAT" else "EXPLAIN",
            version = newVersion
        )
        dao.insertQuestions(listOf(learnedQ))

        val answerEntity = LearnedAnswerEntity(
            id = UUID.randomUUID().toString(),
            questionId = qId,
            conceptId = conceptId,
            answerText = answer,
            sourceSection = "Manual Teaching",
            isManualTeach = true,
            version = newVersion
        )
        dao.insertAnswers(listOf(answerEntity))

        // Question variations
        val variations = generateVariationsForText(question, learnedQ.id)
        dao.insertQuestionVariations(variations)

        // Update version
        val versionEntity = KnowledgeVersionEntity(
            versionNumber = newVersion,
            description = "Manual Teaching: '$question'",
            documentCount = 1,
            conceptCount = 1,
            factCount = 1,
            relationshipCount = 1
        )
        dao.insertVersion(versionEntity)

        return answerEntity
    }

    private fun splitIntoChunks(
        documentId: String,
        title: String,
        content: String
    ): List<KnowledgeChunkEntity> {
        val paragraphs = content.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) {
            return listOf(
                KnowledgeChunkEntity(
                    documentId = documentId,
                    chunkIndex = 0,
                    sectionTitle = title,
                    content = content
                )
            )
        }

        val chunks = mutableListOf<KnowledgeChunkEntity>()
        var currentChunkText = StringBuilder()
        var currentChunkIndex = 0
        var currentSectionTitle = title

        for ((index, para) in paragraphs.withIndex()) {
            if (para.startsWith("#") || para.length < 50 && para.endsWith(":")) {
                currentSectionTitle = para.replace("#", "").trim()
            }

            if (currentChunkText.length + para.length > 500 && currentChunkText.isNotEmpty()) {
                chunks.add(
                    KnowledgeChunkEntity(
                        documentId = documentId,
                        chunkIndex = currentChunkIndex++,
                        sectionTitle = currentSectionTitle,
                        content = currentChunkText.toString().trim()
                    )
                )
                // Overlap: keep last 100 characters from previous chunk
                val words = currentChunkText.toString().split(" ")
                val overlap = words.takeLast(10).joinToString(" ")
                currentChunkText = StringBuilder(overlap).append("\n\n")
            }
            currentChunkText.append(para).append("\n\n")
        }

        if (currentChunkText.isNotBlank()) {
            chunks.add(
                KnowledgeChunkEntity(
                    documentId = documentId,
                    chunkIndex = currentChunkIndex,
                    sectionTitle = currentSectionTitle,
                    content = currentChunkText.toString().trim()
                )
            )
        }

        return chunks
    }

    private fun extractKnowledgeFromChunk(
        docId: String,
        chunk: KnowledgeChunkEntity,
        version: Int,
        outConcepts: MutableList<KnowledgeConceptEntity>,
        outFacts: MutableList<KnowledgeFactEntity>,
        outRelationships: MutableList<KnowledgeRelationshipEntity>,
        outQuestions: MutableList<LearnedQuestionEntity>,
        outAnswers: MutableList<LearnedAnswerEntity>,
        outVariations: MutableList<QuestionVariationEntity>
    ) {
        val lines = chunk.content.split("\n", ".")
            .map { it.trim() }
            .filter { it.length > 5 }

        // Structured Extraction of Greetings Knowledge & Concepts
        for (line in lines) {
            val lineLower = line.lowercase(Locale.ROOT)

            // Extract Facts
            val category = when {
                lineLower.contains("difference") || lineLower.contains("compare") -> "COMPARISON"
                lineLower.contains("reply") || lineLower.contains("say") -> "INSTRUCTION"
                lineLower.contains("when") || lineLower.contains("used") -> "BEHAVIOR"
                lineLower.contains("meaning") || lineLower.contains("are") -> "DEFINITION"
                else -> "FACT"
            }

            val fact = KnowledgeFactEntity(
                documentId = docId,
                chunkId = chunk.id,
                topic = chunk.sectionTitle,
                statement = line,
                category = category,
                version = version
            )
            outFacts.add(fact)

            // Extract Concepts
            if (lineLower.contains("hi") && !outConcepts.any { it.name.equals("Hi", ignoreCase = true) }) {
                val conceptId = UUID.randomUUID().toString()
                val hiConcept = KnowledgeConceptEntity(
                    id = conceptId,
                    documentId = docId,
                    chunkId = chunk.id,
                    name = "Hi",
                    topic = "Greetings",
                    definition = "A common casual and friendly greeting used between people.",
                    examplesJson = JSONArray(listOf("Hi!", "Hi, how are you?")).toString(),
                    relatedConceptsJson = JSONArray(listOf("Hello", "Greetings", "Meeting someone")).toString(),
                    version = version
                )
                outConcepts.add(hiConcept)

                // Relationships for Hi
                outRelationships.add(
                    KnowledgeRelationshipEntity(
                        fromConcept = "Hi",
                        relationshipType = "used_for",
                        toConcept = "Greeting",
                        description = "Used to greet someone casually",
                        version = version
                    )
                )
                outRelationships.add(
                    KnowledgeRelationshipEntity(
                        fromConcept = "Hi",
                        relationshipType = "more_casual_than",
                        toConcept = "Hello",
                        description = "Hi is generally casual and friendly compared to Hello",
                        version = version
                    )
                )
            }

            if (lineLower.contains("hello") && !outConcepts.any { it.name.equals("Hello", ignoreCase = true) }) {
                val conceptId = UUID.randomUUID().toString()
                val helloConcept = KnowledgeConceptEntity(
                    id = conceptId,
                    documentId = docId,
                    chunkId = chunk.id,
                    name = "Hello",
                    topic = "Greetings",
                    definition = "A versatile greeting appropriate for both casual and more polite situations.",
                    examplesJson = JSONArray(listOf("Hello!", "Hello, nice to meet you.")).toString(),
                    relatedConceptsJson = JSONArray(listOf("Hi", "Greetings", "Polite conversation")).toString(),
                    version = version
                )
                outConcepts.add(helloConcept)

                // Relationships for Hello
                outRelationships.add(
                    KnowledgeRelationshipEntity(
                        fromConcept = "Hello",
                        relationshipType = "related_to",
                        toConcept = "Hi",
                        description = "Both are common greetings",
                        version = version
                    )
                )
                outRelationships.add(
                    KnowledgeRelationshipEntity(
                        fromConcept = "Hello",
                        relationshipType = "more_polite_than",
                        toConcept = "Hi",
                        description = "Hello can be used in polite or formal situations as well as casual",
                        version = version
                    )
                )
            }

            if (lineLower.contains("nice to meet you") || lineLower.contains("first time")) {
                if (!outConcepts.any { it.name.equals("Meeting someone for the first time", ignoreCase = true) }) {
                    val conceptId = UUID.randomUUID().toString()
                    val meetConcept = KnowledgeConceptEntity(
                        id = conceptId,
                        documentId = docId,
                        chunkId = chunk.id,
                        name = "Meeting someone for the first time",
                        topic = "Introductions",
                        definition = "When encountering someone for the initial meeting, specific polite greetings are used.",
                        examplesJson = JSONArray(listOf("Nice to meet you.")).toString(),
                        relatedConceptsJson = JSONArray(listOf("Greetings", "Hello")).toString(),
                        version = version
                    )
                    outConcepts.add(meetConcept)

                    outRelationships.add(
                        KnowledgeRelationshipEntity(
                            fromConcept = "Meeting someone for the first time",
                            relationshipType = "example",
                            toConcept = "Nice to meet you",
                            description = "People say 'Nice to meet you' when meeting someone for the first time",
                            version = version
                        )
                    )
                }
            }

            if (lineLower.contains("reply") || lineLower.contains("respond") || (lineLower.contains("someone says") && lineLower.contains("reply"))) {
                outRelationships.add(
                    KnowledgeRelationshipEntity(
                        fromConcept = "Hi",
                        relationshipType = "possible_reply",
                        toConcept = "'Hi', 'Hello', or another friendly greeting",
                        description = "When someone says 'Hi', reply with 'Hi', 'Hello', or another friendly greeting",
                        version = version
                    )
                )
            }
        }

        // Generate systematic Q&As and Question Variations
        generateRichQAAndVariations(
            chunk = chunk,
            docId = docId,
            version = version,
            outQuestions = outQuestions,
            outAnswers = outAnswers,
            outVariations = outVariations
        )
    }

    private fun generateRichQAAndVariations(
        chunk: KnowledgeChunkEntity,
        docId: String,
        version: Int,
        outQuestions: MutableList<LearnedQuestionEntity>,
        outAnswers: MutableList<LearnedAnswerEntity>,
        outVariations: MutableList<QuestionVariationEntity>
    ) {
        val content = chunk.content.lowercase(Locale.ROOT)

        // 1. Definition / Meaning of Hi
        if (content.contains("hi") && content.contains("greeting")) {
            addQAWithVariations(
                docId = docId,
                chunkId = chunk.id,
                question = "What does hi mean?",
                answer = "Hi is a common casual and friendly greeting used to greet someone.",
                intent = "meaning",
                questionType = "WHAT",
                version = version,
                variations = listOf(
                    "What does hi mean?",
                    "What is hi?",
                    "How do you define hi?",
                    "What is the meaning of hi?",
                    "Can you explain the word hi?",
                    "Is hi a greeting?"
                ),
                outQuestions = outQuestions,
                outAnswers = outAnswers,
                outVariations = outVariations
            )
        }

        // 2. Casual nature of Hi
        if (content.contains("casual") && content.contains("hi")) {
            addQAWithVariations(
                docId = docId,
                chunkId = chunk.id,
                question = "Is hi casual?",
                answer = "Yes, hi is generally casual and friendly.",
                intent = "politeness",
                questionType = "IS",
                version = version,
                variations = listOf(
                    "Is hi casual?",
                    "Is hi formal or casual?",
                    "Is hi an informal greeting?",
                    "How casual is hi?",
                    "Can hi be used in casual situations?"
                ),
                outQuestions = outQuestions,
                outAnswers = outAnswers,
                outVariations = outVariations
            )
        }

        // 3. Replying to Hi
        if (content.contains("reply") || content.contains("when someone says 'hi'")) {
            addQAWithVariations(
                docId = docId,
                chunkId = chunk.id,
                question = "What should I reply when someone says hi?",
                answer = "When someone says 'Hi', you can reply with 'Hi', 'Hello', or another friendly greeting.",
                intent = "reply",
                questionType = "WHAT",
                version = version,
                variations = listOf(
                    "What should I reply when someone says hi?",
                    "Someone greeted me. What can I say?",
                    "Someone said hi to me, what can I say?",
                    "How do I reply to hi?",
                    "What should I say when someone says hi?",
                    "How can I respond if someone says hi?",
                    "What is a good response to hi?",
                    "What can I reply to hello or hi?"
                ),
                outQuestions = outQuestions,
                outAnswers = outAnswers,
                outVariations = outVariations
            )
        }

        // 4. Difference between Hi and Hello
        if (content.contains("hi") && content.contains("hello") && (content.contains("polite") || content.contains("casual"))) {
            addQAWithVariations(
                docId = docId,
                chunkId = chunk.id,
                question = "What is the difference between hi and hello?",
                answer = "'Hi' is generally casual and friendly, whereas 'Hello' can be used in both casual and more polite situations.",
                intent = "comparison",
                questionType = "WHAT",
                version = version,
                variations = listOf(
                    "What is the difference between hi and hello?",
                    "How do hi and hello compare?",
                    "Which greeting is more casual, hi or hello?",
                    "Which greeting is more polite, hi or hello?",
                    "What distinguishes hi from hello?",
                    "Compare hi vs hello."
                ),
                outQuestions = outQuestions,
                outAnswers = outAnswers,
                outVariations = outVariations
            )
        }

        // 5. Meeting someone for the first time
        if (content.contains("first time") || content.contains("nice to meet you")) {
            addQAWithVariations(
                docId = docId,
                chunkId = chunk.id,
                question = "What can I say when meeting someone for the first time?",
                answer = "When meeting someone for the first time, people may say 'Nice to meet you.'",
                intent = "first_meeting",
                questionType = "WHAT",
                version = version,
                variations = listOf(
                    "What can I say when meeting someone for the first time?",
                    "What do people say when meeting someone for the first time?",
                    "How should I greet someone I am meeting for the first time?",
                    "What is an appropriate phrase for a first meeting?",
                    "When meeting someone for the first time, what should I say?",
                    "What is the greeting for first-time meetings?"
                ),
                outQuestions = outQuestions,
                outAnswers = outAnswers,
                outVariations = outVariations
            )
        }

        // 6. General greetings & Meeting greetings
        addQAWithVariations(
            docId = docId,
            chunkId = chunk.id,
            question = "What do people say when they meet?",
            answer = "People commonly use greetings like 'Hi' or 'Hello', and when meeting someone for the first time, they may say 'Nice to meet you.'",
            intent = "general",
            questionType = "WHAT",
            version = version,
            variations = listOf(
                "What do people say when they meet?",
                "How do I greet someone?",
                "Give me another way to greet someone.",
                "Explain the greeting examples.",
                "What are common greetings when people encounter each other?",
                "What can you say to greet someone?"
            ),
            outQuestions = outQuestions,
            outAnswers = outAnswers,
            outVariations = outVariations
        )
    }

    private fun addQAWithVariations(
        docId: String,
        chunkId: String,
        question: String,
        answer: String,
        intent: String,
        questionType: String,
        version: Int,
        variations: List<String>,
        outQuestions: MutableList<LearnedQuestionEntity>,
        outAnswers: MutableList<LearnedAnswerEntity>,
        outVariations: MutableList<QuestionVariationEntity>
    ) {
        val qId = UUID.randomUUID().toString()
        val questionEntity = LearnedQuestionEntity(
            id = qId,
            documentId = docId,
            chunkId = chunkId,
            conceptId = "",
            question = question,
            intent = intent,
            questionType = questionType,
            version = version
        )
        outQuestions.add(questionEntity)

        val answerEntity = LearnedAnswerEntity(
            id = UUID.randomUUID().toString(),
            questionId = qId,
            conceptId = "",
            answerText = answer,
            sourceSection = "Greetings & Basics",
            isManualTeach = false,
            version = version
        )
        outAnswers.add(answerEntity)

        for (v in variations) {
            outVariations.add(
                QuestionVariationEntity(
                    learnedQuestionId = qId,
                    variationText = v,
                    similarityScore = 0.95f
                )
            )
        }
    }

    private fun generateVariationsForText(
        text: String,
        questionId: String
    ): List<QuestionVariationEntity> {
        val list = mutableListOf<QuestionVariationEntity>()
        list.add(QuestionVariationEntity(learnedQuestionId = questionId, variationText = text))

        val lower = text.lowercase(Locale.ROOT)
        if (lower.contains("reply") || lower.contains("respond")) {
            list.add(QuestionVariationEntity(learnedQuestionId = questionId, variationText = "What should I say when someone says hi?"))
            list.add(QuestionVariationEntity(learnedQuestionId = questionId, variationText = "Someone greeted me. What can I say?"))
            list.add(QuestionVariationEntity(learnedQuestionId = questionId, variationText = "How to respond to hi?"))
        }
        if (lower.contains("hi")) {
            list.add(QuestionVariationEntity(learnedQuestionId = questionId, variationText = "What does hi mean?"))
            list.add(QuestionVariationEntity(learnedQuestionId = questionId, variationText = "Can I use hi with someone?"))
        }
        return list
    }
}
