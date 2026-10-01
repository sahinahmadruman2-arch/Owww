package com.example.ui.screens.knowledge

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun KnowledgeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val title by viewModel.docTitleInput.collectAsState()
    val content by viewModel.docContentInput.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val progress by viewModel.pipelineProgress.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = ObsidianCard
            ),
            border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradient)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricIndigoContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = ElectricCyan
                        )
                    }
                }
                Column {
                    Text(
                        text = "Document & Book Ingestion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ingest books and documents. UrBots7 extracts concepts, facts, relationships, and generates semantic question variations for exact grounded retrieval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Quick Preset: Load "Basic Conversation" mini book
        OutlinedButton(
            onClick = { viewModel.loadSampleBookIntoForm() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("load_sample_book_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = ElectricCyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Load Mini Book: \"Basic Conversation\"", color = ElectricCyan, fontWeight = FontWeight.SemiBold)
        }

        // Title Input Field
        OutlinedTextField(
            value = title,
            onValueChange = { viewModel.updateDocTitle(it) },
            label = { Text("Book / Document Title", color = TextSecondary) },
            placeholder = { Text("e.g. Basic Conversation", color = TextTertiary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("document_title_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            leadingIcon = {
                Icon(Icons.Default.Title, contentDescription = null, tint = ElectricIndigo)
            }
        )

        // Content Input Field
        OutlinedTextField(
            value = content,
            onValueChange = { viewModel.updateDocContent(it) },
            label = { Text("Book Content / Full Text", color = TextSecondary) },
            placeholder = { Text("Paste document or book sections here...", color = TextTertiary) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .testTag("document_content_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = ElectricIndigo)
            }
        )

        // Analyze & Ingest Button
        Button(
            onClick = { viewModel.processKnowledgeDocument() },
            enabled = title.isNotBlank() && content.isNotBlank() && !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("analyze_document_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricIndigo
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing & Building Graph...", color = Color.White)
            } else {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deeply Analyze & Index Book", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Pipeline Live Progress Display
        AnimatedVisibility(visible = isAnalyzing || progress != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ObsidianCard
                ),
                border = BorderStroke(1.dp, Brush.linearGradient(AccentGlowGradient)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Knowledge Processing Pipeline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (progress?.isCompleted == true) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress?.progress ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = ElectricCyan,
                        trackColor = ObsidianCardBorder
                    )

                    Text(
                        text = progress?.stageName ?: "Preparing pipeline...",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Medium
                    )

                    HorizontalDivider(color = ObsidianCardBorder)

                    // Step badges
                    val steps = listOf(
                        "Reading...",
                        "Splitting chunks with overlap...",
                        "Deeply analyzing sections...",
                        "Extracting concepts & definitions...",
                        "Building relationships graph...",
                        "Generating question variations...",
                        "Indexing knowledge..."
                    )

                    steps.forEachIndexed { idx, stepName ->
                        val currentIdx = ((progress?.progress ?: 0f) * steps.size).toInt()
                        val isDone = currentIdx > idx || progress?.isCompleted == true
                        val isCurrent = currentIdx == idx && progress?.isCompleted != true

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else if (isCurrent) Icons.Default.Sync else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDone) NeonEmerald else if (isCurrent) ElectricCyan else TextTertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stepName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDone || isCurrent) TextPrimary else TextTertiary,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
