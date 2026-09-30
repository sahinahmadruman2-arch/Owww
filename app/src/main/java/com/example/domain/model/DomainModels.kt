package com.example.domain.model

import com.example.data.local.entity.KnowledgeConceptEntity
import com.example.data.local.entity.KnowledgeFactEntity
import com.example.data.local.entity.KnowledgeRelationshipEntity
import com.example.data.local.entity.LearnedAnswerEntity

enum class UserIntent {
    NORMAL_CONVERSATION,
    QUESTION,
    REQUEST_FOR_INFORMATION,
    INSTRUCTION,
    LEARNING_EXPLANATION,
    GREETING,
    FAREWELL,
    THANKS,
    APOLOGY,
    CASUAL_STATEMENT,
    AMBIGUOUS;

    val isConversational: Boolean
        get() = this == GREETING || this == NORMAL_CONVERSATION || this == CASUAL_STATEMENT ||
                this == THANKS || this == FAREWELL || this == APOLOGY
}

data class SourceCitation(
    val documentTitle: String,
    val sectionTitle: String,
    val conceptName: String? = null,
    val statement: String? = null
)

data class RetrievalResult(
    val query: String,
    val resolvedContext: String? = null,
    val intent: String = "general",
    val userIntent: UserIntent = UserIntent.QUESTION,
    val matchedConcepts: List<KnowledgeConceptEntity> = emptyList(),
    val matchedFacts: List<KnowledgeFactEntity> = emptyList(),
    val matchedRelationships: List<KnowledgeRelationshipEntity> = emptyList(),
    val matchedAnswers: List<LearnedAnswerEntity> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val citations: List<SourceCitation> = emptyList(),
    val confidence: Float = 1.0f,
    val isSufficient: Boolean = true
)

data class GeneratedAnswerResult(
    val answerText: String,
    val sources: List<SourceCitation> = emptyList(),
    val resolvedContext: String? = null,
    val userIntent: UserIntent? = null,
    val isSufficient: Boolean = true,
    val confidence: Float = 1.0f,
    val hasConflict: Boolean = false,
    val conflictDescription: String? = null
)

data class PipelineStageProgress(
    val stageName: String,
    val progress: Float,
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

data class VerificationTestItem(
    val id: Int,
    val query: String,
    val expectedTheme: String,
    var actualAnswer: String = "",
    var isPassed: Boolean? = null,
    var sources: List<String> = emptyList()
)
