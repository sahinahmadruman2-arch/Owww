package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.UrBotsDatabase
import com.example.data.repository.UrBotsRepository
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
class ExampleRobolectricTest {

    private lateinit var database: UrBotsDatabase
    private lateinit var repository: UrBotsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UrBotsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UrBotsRepository(database.knowledgeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("UrBots7", appName)
    }

    @Test
    fun testBasicConversationMiniBookCUJs() = runBlocking {
        // Step 1: Preload sample book "Basic Conversation"
        val doc = repository.preloadSampleBook()
        assertNotNull(doc)
        assertEquals("Basic Conversation", doc.title)

        // Step 2: Run verification test suite for all 10 queries
        val verificationResults = repository.runVerificationSuite()
        assertEquals(10, verificationResults.size)

        for (item in verificationResults) {
            assertTrue("Query failed: '${item.query}', Answer: '${item.actualAnswer}'", item.isPassed == true)
            if (item.id in listOf(5, 6, 7, 10)) {
                assertTrue("Knowledge query citations should be grounded: '${item.query}'", item.sources.isNotEmpty())
            }
        }

        // Step 3: Test Insufficient Knowledge Handling
        val unrelatedResult = repository.askQuestion("What is quantum entanglement in computing?", "test_session")
        assertFalse(unrelatedResult.isSufficient)
        assertTrue(unrelatedResult.answerText.contains("does not contain enough information"))

        // Step 4: Test Manual Teaching
        val taught = repository.manualTeach(
            "How do I say goodbye?",
            "You can say 'Goodbye', 'See you later', or 'Have a nice day!'"
        )
        assertNotNull(taught)

        // Ask with paraphrased question
        val retrievedTaught = repository.askQuestion("What can I say to say goodbye to someone?", "test_session")
        assertTrue(retrievedTaught.isSufficient)
        assertTrue(retrievedTaught.answerText.contains("Goodbye") || retrievedTaught.answerText.contains("Have a nice day"))
    }
}
