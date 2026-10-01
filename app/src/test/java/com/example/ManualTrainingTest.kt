package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.UrBotsDatabase
import com.example.data.repository.UrBotsRepository
import com.example.domain.engine.ManualTrainingParser
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
class ManualTrainingTest {

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
    fun testParsingSinglePair() {
        val text = """
            Question: What does "I'm full" mean?
            Answer: It means the person has eaten enough and does not want to eat more.
        """.trimIndent()

        val result = ManualTrainingParser.parse(text)
        assertEquals(1, result.totalQuestionsDetected)
        assertEquals(1, result.totalAnswersDetected)
        assertEquals(1, result.pairedEntries.size)
        assertEquals(0, result.incompleteEntriesCount)
        assertEquals(0, result.invalidEntriesCount)

        val pair = result.pairedEntries[0]
        assertEquals("What does \"I'm full\" mean?", pair.question)
        assertEquals("It means the person has eaten enough and does not want to eat more.", pair.answer)
    }

    @Test
    fun testParsingMultipleQuestionsInOneSession() {
        val text = """
            Question: How are you?
            Answer: I'm doing well, thanks! How are you?

            Question: What does "I'm exhausted" mean?
            Answer: It means someone is extremely tired.

            Question: How can I ask for help?
            Answer: You can say, "Could you give me a hand?"
        """.trimIndent()

        val result = ManualTrainingParser.parse(text)
        assertEquals(3, result.totalQuestionsDetected)
        assertEquals(3, result.totalAnswersDetected)
        assertEquals(3, result.pairedEntries.size)
        assertEquals(0, result.incompleteEntriesCount)
        assertEquals(0, result.invalidEntriesCount)

        assertEquals("How are you?", result.pairedEntries[0].question)
        assertEquals("I'm doing well, thanks! How are you?", result.pairedEntries[0].answer)

        assertEquals("What does \"I'm exhausted\" mean?", result.pairedEntries[1].question)
        assertEquals("It means someone is extremely tired.", result.pairedEntries[1].answer)

        assertEquals("How can I ask for help?", result.pairedEntries[2].question)
        assertEquals("You can say, \"Could you give me a hand?\"", result.pairedEntries[2].answer)
    }

    @Test
    fun testParsingLargeBatchOfEntries() {
        val sb = StringBuilder()
        for (i in 1..150) {
            sb.append("Question: Question number $i in training session?\n")
            sb.append("Answer: This is the verified answer for question $i.\n\n")
        }

        val result = ManualTrainingParser.parse(sb.toString())
        assertEquals(150, result.totalQuestionsDetected)
        assertEquals(150, result.totalAnswersDetected)
        assertEquals(150, result.pairedEntries.size)
        assertEquals(0, result.incompleteEntriesCount)
    }

    @Test
    fun testIncompleteAndInvalidParsing() {
        val text = """
            Question: What is an unanswered question?
            Question: What is a valid question?
            Answer: This is the valid answer.

            Answer: This is an answer without a question.

            Question: Final question without answer.
        """.trimIndent()

        val result = ManualTrainingParser.parse(text)
        assertEquals(3, result.totalQuestionsDetected)
        assertEquals(2, result.totalAnswersDetected)
        assertEquals(1, result.pairedEntries.size)
        assertEquals(2, result.incompleteEntriesCount) // two questions had no answer
        assertEquals(1, result.invalidEntriesCount) // one orphan answer

        assertEquals("What is a valid question?", result.pairedEntries[0].question)
        assertEquals("This is the valid answer.", result.pairedEntries[0].answer)
    }

    @Test
    fun testPersistentStorageAndVerificationReport() = runBlocking {
        val trainingText = """
            Question: What does "I'm full" mean?
            Answer: It means the person has eaten enough and does not want to eat more.

            Question: What does "I'm exhausted" mean?
            Answer: It means someone is extremely tired.

            Question: How can I ask for help?
            Answer: You can say, "Could you give me a hand?"
        """.trimIndent()

        val report = repository.processManualTraining(trainingText, "Unit Test Training")

        assertEquals(3, report.totalQuestionsDetected)
        assertEquals(3, report.totalAnswersDetected)
        assertEquals(3, report.successfullyPairedEntries)
        assertEquals(0, report.incompleteOrInvalidEntries)
        assertEquals(3, report.entriesSavedToPersistentStorage)
        assertEquals(0, report.entriesNeedingProcessing)
        assertTrue(report.isSuccess)

        // Verify stored in persistent Room database
        val storedEntries = database.knowledgeDao().getAllManualTrainingEntries()
        assertEquals(3, storedEntries.size)

        // Verify report string formatting contains all 6 required verification items
        val reportString = report.toFormattedReport()
        assertTrue(reportString.contains("Total Question entries detected: 3"))
        assertTrue(reportString.contains("Total Answer entries detected: 3"))
        assertTrue(reportString.contains("Successfully paired entries: 3"))
        assertTrue(reportString.contains("Incomplete or invalid entries: 0"))
        assertTrue(reportString.contains("Entries successfully saved to persistent storage: 3"))
        assertTrue(reportString.contains("Any entries that still need processing: 0"))
    }

