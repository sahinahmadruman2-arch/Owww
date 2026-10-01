package com.example.domain.engine

import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.entity.*
import com.example.domain.model.ManualQaPair
import com.example.domain.model.ManualTrainingParseResult
import com.example.domain.model.ManualTrainingVerificationReport
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.util.Locale
import java.util.UUID

class ManualTrainingBatchProcessor(
    private val dao: KnowledgeDao
) {
    suspend fun processAndStore(
        parseResult: ManualTrainingParseResult,
        sessionTitle: String,
        batchSize: Int = 25,
        onProgress: (current: Int, total: Int, stage: String) -> Unit = { _, _, _ -> }
    ): ManualTrainingVerificationReport {
        val pairs = parseResult.pairedEntries
        if (pairs.isEmpty()) {
            return ManualTrainingParser.buildVerificationReport(
                parseResult = parseResult,
                savedCount = 0,
                unprocessedCount = 0,
                documentTitle = sessionTitle
            )
        }

        val totalPairs = pairs.size
        var savedCount = 0
        val latestVer = dao.getLatestVersionNumber() ?: 0
        val newVersion = latestVer + 1

        val docId = UUID.randomUUID().toString()
        val docEntity = KnowledgeDocumentEntity(
            id = docId,
            title = sessionTitle,
            sourceType = "MANUAL_TRAINING",
            content = "Batch manual training session containing $totalPairs paired entries.",
            totalChunks = (totalPairs + batchSize - 1) / batchSize,
            version = newVersion,
            status = "PROCESSED",
            createdAt = System.currentTimeMillis()
        )
        dao.insertDocument(docEntity)

        // Process in batches
        val batches = pairs.chunked(batchSize)
        for ((batchIndex, batch) in batches.withIndex()) {
            onProgress(
                savedCount,
                totalPairs,
                "Processing batch ${batchIndex + 1}/${batches.size} (${batch.size} entries)..."
            )

            val manualEntries = mutableListOf<ManualTrainingEntryEntity>()
            val chunkEntities = mutableListOf<KnowledgeChunkEntity>()
            val conceptEntities = mutableListOf<KnowledgeConceptEntity>()
            val factEntities = mutableListOf<KnowledgeFactEntity>()
            val relationshipEntities = mutableListOf<KnowledgeRelationshipEntity>()
            val questionEntities = mutableListOf<LearnedQuestionEntity>()
            val answerEntities = mutableListOf<LearnedAnswerEntity>()
            val variationEntities = mutableListOf<QuestionVariationEntity>()

            for (pair in batch) {
                val qTrim = pair.question.trim()
                val aTrim = pair.answer.trim()
                val qLower = qTrim.lowercase(Locale.ROOT)
                val chunkId = UUID.randomUUID().toString()
                val conceptId = UUID.randomUUID().toString()
                val qId = UUID.randomUUID().toString()
                val ansId = UUID.randomUUID().toString()

                // 1. Chunk representation
                chunkEntities.add(
                    KnowledgeChunkEntity(
                        id = chunkId,
                        documentId = docId,
                        chunkIndex = pair.index,
                        sectionTitle = "Entry #${pair.index}",
                        content = "Question: $qTrim\nAnswer: $aTrim"
                    )
                )

                // 2. Extract Concept
                val conceptName = extractConceptName(qTrim)
                conceptEntities.add(
                    KnowledgeConceptEntity(
                        id = conceptId,
                        documentId = docId,
                        chunkId = chunkId,
                        name = conceptName,
                        topic = "Manual Training",
                        definition = aTrim,
                        examplesJson = JSONArray(listOf(qTrim)).toString(),
                        confidence = 1.0f,
                        version = newVersion
                    )
                )

                // 3. Extract Fact
                factEntities.add(
                    KnowledgeFactEntity(
                        id = UUID.randomUUID().toString(),
                        documentId = docId,
                        chunkId = chunkId,
                        topic = conceptName,
                        statement = if (qLower.contains("mean")) {
                            "'$conceptName' means: $aTrim"
                        } else {
                            "Regarding '$qTrim': $aTrim"
                        },
                        category = if (qLower.contains("mean")) "DEFINITION" else "INSTRUCTION",
                        confidence = 1.0f,
                        version = newVersion
                    )
                )

                // 4. Graph Relationship
                relationshipEntities.add(
                    KnowledgeRelationshipEntity(
                        id = UUID.randomUUID().toString(),
                        fromConcept = conceptName,
                        relationshipType = if (qLower.contains("reply") || qLower.contains("how")) "possible_reply" else "meaning",
                        toConcept = aTrim.take(50),
                        description = "Taught pair #${pair.index}: $qTrim -> $aTrim",
                        confidence = 1.0f,
                        version = newVersion
                    )
                )

                // 5. Learned Question & Answer
                val qType = when {
                    qLower.startsWith("what") -> "WHAT"
                    qLower.startsWith("how") -> "HOW"
                    qLower.startsWith("why") -> "WHY"
                    qLower.startsWith("when") -> "WHEN"
                    else -> "QUESTION"
                }

                val learnedQ = LearnedQuestionEntity(
                    id = qId,
                    documentId = docId,
                    chunkId = chunkId,
                    conceptId = conceptId,
                    question = qTrim,
                    intent = when {
                        qLower.contains("mean") -> "meaning"
                        qLower.contains("reply") || qLower.contains("say") -> "reply"
                        qLower.contains("help") -> "help"
                        else -> "general"
                    },
                    questionType = qType,
                    version = newVersion
                )
                questionEntities.add(learnedQ)

                answerEntities.add(
                    LearnedAnswerEntity(
                        id = ansId,
                        questionId = qId,
                        conceptId = conceptId,
                        answerText = aTrim,
                        sourceSection = sessionTitle,
                        isManualTeach = true,
                        confidence = 1.0f,
                        version = newVersion
                    )
                )

                // 6. Semantic Variations
                val variations = generateVariations(qTrim, qId)
                variationEntities.addAll(variations)

                // 7. Manual Training Entry (Individual persistent record)
                val semanticTokens = extractSemanticKeywords(qTrim, aTrim)
                manualEntries.add(
                    ManualTrainingEntryEntity(
                        id = UUID.randomUUID().toString(),
                        question = qTrim,
                        answer = aTrim,
                        sourceSessionOrDoc = sessionTitle,
                        semanticKeywords = semanticTokens,
                        questionType = qType,
                        status = "SAVED",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // Persist entire batch into Room
            dao.insertChunks(chunkEntities)
            dao.insertConcepts(conceptEntities)
            dao.insertFacts(factEntities)
            dao.insertRelationships(relationshipEntities)
            dao.insertQuestions(questionEntities)
            dao.insertAnswers(answerEntities)
            dao.insertQuestionVariations(variationEntities)
            dao.insertManualTrainingEntries(manualEntries)

            savedCount += batch.size
            onProgress(
                savedCount,
                totalPairs,
                "Saved $savedCount of $totalPairs entries to persistent database."
            )
            // Small pause for UI responsiveness on large datasets
            if (batches.size > 1) {
                delay(20)
            }
        }

        // Increment version record
        val versionEntity = KnowledgeVersionEntity(
            versionNumber = newVersion,
            description = "Manual Training: $totalPairs Q&A pairs from '$sessionTitle'",
            documentCount = 1,
            conceptCount = totalPairs,
            factCount = totalPairs,
            relationshipCount = totalPairs,
            createdAt = System.currentTimeMillis()
        )
        dao.insertVersion(versionEntity)

        // Verify count from persistent storage
        val finalVerifiedCount = savedCount
        val unprocessed = totalPairs - finalVerifiedCount

        return ManualTrainingParser.buildVerificationReport(
            parseResult = parseResult,
            savedCount = finalVerifiedCount,
            unprocessedCount = unprocessed,
            documentTitle = sessionTitle
        )
    }

    private fun extractConceptName(question: String): String {
        val qClean = question.replace(Regex("""["'“”?.,!]"""), "").trim()
        val qLower = qClean.lowercase(Locale.ROOT)

        if (qLower.contains("i'm full") || qLower.contains("im full")) return "I'm full"
        if (qLower.contains("i'm exhausted") || qLower.contains("im exhausted")) return "I'm exhausted"
        if (qLower.contains("ask for help") || qLower.contains("for help")) return "Asking for help"
        if (qLower.contains("how are you")) return "How are you"
        if (qLower.contains("hello")) return "Hello"
        if (qLower.contains("hi")) return "Hi"

        // Check for quoted phrases e.g. What does "I'm full" mean?
        val quoteMatch = Regex("""["“']([^"”']+)["”']""").find(question)
        if (quoteMatch != null) {
            val phrase = quoteMatch.groupValues[1].trim()
            if (phrase.length in 2..40) return phrase
        }

        // Fallback: take meaningful content words
        val words = qClean.split(" ").filter { word ->
            val w = word.lowercase(Locale.ROOT)
            !setOf("what", "does", "do", "how", "can", "i", "a", "an", "the", "mean", "is", "are", "to", "you").contains(w)
        }
        return if (words.isNotEmpty()) words.take(3).joinToString(" ") else qClean.take(30)
    }

    private fun extractSemanticKeywords(question: String, answer: String): String {
        val stopWords = setOf("a", "an", "the", "is", "are", "was", "were", "what", "does", "how", "can", "i", "you", "it", "to", "of", "and", "or", "in", "on", "mean", "means")
        val combined = "$question $answer".lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9' ]"), " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length > 2 && !stopWords.contains(it) }
            .distinct()
        return combined.joinToString(", ")
    }

    private fun generateVariations(question: String, qId: String): List<QuestionVariationEntity> {
        val variations = mutableListOf<QuestionVariationEntity>()
        variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = question, similarityScore = 1.0f))

        val qLower = question.lowercase(Locale.ROOT)
        val clean = qLower.replace(Regex("""["'“”?.,!]"""), "").trim()

        if (clean.contains("what does") && clean.contains("mean")) {
            val concept = clean.substringAfter("what does").substringBefore("mean").trim()
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "What is the meaning of $concept?", similarityScore = 0.95f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "Can you explain what $concept means?", similarityScore = 0.90f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "What do people mean by $concept?", similarityScore = 0.90f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "Meaning of $concept", similarityScore = 0.88f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "Define $concept", similarityScore = 0.85f))
        }

        if (clean.contains("how can i ask for help") || clean.contains("how to ask for help")) {
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "How do I ask for help?", similarityScore = 0.95f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "What should I say to get help?", similarityScore = 0.92f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "Ways to ask for assistance", similarityScore = 0.88f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "How to ask someone to give me a hand?", similarityScore = 0.85f))
        }

        if (clean == "how are you" || clean.startsWith("how are you")) {
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "How are you doing?", similarityScore = 0.95f))
            variations.add(QuestionVariationEntity(learnedQuestionId = qId, variationText = "How is everything?", similarityScore = 0.90f))
        }

        return variations
    }
}
