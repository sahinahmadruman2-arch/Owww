package com.example.ui.screens.teach

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun TeachScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val question by viewModel.teachQuestionInput.collectAsState()
    val answer by viewModel.teachAnswerInput.collectAsState()
    val isTeaching by viewModel.isTeaching.collectAsState()
    val successMsg by viewModel.teachSuccess.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Manual Knowledge Teaching",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Teach UrBots7 direct Q&A. The engine extracts concepts, builds graph links, and generates semantic variations so paraphrased queries retrieve it seamlessly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Quick Preset Buttons
        Text(
            text = "Try Example Knowledge:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    viewModel.updateTeachQuestion("How do I reply to hi?")
                    viewModel.updateTeachAnswer("You can say hi, hello, or another friendly greeting.")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Hi Reply Q&A", fontSize = 12.sp)
            }
            FilledTonalButton(
                onClick = {
                    viewModel.updateTeachQuestion("What is the polite way to say goodbye?")
                    viewModel.updateTeachAnswer("You can say 'Have a nice day' or 'It was a pleasure meeting you'.")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Goodbye Q&A", fontSize = 12.sp)
            }
        }

        // Question Input
        OutlinedTextField(
            value = question,
            onValueChange = { viewModel.updateTeachQuestion(it) },
            label = { Text("User Question") },
            placeholder = { Text("e.g. How do I reply to hi?") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("teach_question_input"),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.HelpOutline, contentDescription = null)
            }
        )

        // Answer Input
        OutlinedTextField(
            value = answer,
            onValueChange = { viewModel.updateTeachAnswer(it) },
            label = { Text("Taught Answer") },
            placeholder = { Text("e.g. You can say hi, hello, or another friendly greeting.") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .testTag("teach_answer_input"),
            leadingIcon = {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null)
            }
        )

        // Teach Action Button
        Button(
            onClick = { viewModel.submitManualTeach() },
            enabled = question.isNotBlank() && answer.isNotBlank() && !isTeaching,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_teach_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTeaching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthesizing Variations & Indexing...")
            } else {
                Icon(Icons.Default.School, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Teach UrBots7")
            }
        }

        // Success Confirmation Card
        AnimatedVisibility(visible = successMsg != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Knowledge Integrated!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { viewModel.dismissTeachSuccess() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = successMsg ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Now in Chat, asking 'Someone said hi to me, what can I say?' or 'What should I reply when greeted?' will semantically retrieve this taught answer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Pipeline Insights Explainer
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Semantic Teaching Architecture:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. Extracts concepts and topics from the question and answer\n" +
                            "2. Generates 6+ semantic variations (e.g. 'Someone greeted me. What can I say?')\n" +
                            "3. Links to Knowledge Graph with 'possible_reply' and 'related_to' relationships\n" +
                            "4. Persists into Room database and bumps Knowledge Version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
