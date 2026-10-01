package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "knowledge_documents")
data class KnowledgeDocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourceType: String = "BOOK", // BOOK, NOTE, MANUAL_TEACH, SAMPLE
    val content: String,
    val totalChunks: Int = 1,
    val version: Int = 1,
    val status: String = "PROCESSED", // ANALYZING, PROCESSED, ERROR
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_chunks")
data class KnowledgeChunkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val chunkIndex: Int,
    val sectionTitle: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "concepts")
data class KnowledgeConceptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val chunkId: String,
    val name: String,
    val topic: String,
    val definition: String,
    val examplesJson: String = "[]",
    val relatedConceptsJson: String = "[]",
    val confidence: Float = 1.0f,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "facts")
data class KnowledgeFactEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val chunkId: String,
    val topic: String,
    val statement: String,
    val category: String = "FACT", // RULE, DEFINITION, COMPARISON, EXAMPLE, BEHAVIOR, INSTRUCTION
    val confidence: Float = 1.0f,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "relationships")
data class KnowledgeRelationshipEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fromConcept: String,
    val relationshipType: String, // related_to, used_for, possible_reply, example, prerequisite, more_casual_than, more_polite_than, situation
    val toConcept: String,
    val description: String = "",
    val confidence: Float = 1.0f,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learned_questions")
data class LearnedQuestionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val chunkId: String,
    val conceptId: String,
    val question: String,
    val intent: String = "general", // meaning, usage, reply, comparison, first_time, politeness, general
    val questionType: String = "WHAT", // WHAT, HOW, WHEN, WHICH, IS, EXPLAIN
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learned_answers")
data class LearnedAnswerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val questionId: String,
    val conceptId: String,
    val answerText: String,
    val sourceSection: String = "",
    val isManualTeach: Boolean = false,
    val confidence: Float = 1.0f,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "question_variations")
data class QuestionVariationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learnedQuestionId: String,
    val variationText: String,
    val similarityScore: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "default_session",
    val role: String, // user, assistant
    val message: String,
    val usedSourcesJson: String = "[]",
    val resolvedContext: String? = null,
    val analysisMetadataJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "training_runs")
data class TrainingRunEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val documentTitle: String,
    val stepName: String,
    val status: String = "COMPLETED", // IN_PROGRESS, COMPLETED, FAILED
    val progress: Float = 1.0f,
    val totalSteps: Int = 7,
    val currentStepIndex: Int = 7,
    val summary: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_versions")
data class KnowledgeVersionEntity(
    @PrimaryKey val versionNumber: Int,
    val description: String,
    val documentCount: Int = 0,
    val conceptCount: Int = 0,
    val factCount: Int = 0,
    val relationshipCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "manual_training_entries")
data class ManualTrainingEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answer: String,
    val sourceSessionOrDoc: String,
    val semanticKeywords: String = "",
    val questionType: String = "WHAT",
    val status: String = "SAVED",
    val createdAt: Long = System.currentTimeMillis()
)

