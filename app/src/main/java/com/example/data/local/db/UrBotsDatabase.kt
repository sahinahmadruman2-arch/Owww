package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.entity.*

@Database(
    entities = [
        KnowledgeDocumentEntity::class,
        KnowledgeChunkEntity::class,
        KnowledgeConceptEntity::class,
        KnowledgeFactEntity::class,
        KnowledgeRelationshipEntity::class,
        LearnedQuestionEntity::class,
        LearnedAnswerEntity::class,
        QuestionVariationEntity::class,
        ConversationMessageEntity::class,
        TrainingRunEntity::class,
        KnowledgeVersionEntity::class,
        ManualTrainingEntryEntity::class,
        LiveFeedEntryEntity::class,
        UserMemoryEntity::class,
        UserSkillProfileEntity::class,
        LearningCandidateEntity::class,
        SharedKnowledgeEntity::class,
        KnowledgeSourceEntity::class,
        LearningTaskEntity::class,
        LearningEventEntity::class,
        ResponseEvaluationEntity::class,
        DiagnosticReportEntity::class,
        ProposedPatchEntity::class,
        AuditLogEntity::class,
        LearningEngineSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class UrBotsDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun systemLearningDao(): com.example.data.local.dao.SystemLearningDao

    companion object {
        @Volatile
        private var INSTANCE: UrBotsDatabase? = null

        fun getDatabase(context: Context): UrBotsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UrBotsDatabase::class.java,
                    "urbots7_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
