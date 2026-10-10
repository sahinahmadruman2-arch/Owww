package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Memory Category constants matching user specifications
 */
object MemoryCategories {
    const val CONVERSATION_CONTEXT = "Conversation Context"
    const val USER_PREFERENCES = "User Preferences"
    const val USER_SKILLS = "User Skills"
    const val ACTIVE_PROJECTS = "Active Projects"
    const val IMPORTANT_FACTS = "Important Facts"
    const val CORRECTIONS_AND_FEEDBACK = "Corrections and Feedback"
    const val GENERAL_KNOWLEDGE = "General Knowledge"
    const val LEARNING_CANDIDATES = "Learning Candidates"
}

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val category: String, // One of MemoryCategories
    val key: String,
    val value: String,
    val confidence: Float = 0.9f,
    val sourceSessionId: String = "default_session",
    val sourceMessageId: String? = null,
    val isSensitive: Boolean = false,
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_skill_profiles")
data class UserSkillProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val topic: String, // e.g. "Lua", "Roblox", "Game Development", "Physics"
    val level: String = "BEGINNER", // BEGINNER, INTERMEDIATE, ADVANCED
    val confidence: Float = 0.75f,
    val topicsUnderstoodJson: String = "[]",
    val topicsNeedingHelpJson: String = "[]",
    val tasksCompletedJson: String = "[]",
    val preferredExplanationStyle: String = "STEP_BY_STEP", // STEP_BY_STEP, CONCISE, THEORETICAL, PRACTICAL_EXAMPLES
    val lastEvidence: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_candidates")
data class LearningCandidateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val title: String,
    val extractedContent: String,
    val category: String = MemoryCategories.LEARNING_CANDIDATES,
    val sourceType: String = "CONVERSATION", // CONVERSATION, WEB_RESEARCH, USER_CORRECTION
    val sourceOrigin: String = "",
    val status: String = "PENDING_REVIEW", // PENDING_REVIEW, APPROVED, REJECTED, VERIFIED
    val isSharedKnowledge: Boolean = false,
    val reliabilityScore: Float = 0.85f,
    val privacyRisk: String = "NONE", // NONE, LOW, POTENTIAL_PII
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shared_knowledge")
data class SharedKnowledgeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val category: String = "General Knowledge",
    val sourceUrl: String? = null,
    val sourceDomain: String? = null,
    val verificationStatus: String = "VERIFIED", // VERIFIED, COMMUNITY_CONSENSUS, OFFICIAL_DOC
    val confidenceScore: Float = 0.95f,
    val relatedConceptsJson: String = "[]",
    val isShared: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_sources")
data class KnowledgeSourceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val snippet: String,
    val domain: String,
    val retrievalTimestamp: Long = System.currentTimeMillis(),
    val verificationStatus: String = "VERIFIED", // VERIFIED, PENDING, UNTRUSTED
    val isSavedToKnowledge: Boolean = false,
    val extractedSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_tasks")
data class LearningTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskType: String, // DOCUMENT_ANALYSIS, KNOWLEDGE_EXTRACTION, DUPLICATE_DETECTION, SEMANTIC_INDEXING, SOURCE_VERIFICATION, MEMORY_CLEANUP, EVALUATION_REPORT
    val title: String,
    val status: String = "QUEUED", // QUEUED, RUNNING, PAUSED, COMPLETED, FAILED
    val progress: Float = 0.0f,
    val details: String = "",
    val errorMessage: String? = null,
    val isOnlineTask: Boolean = false,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_events")
data class LearningEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventType: String, // CONVERSATION_ANALYSIS, COREFERENCE_RESOLVED, SKILL_UPDATED, CANDIDATE_CREATED, DEDUPLICATION_CHECK, WEB_RESEARCH, SOURCE_VERIFIED, INDEX_UPDATED, QUALITY_EVALUATED, ERROR_DETECTED, PATCH_PROPOSED, TEST_EXECUTED, OWNER_APPROVAL_REQUESTED
    val summary: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String? = null
)

@Entity(tableName = "response_evaluations")
data class ResponseEvaluationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val messageId: String = "",
    val query: String,
    val response: String,
    val relevanceScore: Float = 0.95f,
    val contextAccuracyScore: Float = 0.95f,
    val factualReliabilityScore: Float = 0.95f,
    val taskCompletionScore: Float = 0.95f,
    val instructionFollowScore: Float = 0.95f,
    val passed: Boolean = true,
    val failureReason: String? = null,
    val retryCount: Int = 0,
    val evaluatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "diagnostic_reports")
data class DiagnosticReportEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val component: String,
    val severity: String = "INFO", // INFO, WARNING, ERROR, CRITICAL
    val errorMessage: String,
    val stackTrace: String? = null,
    val rootCauseHypothesis: String = "",
    val detectedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "proposed_patches")
data class ProposedPatchEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetFile: String,
    val description: String,
    val patchDiff: String,
    val testResults: String = "PENDING_VERIFICATION",
    val status: String = "PROPOSED", // PROPOSED, TEST_PASSED, TEST_FAILED, APPROVED, REJECTED, ROLLED_BACK
    val proposedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val action: String,
    val actor: String = "SYSTEM", // SYSTEM, USER, OWNER
    val resourceType: String,
    val resourceId: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_engine_settings")
data class LearningEngineSettingsEntity(
    @PrimaryKey val id: String = "global_settings",
    val isAutoLearningEnabled: Boolean = true,
    val isWebLearningEnabled: Boolean = true,
    val isMemoryExtractionEnabled: Boolean = true,
    val maxDailyCandidates: Int = 50,
    val maxStorageMb: Int = 100,
    val requireApprovalForSensitive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
