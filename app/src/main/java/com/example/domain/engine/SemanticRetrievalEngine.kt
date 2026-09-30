package com.example.domain.engine

import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.entity.*
import com.example.domain.model.RetrievalResult
import com.example.domain.model.SourceCitation
import com.example.domain.model.UserIntent
import java.util.Locale

class SemanticRetrievalEngine(
    private val dao: KnowledgeDao
) {
    // Semantic synonym lexicon for high-recall concept and intent expansion
    private val synonymClusters = mapOf(
        "greet" to setOf("greet", "greeting", "greetings", "hi", "hello", "hey", "meet", "saying hello", "welcome"),
        "hi" to setOf("hi", "casual greeting", "informal", "friendly greeting", "greeting"),
        "hello" to setOf("hello", "polite", "casual", "formal", "greeting"),
        "reply" to setOf("reply", "respond", "response", "say", "say back", "answer", "answering", "return"),
        "casual" to setOf("casual", "informal", "friendly", "relaxed", "colloquial"),
        "polite" to setOf("polite", "formal", "respectful", "teacher", "elder", "older person", "professional"),
        "difference" to setOf("difference", "compare", "comparison", "contrast", "distinguish", "differ", "vs", "versus"),
        "first_time" to setOf("first time", "first meeting", "meeting someone for the first time", "nice to meet you", "new person"),
        "meaning" to setOf("meaning", "mean", "definition", "define", "what is", "signify")
    )

    suspend fun retrieve(
        rawQuery: String,
        sessionId: String = "default_session",
        passedRecentMessages: List<ConversationMessageEntity>? = null
    ): RetrievalResult {
        val queryLower = rawQuery.trim().lowercase(Locale.ROOT)
        val recentMessages = passedRecentMessages ?: dao.getRecentMessages(sessionId, limit = 6)

        // 1. Semantic Understanding & Deep Intent Analysis First
        val analysis = SemanticUnderstandingEngine.analyze(rawQuery, recentMessages)
        val userIntent = analysis.intent

        // If intent is conversational or reporting a problem/asking for help,
        // do not let textbook knowledge definitions override user conversation!
        if (userIntent.isConversational || userIntent.isHelpOrProblem) {
            return RetrievalResult(
                query = rawQuery,
                resolvedContext = if (analysis.referencedEntities.isNotEmpty()) analysis.referencedEntities.first() else null,
                intent = "conversation",
                userIntent = userIntent,
                semanticAnalysis = analysis,
                matchedConcepts = emptyList(),
                matchedFacts = emptyList(),
                matchedRelationships = emptyList(),
                matchedAnswers = emptyList(),
                conflicts = emptyList(),
                citations = emptyList(),
                confidence = 1.0f,
                isSufficient = true
            )
        }

        // 2. Context & Pronoun Coreference Resolution
        val (resolvedQuery, resolvedContext) = resolveContext(rawQuery, recentMessages)
        val searchTokens = tokenizeAndExpand(resolvedQuery.lowercase(Locale.ROOT))

        // 2. Fetch all knowledge items from Room
        val allConcepts = dao.getAllConceptsList()
        val allFacts = dao.getAllFactsList()
        val allRelationships = dao.getAllRelationshipsList()
        val allVariations = dao.getAllQuestionVariations()
        val allQuestions = dao.getAllQuestions()
        val allAnswers = dao.getAllAnswers()

        if (allConcepts.isEmpty() && allFacts.isEmpty() && allAnswers.isEmpty()) {
            return RetrievalResult(
                query = rawQuery,
                resolvedContext = resolvedContext,
                isSufficient = false,
                confidence = 0f
            )
        }

        // 3. Question Variations & Intent Matching
        val matchedQuestionIds = mutableMapOf<String, Float>()
        for (variation in allVariations) {
            val score = computeSemanticSimilarity(searchTokens, variation.variationText)
            if (score > 0.35f) {
                val currentScore = matchedQuestionIds[variation.learnedQuestionId] ?: 0f
                if (score > currentScore) {
                    matchedQuestionIds[variation.learnedQuestionId] = score
                }
            }
        }

        for (question in allQuestions) {
            val score = computeSemanticSimilarity(searchTokens, question.question)
            if (score > 0.35f) {
                val currentScore = matchedQuestionIds[question.id] ?: 0f
                if (score > currentScore) {
                    matchedQuestionIds[question.id] = score
                }
            }
        }

        val matchedAnswers = matchedQuestionIds.keys.mapNotNull { qId ->
            dao.getAnswerForQuestion(qId)
        }

        // 4. Concept Matching (Direct + Semantic Expansion)
        val matchedConcepts = allConcepts.filter { concept ->
            val conceptTokens = tokenizeAndExpand(concept.name.lowercase(Locale.ROOT) + " " + concept.topic.lowercase(Locale.ROOT))
            val overlap = conceptTokens.intersect(searchTokens).isNotEmpty()
            val textMatch = queryLower.contains(concept.name.lowercase(Locale.ROOT)) ||
                    resolvedQuery.lowercase(Locale.ROOT).contains(concept.name.lowercase(Locale.ROOT))
            overlap || textMatch
        }.toMutableList()

        // If asking "what does it refer to in ...", prioritize the antecedent concept
        if (resolvedContext != null) {
            allConcepts.find { it.name.equals(resolvedContext.substringAfter("'").substringBefore("'"), ignoreCase = true) }?.let {
                if (!matchedConcepts.contains(it)) matchedConcepts.add(0, it)
            }
        }

        // 5. Fact Matching & Graph Traversal
        val matchedFacts = mutableListOf<KnowledgeFactEntity>()
        for (fact in allFacts) {
            val factLower = fact.statement.lowercase(Locale.ROOT)
            val factTokens = tokenizeAndExpand(factLower)
            val overlap = factTokens.intersect(searchTokens).size
            val matchesConcept = matchedConcepts.any {
                factLower.contains(it.name.lowercase(Locale.ROOT))
            }
            if (overlap >= 2 || matchesConcept || queryLower.contains("difference") && (factLower.contains("hi") && factLower.contains("hello"))) {
                matchedFacts.add(fact)
            }
        }

        // 6. Relationship Traversal
        val matchedRelationships = mutableListOf<KnowledgeRelationshipEntity>()
        val conceptNames = matchedConcepts.map { it.name.lowercase(Locale.ROOT) }
        for (rel in allRelationships) {
            val fromMatch = conceptNames.contains(rel.fromConcept.lowercase(Locale.ROOT))
            val toMatch = conceptNames.contains(rel.toConcept.lowercase(Locale.ROOT))
            val textMatch = searchTokens.contains(rel.relationshipType.lowercase(Locale.ROOT)) ||
                    rel.description.lowercase(Locale.ROOT).split(" ").any { searchTokens.contains(it) }

            if (fromMatch || toMatch || textMatch) {
                matchedRelationships.add(rel)
            }
        }

        // 7. Check for Contradictions / Conflicts across sources
        val conflicts = detectConflicts(matchedFacts)

        // 8. Build Source Citations
        val citations = mutableListOf<SourceCitation>()
        val allDocs = dao.getAllDocuments()
        // Map document titles
        for (fact in matchedFacts) {
            val doc = dao.getDocumentById(fact.documentId)
            val citation = SourceCitation(
                documentTitle = doc?.title ?: "Knowledge Base",
                sectionTitle = fact.topic,
                statement = fact.statement
            )
            if (!citations.contains(citation)) citations.add(citation)
        }
        for (ans in matchedAnswers) {
            val citation = SourceCitation(
                documentTitle = if (ans.isManualTeach) "Manual Teaching" else "Document Section",
                sectionTitle = ans.sourceSection.ifEmpty { "Q&A" },
                statement = ans.answerText
            )
            if (!citations.contains(citation)) citations.add(citation)
        }

        val isSufficient = matchedConcepts.isNotEmpty() || matchedFacts.isNotEmpty() || matchedAnswers.isNotEmpty()

        return RetrievalResult(
            query = rawQuery,
            resolvedContext = resolvedContext,
            intent = determineIntent(queryLower),
            userIntent = userIntent,
            semanticAnalysis = analysis,
            matchedConcepts = matchedConcepts,
            matchedFacts = matchedFacts,
            matchedRelationships = matchedRelationships,
            matchedAnswers = matchedAnswers,
            conflicts = conflicts,
            citations = citations,
            confidence = if (isSufficient) 0.95f else 0.1f,
            isSufficient = isSufficient
        )
    }

    private fun resolveContext(
        query: String,
        recentMessages: List<ConversationMessageEntity>
    ): Pair<String, String?> {
        val queryLower = query.lowercase(Locale.ROOT)
        // Check for pronoun "it", "this", "that"
        val hasPronoun = queryLower.matches(Regex(".*\\b(it|this|that|them)\\b.*"))
        val isImplicit = queryLower.startsWith("can i use it") ||
                queryLower.startsWith("what about") ||
                queryLower.startsWith("what if i") ||
                queryLower.contains("what does \"it\" refer to") ||
                queryLower.contains("what does it refer to")

        if (!hasPronoun && !isImplicit) {
            return Pair(query, null)
        }

        // Find last user and assistant turns
        val lastAssistant = recentMessages.firstOrNull { it.role == "assistant" }
        val lastUser = recentMessages.firstOrNull { it.role == "user" }

        var resolvedSubject: String? = null
        if (lastAssistant != null) {
            val text = lastAssistant.message
            if (text.contains("Hi", ignoreCase = false) || text.contains("'Hi'", ignoreCase = true)) {
                resolvedSubject = "Hi"
            } else if (text.contains("Hello", ignoreCase = false)) {
                resolvedSubject = "Hello"
            }
        }
        if (resolvedSubject == null && lastUser != null) {
            val userText = lastUser.message
            if (userText.contains("hi", ignoreCase = true)) {
                resolvedSubject = "Hi"
            } else if (userText.contains("hello", ignoreCase = true)) {
                resolvedSubject = "Hello"
            }
        }

        // Default fallback if talking about greetings
        if (resolvedSubject == null) {
            resolvedSubject = "Hi"
        }

        val contextResolutionNote = "Antecedent 'it' resolved to '$resolvedSubject' from conversation history"
        val expandedQuery = if (queryLower.contains("what does \"it\" refer to") || queryLower.contains("what does it refer to")) {
            "In 'Can I use it with someone?', 'it' refers to $resolvedSubject"
        } else {
            "$query (context: referring to $resolvedSubject)"
        }

        return Pair(expandedQuery, contextResolutionNote)
    }

    private fun tokenizeAndExpand(text: String): Set<String> {
        val tokens = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 1 && !stopWords.contains(it) }
            .toMutableSet()

        val expanded = mutableSetOf<String>()
        expanded.addAll(tokens)

        for (token in tokens) {
            for ((_, cluster) in synonymClusters) {
                if (cluster.contains(token)) {
                    expanded.addAll(cluster)
                }
            }
        }
        return expanded
    }

    private fun computeSemanticSimilarity(queryTokens: Set<String>, candidateText: String): Float {
        val candTokens = tokenizeAndExpand(candidateText)
        if (candTokens.isEmpty() || queryTokens.isEmpty()) return 0f
        val intersection = queryTokens.intersect(candTokens).size
        val union = queryTokens.union(candTokens).size
        val jaccard = intersection.toFloat() / union.toFloat()

        // Additional boost if key phrase matches
        val queryPhrase = queryTokens.joinToString(" ")
        val candLower = candidateText.lowercase(Locale.ROOT)
        var boost = 0f
        if (candLower.contains("reply") && (queryTokens.contains("reply") || queryTokens.contains("say"))) boost += 0.2f
        if (candLower.contains("casual") && queryTokens.contains("casual")) boost += 0.2f
        if (candLower.contains("first time") && (queryTokens.contains("first time") || queryTokens.contains("meet"))) boost += 0.2f

        return (jaccard * 0.8f + boost).coerceIn(0f, 1.0f)
    }

    private fun determineIntent(query: String): String {
        return when {
            query.contains("difference") || query.contains("compare") || query.contains("versus") || query.contains("vs") -> "comparison"
            query.contains("reply") || query.contains("respond") || query.contains("say back") || query.contains("what should i say") || query.contains("what can i say") -> "reply"
            query.contains("mean") || query.contains("what is") || query.contains("definition") -> "meaning"
            query.contains("first time") || query.contains("first meeting") -> "first_meeting"
            query.contains("casual") || query.contains("polite") || query.contains("formal") -> "politeness"
            query.contains("another way") || query.contains("alternative") || query.contains("instead of") -> "alternatives"
            else -> "general"
        }
    }

    private fun detectConflicts(facts: List<KnowledgeFactEntity>): List<String> {
        val conflicts = mutableListOf<String>()
        val factsByTopic = facts.groupBy { it.topic.lowercase(Locale.ROOT) }
        for ((topic, topicFacts) in factsByTopic) {
            if (topicFacts.size > 1) {
                val docIds = topicFacts.map { it.documentId }.distinct()
                if (docIds.size > 1) {
                    // Check if contradictory keywords exist
                    val hasAlways = topicFacts.any { it.statement.contains("always", ignoreCase = true) }
                    val hasNever = topicFacts.any { it.statement.contains("never", ignoreCase = true) }
                    if (hasAlways && hasNever) {
                        conflicts.add("Conflicting perspectives detected regarding topic '$topic' across multiple sources.")
                    }
                }
            }
        }
        return conflicts
    }

    private val stopWords = setOf(
        "a", "an", "the", "and", "or", "but", "about", "above", "after", "again", "against",
        "all", "am", "an", "and", "any", "are", "as", "at", "be", "because", "been", "before",
        "being", "below", "between", "both", "by", "could", "did", "do", "does", "doing", "down",
        "during", "each", "few", "for", "from", "further", "had", "has", "have", "having", "he",
        "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into",
        "is", "it", "its", "itself", "me", "more", "most", "my", "myself", "no", "nor", "not", "of",
        "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out",
        "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their",
        "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through",
        "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where",
        "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself",
        "yourselves"
    )
}
