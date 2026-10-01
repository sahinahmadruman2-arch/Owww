package com.example.domain.model

data class ManualQaPair(
    val index: Int,
    val question: String,
    val answer: String
)

data class ManualTrainingParseResult(
    val totalQuestionsDetected: Int,
    val totalAnswersDetected: Int,
    val pairedEntries: List<ManualQaPair>,
    val incompleteEntriesCount: Int,
    val invalidEntriesCount: Int,
    val incompleteDetails: List<String> = emptyList()
)

data class ManualTrainingVerificationReport(
    val totalQuestionsDetected: Int,
    val totalAnswersDetected: Int,
    val successfullyPairedEntries: Int,
    val incompleteOrInvalidEntries: Int,
    val entriesSavedToPersistentStorage: Int,
    val entriesNeedingProcessing: Int,
    val documentTitle: String,
    val isSuccess: Boolean = true
) {
    fun toFormattedReport(): String {
        return buildString {
            appendLine("📊 Training Verification Report")
            appendLine("• Total Question entries detected: $totalQuestionsDetected")
            appendLine("• Total Answer entries detected: $totalAnswersDetected")
            appendLine("• Successfully paired entries: $successfullyPairedEntries")
            appendLine("• Incomplete or invalid entries: $incompleteOrInvalidEntries")
            appendLine("• Entries successfully saved to persistent storage: $entriesSavedToPersistentStorage")
            appendLine("• Any entries that still need processing: $entriesNeedingProcessing")
            if (entriesSavedToPersistentStorage > 0) {
                appendLine("\nUrBots7 has indexed all $entriesSavedToPersistentStorage entries into persistent memory with semantic variations.")
            }
        }
    }
}