    @Test
    fun testLearnedKnowledgeRetrievalAndUse() = runBlocking {
        val trainingText = """
            Question: What does "I'm full" mean?
            Answer: It means the person has eaten enough and does not want to eat more.

            Question: What does "I'm exhausted" mean?
            Answer: It means someone is extremely tired.

            Question: How can I ask for help?
            Answer: You can say, "Could you give me a hand?"
        """.trimIndent()

        repository.processManualTraining(trainingText, "Test Training")

        // 1. Ask what "I'm full" means
        val r1 = repository.askQuestion("What does \"I'm full\" mean?", "session_1")
        assertTrue("Answer was: '${r1.answerText}'", r1.answerText.contains("eaten enough") && r1.answerText.contains("not want to eat more"))

        // 2. Paraphrased query
        val r1Para = repository.askQuestion("Can you explain what I'm full means?", "session_1")
        assertTrue("Answer was: '${r1Para.answerText}'", r1Para.answerText.contains("eaten enough"))

        // 3. Ask what "I'm exhausted" means
        val r2 = repository.askQuestion("What does \"I'm exhausted\" mean?", "session_2")
        assertTrue("Answer was: '${r2.answerText}'", r2.answerText.contains("extremely tired"))

        // 4. Ask how to ask for help
        val r3 = repository.askQuestion("How can I ask for help?", "session_3")
        assertTrue("Answer was: '${r3.answerText}'", r3.answerText.contains("give me a hand"))
    }

    @Test
    fun testFeelingStatementVsMeaningQuestionDistinction() = runBlocking {
        val trainingText = """
            Question: What does "I'm exhausted" mean?
            Answer: It means someone is extremely tired.
        """.trimIndent()

        repository.processManualTraining(trainingText, "Test Training")

        // User is asking for the meaning -> Retrieve knowledge
        val meaningResult = repository.askQuestion("What does \"I'm exhausted\" mean?", "session_q")
        assertTrue(meaningResult.answerText.contains("extremely tired"))

        // User is sharing how they feel -> Respond naturally, DO NOT automatically define the sentence!
        val feelingResult = repository.askQuestion("I can barely keep my eyes open because I'm so tired.", "session_feeling")
        assertFalse(
            "Should not define the sentence when user shares feeling. Answer was: ${feelingResult.answerText}",
            feelingResult.answerText.contains("It means someone is extremely tired")
        )
        assertTrue(
            "Should respond naturally to fatigue. Answer was: ${feelingResult.answerText}",
            feelingResult.answerText.lowercase().contains("rest") || feelingResult.answerText.lowercase().contains("sorry") || feelingResult.answerText.lowercase().contains("sleep")
        )
    }

    @Test
    fun testChatDirectTrainingSubmission() = runBlocking {
        val chatTrainingText = """
            Question: How are you?
            Answer: I'm doing well, thanks! How are you?

            Question: What does "I'm exhausted" mean?
            Answer: It means someone is extremely tired.

            Question: How can I ask for help?
            Answer: You can say, "Could you give me a hand?"
        """.trimIndent()

        // User pastes training text directly in chat
        val chatResult = repository.askQuestion(chatTrainingText, "chat_training_session")

        // Chat response must be the verification report
        assertTrue(chatResult.answerText.contains("Training Verification Report"))
        assertTrue(chatResult.answerText.contains("Successfully paired entries: 3"))
        assertTrue(chatResult.answerText.contains("Entries successfully saved to persistent storage: 3"))

        // Subsequent chat query can immediately retrieve the taught answer
        val followUp = repository.askQuestion("How can I ask for help?", "chat_training_session")
        assertTrue(followUp.answerText.contains("give me a hand"))
    }
}
