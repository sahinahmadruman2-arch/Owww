package com.example.ui.screens.knowledge

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

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
        // Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        text = "Document & Book Ingestion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Upload or paste books and documents. UrBots7 splits them into overlapping chunks, extracts concepts, facts, relationships, and generates dozens of question variations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Shortcut Button: Load "Basic Conversation" mini book
        OutlinedButton(
            onClick = { viewModel.loadSampleBookIntoForm() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("load_sample_book_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Load Mini Book: \"Basic Conversation\"")
        }

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { viewModel.updateDocTitle(it) },
            label = { Text("Book / Document Title") },
            placeholder = { Text("e.g. Basic Conversation") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("document_title_input"),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Title, contentDescription = null)
            }
        )

        // Content Input
        OutlinedTextField(
            value = content,
            onValueChange = { viewModel.updateDocContent(it) },
            label = { Text("Book Content / Full Text") },
            placeholder = { Text("Paste document or book sections here...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .testTag("document_content_input"),
            leadingIcon = {
                Icon(Icons.Default.Article, contentDescription = null)
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
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing & Building Graph...")
            } else {
                Icon(Icons.Default.Psychology, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deeply Analyze & Index Book")
            }
        }

        // Pipeline Live Progress Display
        AnimatedVisibility(visible = isAnalyzing || progress != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
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
                            fontWeight = FontWeight.Bold
                        )
                        if (progress?.isCompleted == true) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress?.progress ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )

                    Text(
                        text = progress?.stageName ?: "Preparing pipeline...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

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
                                tint = if (isDone) MaterialTheme.colorScheme.primary else if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stepName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
