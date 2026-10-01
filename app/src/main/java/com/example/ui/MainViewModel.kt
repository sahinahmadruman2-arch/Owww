package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.UrBotsDatabase
import com.example.data.local.entity.*
import com.example.data.repository.UrBotsRepository
import com.example.domain.model.PipelineStageProgress
import com.example.domain.model.VerificationTestItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UrBotsTab(val title: String) {
    CHAT("Chat"),
    KNOWLEDGE("Knowledge"),
    TEACH("Teach"),
    ANALYSIS("Analyze"),
    LIBRARY("Library"),
    TRAINING("Training"),
    SETTINGS("Settings")
}

data class DashboardStats(
    val documents: Int = 0,
    val chunks: Int = 0,
    val concepts: Int = 0,
    val facts: Int = 0,
    val questions: Int = 0,
    val relationships: Int = 0,
    val manualTraining: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = UrBotsDatabase.getDatabase(application)
    private val repository = UrBotsRepository(database.knowledgeDao())

    private val _currentTab = MutableStateFlow(UrBotsTab.CHAT)
    val currentTab: StateFlow<UrBotsTab> = _currentTab.asStateFlow()

    val documents = repository.allDocuments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val concepts = repository.allConcepts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val facts = repository.allFacts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val relationships = repository.allRelationships.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val trainingRuns = repository.allTrainingRuns.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val versions = repository.allVersions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val messages = repository.getConversationMessages().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val manualTrainingEntries = repository.allManualTrainingEntries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val dashboardStats = combine(
        listOf(
            repository.documentCount,
            repository.chunkCount,
            repository.conceptCount,
            repository.factCount,
            repository.questionCount,
            repository.relationshipCount,
            repository.manualTrainingCount
        )
    ) { counts ->
        DashboardStats(
            documents = counts[0],
            chunks = counts[1],
            concepts = counts[2],
            facts = counts[3],
            questions = counts[4],
            relationships = counts[5],
            manualTraining = counts[6]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Chat UI state
    var chatInput = MutableStateFlow("")
        private set
    var isChatLoading = MutableStateFlow(false)
        private set
    var chatLoadingStage = MutableStateFlow("UrBots7 is thinking...")
        private set

    // Knowledge Input UI state
    var docTitleInput = MutableStateFlow("")
        private set
    var docContentInput = MutableStateFlow("")
        private set
    var isAnalyzing = MutableStateFlow(false)
        private set
    var pipelineProgress = MutableStateFlow<PipelineStageProgress?>(null)
        private set

    // Teach UI state
    var teachQuestionInput = MutableStateFlow("")
        private set
    var teachAnswerInput = MutableStateFlow("")
        private set
    var isTeaching = MutableStateFlow(false)
        private set
    var teachSuccess = MutableStateFlow<String?>(null)
        private set

    // Batch Manual Training UI state
    var manualTrainingText = MutableStateFlow("")
        private set
    var isBatchTraining = MutableStateFlow(false)
        private set
    var batchProgressStatus = MutableStateFlow<String?>(null)
        private set
    var batchProgressFraction = MutableStateFlow(0f)
        private set
    var manualTrainingReport = MutableStateFlow<com.example.domain.model.ManualTrainingVerificationReport?>(null)
        private set

    // Verification testing UI state
    var verificationItems = MutableStateFlow<List<VerificationTestItem>>(emptyList())
        private set
    var isVerifying = MutableStateFlow(false)
        private set

    init {
        // Auto-seed with "Basic Conversation" book on first launch if empty
        viewModelScope.launch {
            val docs = database.knowledgeDao().getAllDocuments().first()
            if (docs.isEmpty()) {
                repository.preloadSampleBook()
            }
        }
    }

    fun setTab(tab: UrBotsTab) {
        _currentTab.value = tab
    }

    fun updateChatInput(text: String) {
        chatInput.value = text
    }

    fun sendChatMessage() {
        val query = chatInput.value.trim()
        if (query.isEmpty() || isChatLoading.value) return
        chatInput.value = ""
        isChatLoading.value = true
        chatLoadingStage.value = "Understanding intent & context..."

        viewModelScope.launch {
            try {
                chatLoadingStage.value = "Reasoning & generating response..."
                repository.askQuestion(query)
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun regenerateLastMessage() {
        if (isChatLoading.value) return
        isChatLoading.value = true
        chatLoadingStage.value = "Regenerating response..."
        viewModelScope.launch {
            try {
                repository.regenerateLastMessage()
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun askPrompt(prompt: String) {
        chatInput.value = prompt
        sendChatMessage()
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearConversation()
        }
    }

    fun updateDocTitle(title: String) {
        docTitleInput.value = title
    }

    fun updateDocContent(content: String) {
        docContentInput.value = content
    }

    fun loadSampleBookIntoForm() {
        docTitleInput.value = "Basic Conversation"
        docContentInput.value = """
            Hi and hello are common greetings.
            
            Hi is generally casual and friendly.
            
            Hello can be used in both casual and more polite situations.
            
            When someone says 'Hi', a person can reply with 'Hi', 'Hello', or another friendly greeting.
            
            When meeting someone for the first time, people may also say 'Nice to meet you.'
        """.trimIndent()
    }

    fun processKnowledgeDocument() {
        val title = docTitleInput.value.trim()
        val content = docContentInput.value.trim()
        if (title.isEmpty() || content.isEmpty() || isAnalyzing.value) return

        isAnalyzing.value = true
        viewModelScope.launch {
            try {
                repository.processDocument(title, content) { progress ->
                    pipelineProgress.value = progress
                }
                docTitleInput.value = ""
                docContentInput.value = ""
            } finally {
                isAnalyzing.value = false
            }
        }
    }

    fun updateTeachQuestion(q: String) {
        teachQuestionInput.value = q
    }

    fun updateTeachAnswer(a: String) {
        teachAnswerInput.value = a
    }

    fun submitManualTeach() {
        val q = teachQuestionInput.value.trim()
        val a = teachAnswerInput.value.trim()
        if (q.isEmpty() || a.isEmpty() || isTeaching.value) return

        isTeaching.value = true
        viewModelScope.launch {
            try {
                repository.manualTeach(q, a)
                teachSuccess.value = "Successfully taught: UrBots7 generated semantic variations and indexed this knowledge into the graph."
                teachQuestionInput.value = ""
                teachAnswerInput.value = ""
            } finally {
                isTeaching.value = false
            }
        }
    }

    fun dismissTeachSuccess() {
        teachSuccess.value = null
    }

    fun updateManualTrainingText(text: String) {
        manualTrainingText.value = text
    }

    fun loadSampleBatchTraining() {
        manualTrainingText.value = """
            Question: How are you?
            Answer: I'm doing well, thanks! How are you?

            Question: What does "I'm exhausted" mean?
            Answer: It means someone is extremely tired.

            Question: How can I ask for help?
            Answer: You can say, "Could you give me a hand?"

            Question: What does "I'm full" mean?
            Answer: It means the person has eaten enough and does not want to eat more.
        """.trimIndent()
    }

    fun submitManualTrainingBatch(sessionTitle: String = "Manual Training Session") {
        val text = manualTrainingText.value.trim()
        if (text.isEmpty() || isBatchTraining.value) return

        isBatchTraining.value = true
        batchProgressFraction.value = 0f
        batchProgressStatus.value = "Scanning & pairing Question/Answer entries..."
        manualTrainingReport.value = null

        viewModelScope.launch {
            try {
                val report = repository.processManualTraining(
                    text = text,
                    sessionTitle = sessionTitle,
                    onProgress = { current, total, stage ->
                        batchProgressStatus.value = stage
                        batchProgressFraction.value = if (total > 0) current.toFloat() / total.toFloat() else 0f
                    }
                )
                manualTrainingReport.value = report
                teachSuccess.value = "Manual training complete: ${report.entriesSavedToPersistentStorage} Q&A pairs saved to persistent storage."
                manualTrainingText.value = ""
            } finally {
                isBatchTraining.value = false
                batchProgressStatus.value = null
            }
        }
    }

    fun dismissManualTrainingReport() {
        manualTrainingReport.value = null
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    fun deleteManualTrainingEntry(id: String) {
        viewModelScope.launch {
            repository.deleteManualTrainingEntry(id)
        }
    }

    fun runVerificationSuite() {
        if (isVerifying.value) return
        isVerifying.value = true
        viewModelScope.launch {
            try {
                val results = repository.runVerificationSuite()
                verificationItems.value = results
            } finally {
                isVerifying.value = false
            }
        }
    }
}
