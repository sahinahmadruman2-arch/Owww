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
    TEACH("Teach"),
    KNOWLEDGE("Knowledge"),
    LIBRARY("Library"),
    ANALYSIS("Analysis"),
    TRAINING("Training")
}

data class DashboardStats(
    val documents: Int = 0,
    val chunks: Int = 0,
    val concepts: Int = 0,
    val facts: Int = 0,
    val questions: Int = 0,
    val relationships: Int = 0
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

    val dashboardStats = combine(
        listOf(
            repository.documentCount,
            repository.chunkCount,
            repository.conceptCount,
            repository.factCount,
            repository.questionCount,
            repository.relationshipCount
        )
    ) { counts ->
        DashboardStats(
            documents = counts[0],
            chunks = counts[1],
            concepts = counts[2],
            facts = counts[3],
            questions = counts[4],
            relationships = counts[5]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Chat UI state
    var chatInput = MutableStateFlow("")
        private set
    var isChatLoading = MutableStateFlow(false)
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

        viewModelScope.launch {
            try {
                repository.askQuestion(query)
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

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
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
