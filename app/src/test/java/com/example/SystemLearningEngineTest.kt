package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.UrBotsDatabase
import com.example.data.local.entity.MemoryCategories
import com.example.domain.engine.SystemLearningManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemLearningEngineTest {

    private lateinit var database: UrBotsDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UrBotsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testConversationLearningSkillsAndProjects() = runBlocking {
        val learningDao = database.systemLearningDao()

        // 1. User shares they are learning Lua
        SystemLearningManager.processConversationLearning(
            userMessage = "I am learning Lua. I know variables but not functions.",
            assistantAnswer = "Lua functions are defined using the 'function' keyword.",
            sessionId = "session_lua",
            recentMessages = emptyList(),
            learningDao = learningDao
        )

        val skillProfiles = learningDao.getUserSkillProfiles()
        assertTrue(skillProfiles.isNotEmpty())
        val luaSkill = skillProfiles.firstOrNull { it.topic.contains("Lua") }
        assertNotNull(luaSkill)
        assertEquals("BEGINNER", luaSkill?.level)
        assertTrue(luaSkill?.topicsNeedingHelpJson?.contains("functions") == true)

        // 2. User shares active project
        SystemLearningManager.processConversationLearning(
            userMessage = "I am building a game with a day and night cycle.",
            assistantAnswer = "A day and night cycle can use a tween or timer.",
            sessionId = "session_game",
            recentMessages = emptyList(),
            learningDao = learningDao
        )

        val memories = learningDao.getUserMemories()
        val projectMem = memories.firstOrNull { it.category == MemoryCategories.ACTIVE_PROJECTS }
        assertNotNull(projectMem)
        assertTrue(projectMem?.value?.contains("day and night") == true)

        // 3. User correction
        SystemLearningManager.processConversationLearning(
            userMessage = "No, actually that is wrong, the cycle is 24 minutes not 24 seconds.",
            assistantAnswer = "Got it, I updated the cycle length to 24 minutes.",
            sessionId = "session_game",
            recentMessages = emptyList(),
            learningDao = learningDao
        )

        val correctionMem = learningDao.getUserMemoriesByCategory("user_default", MemoryCategories.CORRECTIONS_AND_FEEDBACK)
        assertTrue(correctionMem.isNotEmpty())

        // 4. Verify learning events logged
        val events = learningDao.getRecentLearningEvents()
        assertTrue(events.any { it.eventType == "SKILL_UPDATED" })
        assertTrue(events.any { it.eventType == "CONVERSATION_ANALYSIS" })
    }

    @Test
    fun testBackgroundTasksAndSelfDiagnostics() = runBlocking {
        val learningDao = database.systemLearningDao()

        // Enqueue background task
        val task = SystemLearningManager.enqueueTask(
            taskType = "DUPLICATE_DETECTION",
            title = "Scan Duplicates",
            details = "Deduplicate facts",
            learningDao = learningDao
        )
        assertNotNull(task)
        assertEquals("QUEUED", task.status)

        // Execute task
        SystemLearningManager.executeNextTask(
            context = null,
            knowledgeDao = database.knowledgeDao(),
            learningDao = learningDao
        )

        val updatedTask = learningDao.getTaskById(task.id)
        assertEquals("COMPLETED", updatedTask?.status)

        // Run self diagnostics
        SystemLearningManager.runSelfDiagnostics(learningDao)
        val reports = learningDao.getRecentLearningEvents()
        assertTrue(reports.any { it.eventType == "TEST_EXECUTED" })

        // Propose safe patch
        SystemLearningManager.proposeSafeCodePatch(
            title = "Fix Memory Cache",
            targetFile = "SystemLearningManager.kt",
            patchDiff = "+ cache.clear()",
            description = "Clear cache on sync",
            learningDao = learningDao
        )

        val patches = learningDao.getRecentLearningEvents()
        assertTrue(patches.any { it.eventType == "PATCH_PROPOSED" })
        assertTrue(patches.any { it.eventType == "OWNER_APPROVAL_REQUESTED" })
    }

    @Test
    fun testControlsAndSettingsToggle() = runBlocking {
        val learningDao = database.systemLearningDao()

        SystemLearningManager.toggleAutoLearning(learningDao, false)
        var settings = SystemLearningManager.getSettings(learningDao)
        assertFalse(settings.isAutoLearningEnabled)

        SystemLearningManager.toggleAutoLearning(learningDao, true)
        settings = SystemLearningManager.getSettings(learningDao)
        assertTrue(settings.isAutoLearningEnabled)
    }
}
