package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {

    // Documents
    @Query("SELECT * FROM knowledge_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): KnowledgeDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: KnowledgeDocumentEntity)

    @Update
    suspend fun updateDocument(doc: KnowledgeDocumentEntity)

    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    // Chunks
    @Query("SELECT * FROM knowledge_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: String): List<KnowledgeChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>)

    @Query("DELETE FROM knowledge_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocumentId(documentId: String)

    // Concepts
    @Query("SELECT * FROM concepts ORDER BY name ASC")
    fun getAllConcepts(): Flow<List<KnowledgeConceptEntity>>

    @Query("SELECT * FROM concepts")
    suspend fun getAllConceptsList(): List<KnowledgeConceptEntity>

    @Query("SELECT * FROM concepts WHERE documentId = :documentId")
    suspend fun getConceptsByDocumentId(documentId: String): List<KnowledgeConceptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<KnowledgeConceptEntity>)

    @Query("DELETE FROM concepts WHERE documentId = :documentId")
    suspend fun deleteConceptsByDocumentId(documentId: String)

    // Facts
    @Query("SELECT * FROM facts ORDER BY createdAt DESC")
    fun getAllFacts(): Flow<List<KnowledgeFactEntity>>

    @Query("SELECT * FROM facts")
    suspend fun getAllFactsList(): List<KnowledgeFactEntity>

    @Query("SELECT * FROM facts WHERE documentId = :documentId")
    suspend fun getFactsByDocumentId(documentId: String): List<KnowledgeFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacts(facts: List<KnowledgeFactEntity>)

    @Query("DELETE FROM facts WHERE documentId = :documentId")
    suspend fun deleteFactsByDocumentId(documentId: String)

    // Relationships
    @Query("SELECT * FROM relationships ORDER BY createdAt DESC")
    fun getAllRelationships(): Flow<List<KnowledgeRelationshipEntity>>

    @Query("SELECT * FROM relationships")
    suspend fun getAllRelationshipsList(): List<KnowledgeRelationshipEntity>

    @Query("SELECT * FROM relationships WHERE fromConcept = :concept OR toConcept = :concept")
    suspend fun getRelationshipsForConcept(concept: String): List<KnowledgeRelationshipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<KnowledgeRelationshipEntity>)

    // Questions & Answers
    @Query("SELECT * FROM learned_questions")
    suspend fun getAllQuestions(): List<LearnedQuestionEntity>

    @Query("SELECT * FROM learned_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): LearnedQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<LearnedQuestionEntity>)

    @Query("SELECT * FROM learned_answers WHERE questionId = :questionId LIMIT 1")
    suspend fun getAnswerForQuestion(questionId: String): LearnedAnswerEntity?

    @Query("SELECT * FROM learned_answers")
    suspend fun getAllAnswers(): List<LearnedAnswerEntity>

    @Query("SELECT * FROM learned_answers")
    fun getAllAnswersFlow(): Flow<List<LearnedAnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<LearnedAnswerEntity>)

    // Question Variations
    @Query("SELECT * FROM question_variations")
    suspend fun getAllQuestionVariations(): List<QuestionVariationEntity>

    @Query("SELECT * FROM question_variations")
    fun getAllQuestionVariationsFlow(): Flow<List<QuestionVariationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionVariations(variations: List<QuestionVariationEntity>)

    // Conversations
    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ConversationMessageEntity>>

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String, limit: Int = 10): List<ConversationMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessageEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM conversations WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    // Training Runs
    @Query("SELECT * FROM training_runs ORDER BY startedAt DESC")
    fun getAllTrainingRuns(): Flow<List<TrainingRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingRun(run: TrainingRunEntity)

    @Update
    suspend fun updateTrainingRun(run: TrainingRunEntity)

    // Knowledge Versions
    @Query("SELECT * FROM knowledge_versions ORDER BY versionNumber DESC")
    fun getAllVersions(): Flow<List<KnowledgeVersionEntity>>

    @Query("SELECT MAX(versionNumber) FROM knowledge_versions")
    suspend fun getLatestVersionNumber(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: KnowledgeVersionEntity)

    // Counts for stats
    @Query("SELECT COUNT(*) FROM knowledge_documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM knowledge_chunks")
    fun getChunkCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM concepts")
    fun getConceptCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM facts")
    fun getFactCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM learned_questions")
    fun getQuestionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM relationships")
    fun getRelationshipCount(): Flow<Int>

    // Manual Training Entries
    @Query("SELECT * FROM manual_training_entries ORDER BY createdAt DESC")
    fun getAllManualTrainingEntriesFlow(): Flow<List<ManualTrainingEntryEntity>>

    @Query("SELECT * FROM manual_training_entries")
    suspend fun getAllManualTrainingEntries(): List<ManualTrainingEntryEntity>

    @Query("SELECT COUNT(*) FROM manual_training_entries")
    fun getManualTrainingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM manual_training_entries")
    suspend fun getManualTrainingCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualTrainingEntries(entries: List<ManualTrainingEntryEntity>)

    @Query("DELETE FROM manual_training_entries WHERE id = :id")
    suspend fun deleteManualTrainingEntryById(id: String)
}
