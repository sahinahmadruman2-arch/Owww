package com.example.ui.screens.teach

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ManualTrainingParser
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val question by viewModel.teachQuestionInput.collectAsState()
    val answer by viewModel.teachAnswerInput.collectAsState()
    val isTeaching by viewModel.isTeaching.collectAsState()
    val successMsg by viewModel.teachSuccess.collectAsState()

    val manualTrainingText by viewModel.manualTrainingText.collectAsState()
    val isBatchTraining by viewModel.isBatchTraining.collectAsState()
    val batchStatus by viewModel.batchProgressStatus.collectAsState()
    val batchProgress by viewModel.batchProgressFraction.collectAsState()
    val report by viewModel.manualTrainingReport.collectAsState()
    val storedEntries by viewModel.manualTrainingEntries.collectAsState()

    // 0 = Batch Q&A Format (100+ entries), 1 = Single Q&A, 2 = Stored Vault
    var selectedMode by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Real-time syntax detection preview
    val liveParseResult = remember(manualTrainingText) {
        if (manualTrainingText.isBlank()) null
        else ManualTrainingParser.parse(manualTrainingText)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (selectedMode != 2) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-Tech Header Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradient)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ElectricIndigoContainer,
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = ElectricCyanGlow
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Manual Training Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonEmeraldContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PERSISTENT ROOM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmeraldGlow,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Train UrBots7 using standard 'Question:' & 'Answer:' pairing. Ingest 100, 200, 500+ entries with verified persistence and semantic retrieval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Mode Segmented Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    Triple(0, "Batch Q&A (100+)", Icons.Outlined.FormatListBulleted),
                    Triple(1, "Single Q&A", Icons.Outlined.School),
                    Triple(2, "Vault (${storedEntries.size})", Icons.Outlined.Inventory2)
                )

                modes.forEach { (index, title, icon) ->
                    val isSelected = selectedMode == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ElectricIndigo else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedMode = index }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (selectedMode == 0) {
            // --- BATCH Q&A FORMAT MODE ---
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Rule, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Manual Training Format Rules",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "• Question: [question text]\n• Answer: [answer text]\n• ONE QUESTION + ITS MATCHING ANSWER = ONE KNOWLEDGE ENTRY.\n• Paste 10, 100, 200, or 500+ pairs. All entries are preserved and stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Quick Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Training Document Content:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { viewModel.loadSampleBatchTraining() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricCyan)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Sample 4-Pair Batch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Batch Text Input Area
            OutlinedTextField(
                value = manualTrainingText,
                onValueChange = { viewModel.updateManualTrainingText(it) },
                label = { Text("Question & Answer Training Text") },
                placeholder = {
                    Text(
                        "Question: What does \"I'm full\" mean?\n" +
                                "Answer: It means the person has eaten enough and does not want to eat more.\n\n" +
                                "Question: What does \"I'm exhausted\" mean?\n" +
                                "Answer: It means someone is extremely tired.\n\n" +
                                "Question: How can I ask for help?\n" +
                                "Answer: You can say, \"Could you give me a hand?\""
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .testTag("batch_training_input"),
                leadingIcon = {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = ElectricCyan)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Live Syntax Detection HUD
            if (liveParseResult != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradientSubtle)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live HUD:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            StatPill("Questions", liveParseResult.totalQuestionsDetected.toString(), ElectricCyan)
                            StatPill("Answers", liveParseResult.totalAnswersDetected.toString(), ElectricIndigo)
                            StatPill("Pairs", liveParseResult.pairedEntries.size.toString(), NeonEmerald)
                        }

                        if (liveParseResult.incompleteEntriesCount > 0) {
                            StatPill("Incomplete", liveParseResult.incompleteEntriesCount.toString(), AmberWarning)
                        }
                    }
                }
            }

            // Submit Batch Training Button
            Button(
                onClick = { viewModel.submitManualTrainingBatch() },
                enabled = manualTrainingText.isNotBlank() && !isBatchTraining,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_batch_training_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    disabledContainerColor = ObsidianCardBorder
                )
            ) {
                if (isBatchTraining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Processing & Storing in Batches...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process & Store Training Entries", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Progress Bar when Batch Training
            AnimatedVisibility(visible = isBatchTraining) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = batchStatus ?: "Processing...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        LinearProgressIndicator(
                            progress = { batchProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = ElectricCyan,
                            trackColor = ObsidianCardBorder
                        )
                    }
                }
            }

            // Verification Report Card
            AnimatedVisibility(visible = report != null) {
                report?.let { rep ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Brush.linearGradient(EmeraldGlowGradient)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Training Verification Report",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissManualTrainingReport() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary)
                                }
                            }

                            HorizontalDivider(color = ObsidianCardBorder)

                            // Telemetry Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCard("Questions", rep.totalQuestionsDetected.toString(), ElectricCyan, Modifier.weight(1f))
                                MetricCard("Answers", rep.totalAnswersDetected.toString(), ElectricIndigo, Modifier.weight(1f))
                                MetricCard("Paired", rep.successfullyPairedEntries.toString(), NeonEmerald, Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCard("Incomplete", rep.incompleteOrInvalidEntries.toString(), if (rep.incompleteOrInvalidEntries > 0) AmberWarning else TextSecondary, Modifier.weight(1f))
                                MetricCard("Saved to Room", rep.entriesSavedToPersistentStorage.toString(), NeonEmerald, Modifier.weight(1f))
                                MetricCard("Needing Work", rep.entriesNeedingProcessing.toString(), TextSecondary, Modifier.weight(1f))
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NeonEmeraldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "All ${rep.entriesSavedToPersistentStorage} paired entries are verified and stored in Room database for semantic retrieval.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeonEmeraldGlow,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else if (selectedMode == 1) {
            // --- SINGLE Q&A MODE ---
            Text(
                text = "Try Example Knowledge:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.updateTeachQuestion("What does \"I'm full\" mean?")
                        viewModel.updateTeachAnswer("It means the person has eaten enough and does not want to eat more.")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Text("I'm Full Q&A", fontSize = 12.sp, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = {
                        viewModel.updateTeachQuestion("What does \"I'm exhausted\" mean?")
                        viewModel.updateTeachAnswer("It means someone is extremely tired.")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f))
                ) {
                    Text("Exhausted Q&A", fontSize = 12.sp, color = TextPrimary)
                }
            }

            // Question Input
            OutlinedTextField(
                value = question,
                onValueChange = { viewModel.updateTeachQuestion(it) },
                label = { Text("User Question") },
                placeholder = { Text("e.g. What does \"I'm full\" mean?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teach_question_input"),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = ElectricCyan)
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Answer Input
            OutlinedTextField(
                value = answer,
                onValueChange = { viewModel.updateTeachAnswer(it) },
                label = { Text("Taught Answer") },
                placeholder = { Text("e.g. It means the person has eaten enough and does not want to eat more.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("teach_answer_input"),
                leadingIcon = {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = ElectricIndigo)
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = ObsidianCardBorder,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Teach Action Button
            Button(
                onClick = { viewModel.submitManualTeach() },
                enabled = question.isNotBlank() && answer.isNotBlank() && !isTeaching,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_teach_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    disabledContainerColor = ObsidianCardBorder
                )
            ) {
                if (isTeaching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Synthesizing Variations & Indexing...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Teach UrBots7 Single Pair", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // --- TAUGHT KNOWLEDGE VAULT (MODE 2) ---
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search stored Q&A pairs...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ObsidianCardBorder,
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                val filtered = remember(storedEntries, searchQuery) {
                    if (searchQuery.isBlank()) storedEntries
                    else storedEntries.filter {
                        it.question.contains(searchQuery, ignoreCase = true) ||
                                it.answer.contains(searchQuery, ignoreCase = true) ||
                                it.sourceSessionOrDoc.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Text(
                                text = if (storedEntries.isEmpty()) "No manual training entries stored yet." else "No matching entries found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            val formattedDate = remember(item.createdAt) {
                                SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(item.createdAt))
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                                border = BorderStroke(1.dp, ObsidianCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ElectricIndigoContainer,
                                            border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = item.sourceSessionOrDoc,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricCyanGlow,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteManualTrainingEntry(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Question
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "Q:",
                                            fontWeight = FontWeight.Black,
                                            color = ElectricCyan,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = item.question,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }

                                    // Answer
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "A:",
                                            fontWeight = FontWeight.Black,
                                            color = NeonEmerald,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = item.answer,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Text(
                                        text = formattedDate,
                                        fontSize = 10.sp,
                                        color = TextTertiary,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Success Confirmation Toast/Card
        AnimatedVisibility(visible = successMsg != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald
                            )
                            Text(
                                text = "Knowledge Integrated!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = { viewModel.dismissTeachSuccess() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = successMsg ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonEmeraldGlow
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
            Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextSecondary, maxLines = 1)
        }
    }
}
