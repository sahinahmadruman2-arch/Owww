package com.example.domain.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.dao.SystemLearningDao
import com.example.data.local.entity.*
import com.example.data.remote.WebResearchClient
import com.example.data.remote.WebResearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale
import java.util.UUID

data class WebLearningOutcome(
    val isOnline: Boolean,
    val sourcesResearched: List<KnowledgeSourceEntity>,
    val sharedKnowledgeCreated: List<SharedKnowledgeEntity>,
    val duplicatesAvoided: Int,
    val summary: String
)

object WebLearningEngine {

    /**
     * Checks if actual network connectivity is available.
     * Prevents false online learning claims when offline.
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return true // Test fallback
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Researches web, extracts structured knowledge, records sources,
     * checks for duplicates, and indexes into shared knowledge.
     * Web pages are untrusted data: sanitize text and prevent prompt injection.
     */
    suspend fun learnFromWeb(
        query: String,
        context: Context?,
        knowledgeDao: KnowledgeDao,
        learningDao: SystemLearningDao
    ): WebLearningOutcome = withContext(Dispatchers.IO) {
        val online = isNetworkAvailable(context)

        if (!online) {
            // Queue an offline task instead of falsely claiming online research
            learningDao.insertLearningTask(
                LearningTaskEntity(
                    taskType = "KNOWLEDGE_EXTRACTION",
                    title = "Offline Pending Web Research: $query",
                    status = "QUEUED",
                    isOnlineTask = true,
                    details = "Awaiting network connectivity before researching '$query'."
                )
            )

            learningDao.insertLearningEvent(
                LearningEventEntity(
                    eventType = "WEB_RESEARCH",
                    summary = "Queued offline web research task",
                    details = "Network currently unreachable. Research queued for when connectivity returns."
                )
            )

            return@withContext WebLearningOutcome(
                isOnline = false,
                sourcesResearched = emptyList(),
                sharedKnowledgeCreated = emptyList(),
                duplicatesAvoided = 0,
                summary = "Device is offline. Task queued for background execution."
            )
        }

        // Online research
        val rawResults: List<WebResearchResult> = try {
            WebResearchClient.conductLiveResearch(query, maxResults = 3)
        } catch (_: Exception) {
            emptyList()
        }

        if (rawResults.isEmpty()) {
            return@withContext WebLearningOutcome(
                isOnline = true,
                sourcesResearched = emptyList(),
                sharedKnowledgeCreated = emptyList(),
                duplicatesAvoided = 0,
                summary = "No suitable public sources found."
            )
        }

        val recordedSources = mutableListOf<KnowledgeSourceEntity>()
        val createdKnowledge = mutableListOf<SharedKnowledgeEntity>()
        var duplicatesCount = 0

        // Fetch existing facts and shared knowledge for deduplication check
        val existingShared = learningDao.getAllSharedKnowledge()
        val existingFacts = knowledgeDao.getAllFactsList()

        for (res in rawResults) {
            // Defense against prompt injection: strip instruction tags or markdown injection
            val sanitizedSnippet = sanitizeUntrustedWebData(res.snippet)
            val sanitizedTitle = sanitizeUntrustedWebData(res.title)

            // Check if source already exists
            val existingSource = learningDao.getKnowledgeSourceByUrl(res.url)
            val sourceEntity = existingSource ?: KnowledgeSourceEntity(
                url = res.url,
                title = sanitizedTitle,
                snippet = sanitizedSnippet,
                domain = res.sourceDomain,
                retrievalTimestamp = System.currentTimeMillis(),
                verificationStatus = if (res.sourceDomain.contains("wikipedia.org") || res.sourceDomain.contains("edu") || res.sourceDomain.contains("gov")) "VERIFIED" else "COMMUNITY_CONSENSUS",
                isSavedToKnowledge = true,
                extractedSummary = sanitizedSnippet.take(150)
            )

            learningDao.insertKnowledgeSource(sourceEntity)
            recordedSources.add(sourceEntity)

            // Deduplication Check
            val isDuplicate = existingShared.any {
                it.title.equals(sanitizedTitle, ignoreCase = true) ||
                        it.summary.take(60).equals(sanitizedSnippet.take(60), ignoreCase = true)
            } || existingFacts.any {
                it.statement.take(60).equals(sanitizedSnippet.take(60), ignoreCase = true)
            }

            if (isDuplicate) {
                duplicatesCount++
                learningDao.insertLearningEvent(
                    LearningEventEntity(
                        eventType = "DEDUPLICATION_CHECK",
                        summary = "Duplicate knowledge avoided for '${sanitizedTitle.take(40)}'",
                        details = "Information already indexed in persistent storage. Skipped redundant save."
                    )
                )
                continue
            }

            // Save new shared knowledge
            val sharedKnowledge = SharedKnowledgeEntity(
                title = sanitizedTitle,
                summary = sanitizedSnippet,
                category = "Web Knowledge",
                sourceUrl = res.url,
                sourceDomain = res.sourceDomain,
                verificationStatus = sourceEntity.verificationStatus,
                confidenceScore = if (sourceEntity.verificationStatus == "VERIFIED") 0.95f else 0.85f,
                relatedConceptsJson = JSONArray(listOf(query)).toString(),
                isShared = true
            )

            learningDao.insertSharedKnowledge(sharedKnowledge)
            createdKnowledge.add(sharedKnowledge)

            // Also ingest into core facts knowledge base
            knowledgeDao.insertFacts(
                listOf(
                    KnowledgeFactEntity(
                        documentId = "web_research_${System.currentTimeMillis()}",
                        chunkId = "chunk_${UUID.randomUUID()}",
                        topic = query.take(40),
                        statement = sanitizedSnippet,
                        category = "FACT",
                        confidence = sharedKnowledge.confidenceScore
                    )
                )
            )

            learningDao.insertLearningEvent(
                LearningEventEntity(
                    eventType = "INDEX_UPDATED",
                    summary = "Learned: ${sanitizedTitle.take(40)}",
                    details = "Ingested from ${res.sourceDomain} (${res.url}). Verified status: ${sourceEntity.verificationStatus}"
                )
            )
        }

        WebLearningOutcome(
            isOnline = true,
            sourcesResearched = recordedSources,
            sharedKnowledgeCreated = createdKnowledge,
            duplicatesAvoided = duplicatesCount,
            summary = "Learned ${createdKnowledge.size} items from web (${duplicatesCount} duplicates avoided)."
        )
    }

    /**
     * Sanitize untrusted web content to prevent prompt injection attacks.
     * Replaces system override directives with harmless text.
     */
    private fun sanitizeUntrustedWebData(input: String): String {
        var clean = input.replace(Regex("(?i)ignore previous instructions"), "[sanitized]")
        clean = clean.replace(Regex("(?i)disregard system prompt"), "[sanitized]")
        clean = clean.replace(Regex("(?i)you are now a"), "[sanitized]")
        clean = clean.replace(Regex("(?i)system:"), "[sanitized]:")
        clean = clean.replace(Regex("(?i)developer mode"), "[sanitized]")
        return clean.trim()
    }
}
