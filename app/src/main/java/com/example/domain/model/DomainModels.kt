package com.example.domain.model

import com.example.data.local.entity.KnowledgeConceptEntity
import com.example.data.local.entity.KnowledgeFactEntity
import com.example.data.local.entity.KnowledgeRelationshipEntity
import com.example.data.local.entity.LearnedAnswerEntity

enum class UserIntent {
    GREETING,
    FAREWELL,
    CASUAL_CONVERSATION,
    QUESTION,
    REQUESTING_HELP,
    REPORTING_PROBLEM,
    REQUESTING_EXPLANATION,
    REQUESTING_INSTRUCTIONS,
    THANKING,
    APOLOGY,
    AGREEMENT,
    DISAGREEMENT,
    CONFUSION,
    STATEMENT,
    FOLLOW_UP,
    OTHER,

    // Aliases / Compatibility
    NORMAL_CONVERSATION,
    REQUEST_FOR_INFORMATION,
    INSTRUCTION,
    LEARNING_EXPLANATION,
    THANKS,
    CASUAL_STATEMENT,
    AMBIGUOUS;

    val isConversational: Boolean
        get() = this == GREETING || this == CASUAL_CONVERSATION || this == NORMAL_CONVERSATION ||
                this == CASUAL_STATEMENT || this == THANKING || this == THANKS ||
                this == FAREWELL || this == APOLOGY || this == AGREEMENT || this == DISAGREEMENT ||
                this == STATEMENT

    val isHelpOrProblem: Boolean
        get() = this == REQUESTING_HELP || this == REPORTING_PROBLEM || this == CONFUSION
}

data class MessageSemanticAnalysis(
    val rawText: String,
    val intent: UserIntent,
    val topic: String? = null,
    val entities: List<String> = emptyList(),
    val action: String? = null,
    val speechAct: String = "statement", // asking, telling, greeting, requesting_help, reporting_problem, explaining, continuing
    val sentiment: String = "neutral", // positive, negative, neutral, concerned, frustrated, confused
    val conversationGoal: String = "Communicate with UrBots7",
    val referencedEntities: List<String> = emptyList(), // resolved "it", "that", "the game", etc.
    val confidence: String = "High",
    val detectedMeaning: String
)

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
    val semanticAnalysis: MessageSemanticAnalysis? = null,
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
    val semanticAnalysis: MessageSemanticAnalysis? = null,
    val isSufficient: Boolean = true,
    val confidence: Float = 1.0f,
    val hasConflict: Boolean = false,
    val conflictDescription: String? = null
)

data class PipelineStageProgress(
    val stageName: String,
    val progress: Float,
    val currentStep: Int = 1,
    val totalSteps: Int = 10,
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
