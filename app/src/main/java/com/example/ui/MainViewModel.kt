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
    FEEDS("Live Web"),
    TEACH("Teach"),
    KNOWLEDGE("Knowledge"),
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
    val manualTraining: Int = 0,
    val liveFeeds: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = UrBotsDatabase.getDatabase(application)
    val learningDao = database.systemLearningDao()
    private val repository = UrBotsRepository(database.knowledgeDao(), learningDao)

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

    // Session & Chat UI state
    val currentSessionId = MutableStateFlow("default_session")
    val allSessions = repository.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("default_session")
    )

    val messages = currentSessionId.flatMapLatest { sessionId ->
        repository.getConversationMessages(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val manualTrainingEntries = repository.allManualTrainingEntries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val liveFeeds = repository.allLiveFeeds.stateIn(
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
            repository.manualTrainingCount,
            repository.liveFeedCount
        )
    ) { counts ->
        DashboardStats(
            documents = counts[0],
            chunks = counts[1],
            concepts = counts[2],
            facts = counts[3],
            questions = counts[4],
            relationships = counts[5],
            manualTraining = counts[6],
            liveFeeds = counts[7]
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Live Feeds UI state
    var liveFeedSearchInput = MutableStateFlow("")
        private set
    var isLiveSearching = MutableStateFlow(false)
        private set
    var liveSearchStatus = MutableStateFlow<String?>(null)
        private set

    // Chat UI state
    var chatInput = MutableStateFlow("")
        private set
    var isChatLoading = MutableStateFlow(false)
        private set
    var chatLoadingStage = MutableStateFlow("Karllo is thinking...")
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
        chatLoadingStage.value = "Understanding request & intent..."

        viewModelScope.launch {
            try {
                chatLoadingStage.value = "Deciding tools & verifying answer..."
                repository.askQuestion(query, sessionId = currentSessionId.value)
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun startNewChat(titleSuffix: String = "") {
        val newId = if (titleSuffix.isBlank()) {
            "chat_" + System.currentTimeMillis()
        } else {
            "chat_" + titleSuffix.lowercase().replace(" ", "_") + "_" + (System.currentTimeMillis() % 10000)
        }
        currentSessionId.value = newId
        chatInput.value = ""
    }

    fun switchChatSession(sessionId: String) {
        currentSessionId.value = sessionId
    }

    fun regenerateLastMessage() {
        if (isChatLoading.value) return
        isChatLoading.value = true
        chatLoadingStage.value = "Regenerating & verifying..."
        viewModelScope.launch {
            try {
                repository.regenerateLastMessage(sessionId = currentSessionId.value)
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
            repository.clearConversation(sessionId = currentSessionId.value)
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

    fun updateLiveFeedSearch(query: String) {
        liveFeedSearchInput.value = query
    }

    fun searchLiveWeb(query: String = liveFeedSearchInput.value, subreddit: String? = null) {
        val q = query.trim()
        if (q.isBlank() || isLiveSearching.value) return
        isLiveSearching.value = true
        liveSearchStatus.value = "Fetching live Reddit discussions & YouTube videos..."
        viewModelScope.launch {
            try {
                repository.searchAndHarvestLiveWeb(q, subreddit = subreddit, autoIngest = true)
                liveSearchStatus.value = "Autonomous Knowledge Sync Complete: Ingested to Database!"
            } finally {
                isLiveSearching.value = false
            }
        }
    }

    fun harvestTrendingTopic(topic: String, subreddit: String? = null) {
        liveFeedSearchInput.value = topic
        searchLiveWeb(topic, subreddit)
    }

    fun deleteLiveFeed(id: String) {
        viewModelScope.launch {
            repository.deleteLiveFeed(id)
        }
    }

    fun clearLiveFeeds() {
        viewModelScope.launch {
            repository.clearLiveFeeds()
        }
    }

    // Auto Learning System Flows
    val learningSettings = learningDao.getSettingsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val userMemories = learningDao.getUserMemoriesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val userSkillProfiles = learningDao.getUserSkillProfilesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val learningCandidates = learningDao.getAllLearningCandidatesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sharedKnowledge = learningDao.getAllSharedKnowledgeFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val knowledgeSources = learningDao.getAllKnowledgeSourcesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val learningTasks = learningDao.getAllLearningTasksFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val learningEvents = learningDao.getRecentLearningEventsFlow(100).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val responseEvaluations = learningDao.getResponseEvaluationsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val diagnosticReports = learningDao.getDiagnosticReportsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val proposedPatches = learningDao.getProposedPatchesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val auditLogs = learningDao.getAuditLogsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val queuedTasksCount = learningDao.getQueuedTaskCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val runningTasksCount = learningDao.getRunningTaskCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val completedTasksCount = learningDao.getCompletedTaskCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val failedTasksCount = learningDao.getFailedTaskCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val pendingCandidatesCount = learningDao.getPendingCandidateCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val sharedKnowledgeCount = learningDao.getSharedKnowledgeCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )
    val userMemoriesCount = learningDao.getUserMemoryCountFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    fun toggleAutoLearning(enabled: Boolean) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.toggleAutoLearning(learningDao, enabled)
        }
    }

    fun toggleWebLearning(enabled: Boolean) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.toggleWebLearning(learningDao, enabled)
        }
    }

    fun toggleMemoryExtraction(enabled: Boolean) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.toggleMemoryExtraction(learningDao, enabled)
        }
    }

    fun deleteUserMemory(id: String) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.deleteMemory(id, learningDao)
        }
    }

    fun editUserMemory(memory: UserMemoryEntity) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.editMemory(memory, learningDao)
        }
    }

    fun addUserMemory(category: String, key: String, value: String) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.addMemory(category, key, value, learningDao)
        }
    }

    fun deleteSkillProfile(id: String) {
        viewModelScope.launch {
            learningDao.deleteUserSkillProfileById(id)
        }
    }

    fun reviewCandidate(candidateId: String, approve: Boolean) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.reviewCandidate(candidateId, approve, learningDao)
        }
    }

    fun deleteCandidate(id: String) {
        viewModelScope.launch {
            learningDao.deleteLearningCandidateById(id)
        }
    }

    fun deleteSharedKnowledge(id: String) {
        viewModelScope.launch {
            learningDao.deleteSharedKnowledgeById(id)
        }
    }

    fun addSharedKnowledge(title: String, summary: String, category: String = "General Knowledge") {
        viewModelScope.launch {
            learningDao.insertSharedKnowledge(
                SharedKnowledgeEntity(
                    title = title,
                    summary = summary,
                    category = category,
                    verificationStatus = "VERIFIED",
                    isShared = true
                )
            )
        }
    }

    fun enqueueTask(taskType: String, title: String, details: String) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.enqueueTask(
                taskType = taskType,
                title = title,
                details = details,
                learningDao = learningDao
            )
        }
    }

    fun runNextQueuedTask() {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.executeNextTask(
                context = getApplication(),
                knowledgeDao = database.knowledgeDao(),
                learningDao = learningDao
            )
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.runSelfDiagnostics(learningDao)
        }
    }

    fun proposePatch(title: String, targetFile: String, diff: String, description: String) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.proposeSafeCodePatch(
                title = title,
                targetFile = targetFile,
                patchDiff = diff,
                description = description,
                learningDao = learningDao
            )
        }
    }

    fun reviewPatch(patchId: String, approve: Boolean) {
        viewModelScope.launch {
            com.example.domain.engine.SystemLearningManager.reviewPatch(patchId, approve, learningDao)
        }
    }

    fun clearLearningEvents() {
        viewModelScope.launch {
            learningDao.clearLearningEvents()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            learningDao.clearUserMemories()
        }
    }
}
