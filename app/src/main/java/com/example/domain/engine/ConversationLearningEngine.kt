package com.example.domain.engine

import com.example.data.local.dao.SystemLearningDao
import com.example.data.local.entity.*
import com.example.domain.model.MessageSemanticAnalysis
import java.util.Locale
import java.util.UUID

data class ContextResolutionResult(
    val resolvedQuery: String,
    val referencedEntity: String?,
    val activeProject: String?,
    val activeTopic: String?,
    val isAmbiguous: Boolean = false,
    val clarificationQuestion: String? = null
)

data class ExtractedLearningInsights(
    val memories: List<UserMemoryEntity>,
    val skillUpdates: List<UserSkillProfileEntity>,
    val learningCandidates: List<LearningCandidateEntity>,
    val summary: String? = null
)

object ConversationLearningEngine {

    /**
     * Resolves references across the conversation context:
     * - "it", "that", "they", "the previous one", "the same project"
     * - "teach me the next part" -> connects to active learning topic (e.g., Lua)
     * - "make the enemies stronger at night" -> connects to active game project (day and night system)
     */
    fun resolveContextAndCoreference(
        rawQuery: String,
        recentMessages: List<com.example.data.local.entity.ConversationMessageEntity>,
        activeMemories: List<UserMemoryEntity> = emptyList()
    ): ContextResolutionResult {
        val qLower = rawQuery.lowercase(Locale.ROOT).trim()

        // 1. Identify existing projects and topics from recent conversation history and memories
        var activeProject: String? = activeMemories.find { it.category == MemoryCategories.ACTIVE_PROJECTS }?.value
        var activeTopic: String? = activeMemories.find { it.category == MemoryCategories.USER_SKILLS }?.key

        // Also scan recent turns for project & topic mentions
        val userHistory = recentMessages.filter { it.role == "user" }.map { it.message }
        val allHistoryText = userHistory.joinToString("\n").lowercase(Locale.ROOT)

        if (allHistoryText.contains("learning lua") || allHistoryText.contains("learn lua") || allHistoryText.contains("lua")) {
            activeTopic = "Lua"
        } else if (allHistoryText.contains("python")) {
            activeTopic = "Python"
        } else if (allHistoryText.contains("roblox studio") || allHistoryText.contains("roblox")) {
            activeTopic = "Roblox"
        }

        if (allHistoryText.contains("building a game") || allHistoryText.contains("my game") || allHistoryText.contains("day and night")) {
            activeProject = "Game with Day/Night system"
        }

        // Check for specific coreference patterns:
        // Case A: "Teach me the next part" / "What is the next part"
        if (qLower.contains("next part") || qLower.contains("the next step") || qLower.contains("what's next") || qLower.contains("whats next")) {
            val topic = activeTopic ?: "the topic you were discussing"
            val resolved = "Teach me the next part of $topic (advancing from fundamentals like variables to functions, control flow, and practical examples)."
            return ContextResolutionResult(
                resolvedQuery = resolved,
                referencedEntity = topic,
                activeProject = activeProject,
                activeTopic = topic
            )
        }

        // Case B: Game project coreference e.g. "How do I make the enemies stronger at night?"
        if ((qLower.contains("enemies") || qLower.contains("monsters") || qLower.contains("spawning") || qLower.contains("difficulty")) &&
            (qLower.contains("at night") || qLower.contains("in the night"))
        ) {
            val projectContext = activeProject ?: "your game's day and night system"
            val resolved = "$rawQuery (In the context of $projectContext, scaling enemy difficulty, health, and damage based on the night cycle)."
            return ContextResolutionResult(
                resolvedQuery = resolved,
                referencedEntity = "Day and Night Enemy Scaling",
                activeProject = projectContext,
                activeTopic = activeTopic
            )
        }

        // Case C: Pronoun coreferences ("it", "that", "the previous one", "the same project")
        val pronounRegex = Regex("\\b(it|that|they|the previous one|the same project|the last one)\\b")
        if (pronounRegex.containsMatchIn(qLower)) {
            val lastAssistantMsg = recentMessages.lastOrNull { it.role == "assistant" }?.message
            val lastUserMsg = recentMessages.filter { it.role == "user" }.dropLast(1).lastOrNull()?.message

            val antecedent = lastUserMsg ?: lastAssistantMsg ?: activeProject ?: activeTopic

            if (antecedent != null) {
                val shortAntecedent = antecedent.take(80).replace("\n", " ").trim()
                val resolved = "$rawQuery [Referring to: $shortAntecedent]"
                return ContextResolutionResult(
                    resolvedQuery = resolved,
                    referencedEntity = shortAntecedent,
                    activeProject = activeProject,
                    activeTopic = activeTopic
                )
            }
        }

        // Ambiguity check: user asks "explain that" or "do that again" with empty context
        if ((qLower == "explain that" || qLower == "why?" || qLower == "how do i do it?" || qLower == "what about that?") &&
            recentMessages.isEmpty() && activeMemories.isEmpty()
        ) {
            return ContextResolutionResult(
                resolvedQuery = rawQuery,
                referencedEntity = null,
                activeProject = null,
                activeTopic = null,
                isAmbiguous = true,
                clarificationQuestion = "Could you clarify what you are referring to?"
            )
        }

        return ContextResolutionResult(
            resolvedQuery = rawQuery,
            referencedEntity = null,
            activeProject = activeProject,
            activeTopic = activeTopic
        )
    }

