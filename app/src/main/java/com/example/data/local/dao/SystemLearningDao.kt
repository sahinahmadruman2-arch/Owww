package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemLearningDao {

    // User Memories
    @Query("SELECT * FROM user_memories WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getUserMemoriesFlow(userId: String = "user_default"): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getUserMemories(userId: String = "user_default"): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE userId = :userId AND category = :category ORDER BY updatedAt DESC")
    suspend fun getUserMemoriesByCategory(userId: String = "user_default", category: String): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE id = :id LIMIT 1")
    suspend fun getUserMemoryById(id: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMemory(memory: UserMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMemories(memories: List<UserMemoryEntity>)

    @Update
    suspend fun updateUserMemory(memory: UserMemoryEntity)

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteUserMemoryById(id: String)

    @Query("DELETE FROM user_memories WHERE userId = :userId")
    suspend fun clearUserMemories(userId: String = "user_default")

    @Query("SELECT COUNT(*) FROM user_memories WHERE userId = :userId")
    fun getUserMemoryCountFlow(userId: String = "user_default"): Flow<Int>

    // User Skill Profiles
    @Query("SELECT * FROM user_skill_profiles WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getUserSkillProfilesFlow(userId: String = "user_default"): Flow<List<UserSkillProfileEntity>>

    @Query("SELECT * FROM user_skill_profiles WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getUserSkillProfiles(userId: String = "user_default"): List<UserSkillProfileEntity>

    @Query("SELECT * FROM user_skill_profiles WHERE userId = :userId AND topic = :topic LIMIT 1")
    suspend fun getUserSkillProfileByTopic(userId: String = "user_default", topic: String): UserSkillProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSkillProfile(profile: UserSkillProfileEntity)

    @Query("DELETE FROM user_skill_profiles WHERE id = :id")
    suspend fun deleteUserSkillProfileById(id: String)

    // Learning Candidates
    @Query("SELECT * FROM learning_candidates ORDER BY createdAt DESC")
    fun getAllLearningCandidatesFlow(): Flow<List<LearningCandidateEntity>>

    @Query("SELECT * FROM learning_candidates WHERE status = 'PENDING_REVIEW' ORDER BY createdAt DESC")
    fun getPendingLearningCandidatesFlow(): Flow<List<LearningCandidateEntity>>

    @Query("SELECT * FROM learning_candidates")
    suspend fun getAllLearningCandidates(): List<LearningCandidateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningCandidate(candidate: LearningCandidateEntity)

    @Update
    suspend fun updateLearningCandidate(candidate: LearningCandidateEntity)

    @Query("UPDATE learning_candidates SET status = :status WHERE id = :id")
    suspend fun updateCandidateStatus(id: String, status: String)

    @Query("DELETE FROM learning_candidates WHERE id = :id")
    suspend fun deleteLearningCandidateById(id: String)

    @Query("SELECT COUNT(*) FROM learning_candidates WHERE status = 'PENDING_REVIEW'")
    fun getPendingCandidateCountFlow(): Flow<Int>

    // Shared Knowledge
    @Query("SELECT * FROM shared_knowledge ORDER BY updatedAt DESC")
    fun getAllSharedKnowledgeFlow(): Flow<List<SharedKnowledgeEntity>>

    @Query("SELECT * FROM shared_knowledge ORDER BY updatedAt DESC")
    suspend fun getAllSharedKnowledge(): List<SharedKnowledgeEntity>

    @Query("SELECT * FROM shared_knowledge WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%'")
    suspend fun searchSharedKnowledge(query: String): List<SharedKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedKnowledge(knowledge: SharedKnowledgeEntity)

    @Update
    suspend fun updateSharedKnowledge(knowledge: SharedKnowledgeEntity)

    @Query("DELETE FROM shared_knowledge WHERE id = :id")
    suspend fun deleteSharedKnowledgeById(id: String)

    @Query("SELECT COUNT(*) FROM shared_knowledge")
    fun getSharedKnowledgeCountFlow(): Flow<Int>

    // Knowledge Sources (Web Learning)
    @Query("SELECT * FROM knowledge_sources ORDER BY retrievalTimestamp DESC")
    fun getAllKnowledgeSourcesFlow(): Flow<List<KnowledgeSourceEntity>>

    @Query("SELECT * FROM knowledge_sources WHERE url = :url LIMIT 1")
    suspend fun getKnowledgeSourceByUrl(url: String): KnowledgeSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeSource(source: KnowledgeSourceEntity)

    @Query("DELETE FROM knowledge_sources WHERE id = :id")
    suspend fun deleteKnowledgeSourceById(id: String)

    // Learning Tasks Queue
    @Query("SELECT * FROM learning_tasks ORDER BY createdAt DESC")
    fun getAllLearningTasksFlow(): Flow<List<LearningTaskEntity>>

    @Query("SELECT * FROM learning_tasks WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getTasksByStatus(status: String): List<LearningTaskEntity>

    @Query("SELECT * FROM learning_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): LearningTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningTask(task: LearningTaskEntity)

    @Update
    suspend fun updateLearningTask(task: LearningTaskEntity)

    @Query("DELETE FROM learning_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT COUNT(*) FROM learning_tasks WHERE status = 'QUEUED'")
    fun getQueuedTaskCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM learning_tasks WHERE status = 'RUNNING'")
    fun getRunningTaskCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM learning_tasks WHERE status = 'COMPLETED'")
    fun getCompletedTaskCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM learning_tasks WHERE status = 'FAILED'")
    fun getFailedTaskCountFlow(): Flow<Int>

    // Learning Events (Live Activity Feed)
    @Query("SELECT * FROM learning_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLearningEventsFlow(limit: Int = 100): Flow<List<LearningEventEntity>>

    @Query("SELECT * FROM learning_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLearningEvents(limit: Int = 50): List<LearningEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningEvent(event: LearningEventEntity)

    @Query("DELETE FROM learning_events")
    suspend fun clearLearningEvents()

    // Response Evaluations
    @Query("SELECT * FROM response_evaluations ORDER BY evaluatedAt DESC LIMIT :limit")
    fun getResponseEvaluationsFlow(limit: Int = 50): Flow<List<ResponseEvaluationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponseEvaluation(evaluation: ResponseEvaluationEntity)

    // Diagnostic Reports
    @Query("SELECT * FROM diagnostic_reports ORDER BY detectedAt DESC")
    fun getDiagnosticReportsFlow(): Flow<List<DiagnosticReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticReport(report: DiagnosticReportEntity)

    // Proposed Patches (Self-Debugging & Code Improvement)
    @Query("SELECT * FROM proposed_patches ORDER BY proposedAt DESC")
    fun getProposedPatchesFlow(): Flow<List<ProposedPatchEntity>>

    @Query("SELECT * FROM proposed_patches WHERE id = :id LIMIT 1")
    suspend fun getProposedPatchById(id: String): ProposedPatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposedPatch(patch: ProposedPatchEntity)

    @Update
    suspend fun updateProposedPatch(patch: ProposedPatchEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getAuditLogsFlow(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Settings
    @Query("SELECT * FROM learning_engine_settings WHERE id = 'global_settings' LIMIT 1")
    suspend fun getSettings(): LearningEngineSettingsEntity?

    @Query("SELECT * FROM learning_engine_settings WHERE id = 'global_settings' LIMIT 1")
    fun getSettingsFlow(): Flow<LearningEngineSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: LearningEngineSettingsEntity)
}