    /**
     * Extracts useful long-term memories, user skill signals, active projects,
     * preferences, and corrections from user messages.
     * Separates temporary conversation context from permanent long-term memory.
     */
    fun extractInsights(
        userMessage: String,
        sessionId: String,
        messageId: String,
        userId: String = "user_default"
    ): ExtractedLearningInsights {
        val qLower = userMessage.lowercase(Locale.ROOT).trim()
        val memories = mutableListOf<UserMemoryEntity>()
        val skillUpdates = mutableListOf<UserSkillProfileEntity>()
        val learningCandidates = mutableListOf<LearningCandidateEntity>()

        // 1. User Skills & Knowledge Level (e.g. "I am learning Lua", "I know variables but not functions")
        if (qLower.contains("i am learning") || qLower.contains("im learning") || qLower.contains("i'm learning")) {
            val topic = extractSubjectAfter(qLower, listOf("learning", "studying"))
            if (topic.isNotBlank()) {
                val profile = UserSkillProfileEntity(
                    userId = userId,
                    topic = topic.replaceFirstChar { it.uppercase() },
                    level = "BEGINNER",
                    confidence = 0.85f,
                    topicsUnderstoodJson = "[]",
                    topicsNeedingHelpJson = "[]",
                    preferredExplanationStyle = "STEP_BY_STEP",
                    lastEvidence = userMessage
                )
                skillUpdates.add(profile)

                memories.add(
                    UserMemoryEntity(
                        userId = userId,
                        category = MemoryCategories.USER_SKILLS,
                        key = topic.replaceFirstChar { it.uppercase() },
                        value = "User is learning $topic at beginner level",
                        confidence = 0.9f,
                        sourceSessionId = sessionId,
                        sourceMessageId = messageId
                    )
                )
            }
        }

        // Evidence of partial knowledge (e.g. "I know variables but not functions")
        if (qLower.contains("i know") && qLower.contains("but not")) {
            val known = qLower.substringAfter("i know").substringBefore("but not").trim()
            val unknown = qLower.substringAfter("but not").replace(Regex("[^a-zA-Z0-9 ]"), "").trim()

            memories.add(
                UserMemoryEntity(
                    userId = userId,
                    category = MemoryCategories.USER_SKILLS,
                    key = "Skill Knowledge Boundary",
                    value = "Understands: $known | Needs assistance with: $unknown",
                    confidence = 0.95f,
                    sourceSessionId = sessionId,
                    sourceMessageId = messageId
                )
            )

            skillUpdates.add(
                UserSkillProfileEntity(
                    userId = userId,
                    topic = "Programming Concepts",
                    level = "BEGINNER",
                    confidence = 0.85f,
                    topicsUnderstoodJson = "[\"$known\"]",
                    topicsNeedingHelpJson = "[\"$unknown\"]",
                    preferredExplanationStyle = "STEP_BY_STEP",
                    lastEvidence = userMessage
                )
            )
        }

        // 2. Active Projects (e.g. "I am building a game", "working on a website")
        if (qLower.contains("i am building") || qLower.contains("im building") || qLower.contains("i'm building") ||
            qLower.contains("working on a") || qLower.contains("my project is") || qLower.contains("developing a")
        ) {
            val projectDesc = userMessage.trim()
            memories.add(
                UserMemoryEntity(
                    userId = userId,
                    category = MemoryCategories.ACTIVE_PROJECTS,
                    key = "Active Project",
                    value = projectDesc,
                    confidence = 0.95f,
                    sourceSessionId = sessionId,
                    sourceMessageId = messageId
                )
            )
        }

        // 3. User Preferences (e.g. "I prefer concise answers", "give me code only", "explain step by step")
        if (qLower.contains("i prefer") || qLower.contains("please always") || qLower.contains("keep your answers") ||
            qLower.contains("explain simply") || qLower.contains("step by step")
        ) {
            val prefValue = userMessage.trim()
            memories.add(
                UserMemoryEntity(
                    userId = userId,
                    category = MemoryCategories.USER_PREFERENCES,
                    key = "Communication Style",
                    value = prefValue,
                    confidence = 0.95f,
                    sourceSessionId = sessionId,
                    sourceMessageId = messageId
                )
            )
        }

        // 4. Corrections and Feedback (e.g. "Actually, the answer is...", "That's incorrect, you should...")
        if (qLower.startsWith("actually") || qLower.contains("that is incorrect") || qLower.contains("thats incorrect") ||
            qLower.contains("you made a mistake") || qLower.contains("that's wrong")
        ) {
            memories.add(
                UserMemoryEntity(
                    userId = userId,
                    category = MemoryCategories.CORRECTIONS_AND_FEEDBACK,
                    key = "User Correction",
                    value = userMessage.trim(),
                    confidence = 0.9f,
                    sourceSessionId = sessionId,
                    sourceMessageId = messageId
                )
            )

            learningCandidates.add(
                LearningCandidateEntity(
                    userId = userId,
                    title = "User Fact Correction",
                    extractedContent = userMessage.trim(),
                    category = MemoryCategories.CORRECTIONS_AND_FEEDBACK,
                    sourceType = "USER_CORRECTION",
                    sourceOrigin = "Chat Session $sessionId",
                    status = "PENDING_REVIEW",
                    isSharedKnowledge = false,
                    reliabilityScore = 0.85f
                )
            )
        }

        // 5. Explicit Personal Facts (e.g. "My name is John", "I live in Chicago", "I have a cat named Milo")
        val isFact = qLower.contains("my name is") || qLower.contains("i live in") ||
                qLower.contains("i work as") || qLower.contains("my favorite") ||
                qLower.contains("i am allergic to")

        if (isFact) {
            val key = when {
                qLower.contains("my name is") -> "User Name"
                qLower.contains("i live in") -> "Location"
                qLower.contains("i work as") -> "Occupation"
                else -> "Personal Fact"
            }
            val isSensitive = qLower.contains("allergic") || qLower.contains("password") || qLower.contains("ssn")

            memories.add(
                UserMemoryEntity(
                    userId = userId,
                    category = MemoryCategories.IMPORTANT_FACTS,
                    key = key,
                    value = userMessage.trim(),
                    confidence = 0.98f,
                    sourceSessionId = sessionId,
                    sourceMessageId = messageId,
                    isSensitive = isSensitive,
                    isApproved = !isSensitive // Sensitive items require review
                )
            )
        }

        return ExtractedLearningInsights(
            memories = memories,
            skillUpdates = skillUpdates,
            learningCandidates = learningCandidates
        )
    }

    private fun extractSubjectAfter(text: String, keywords: List<String>): String {
        for (kw in keywords) {
            if (text.contains(kw)) {
                val after = text.substringAfter(kw).trim()
                val cleaned = after.split(Regex("[.,;!?]")).firstOrNull()?.trim() ?: after
                val words = cleaned.split(" ").filter { it.isNotBlank() }
                if (words.isNotEmpty()) {
                    return words.take(3).joinToString(" ")
                }
            }
        }
        return ""
    }
}
