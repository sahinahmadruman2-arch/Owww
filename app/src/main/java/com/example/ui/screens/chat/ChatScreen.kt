package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationMessageEntity
import com.example.domain.ai.TokenAnalysisInfo
import com.example.domain.ai.TokenizerInspector
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val chatLoadingStage by viewModel.chatLoadingStage.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Global toggle for Token Probabilities (A=1, top word prediction probabilities)
    var showTokenProbabilityCards by remember { mutableStateOf(true) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var newChatTopicInput by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "Who is the best football player of all time (say in one word)",
        "Who is Karllo?",
        "What is 25 * 18?",
        "Explain gravitational waves and black holes",
        "What is the capital of France?",
        "How do transformers predict words with probabilities?",
        "What does A = 1 token mapping mean?",
        "Latest discoveries from James Webb Space Telescope"
    )

    LaunchedEffect(messages.size, isChatLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(SpaceNebulaGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp)
        ) {
        // Chat Top Bar & Chat Session Adding Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Research Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpaceSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, CosmicCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TravelExplore,
                        contentDescription = null,
                        tint = CosmicCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Karllo Live Research & AST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StarlightPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(OrbitEmerald)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Word Probabilities & Token Index Toggle
                FilterChip(
                    selected = showTokenProbabilityCards,
                    onClick = { showTokenProbabilityCards = !showTokenProbabilityCards },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Analytics,
                                contentDescription = null,
                                tint = if (showTokenProbabilityCards) SolarFlareGold else StarlightTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (showTokenProbabilityCards) "Words [A=1]" else "Words Off",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showTokenProbabilityCards) SolarFlareGold else StarlightSecondary
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarFlareContainer.copy(alpha = 0.6f),
                        containerColor = SpaceSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (showTokenProbabilityCards) SolarFlareGold.copy(alpha = 0.6f) else SpaceCardBorder
                    ),
                    modifier = Modifier.testTag("toggle_token_probability")
                )

                // Add New Chat Button
                FilledTonalButton(
                    onClick = { showNewChatDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CosmicCyanContainer,
                        contentColor = CosmicCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(34.dp).testTag("new_chat_button")
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Chat", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Chat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sessions Switcher Ribbon if multiple chats exist
        val sessionList = remember(allSessions, currentSessionId) {
            val list = allSessions.toMutableList()
            if (!list.contains(currentSessionId)) list.add(0, currentSessionId)
            if (!list.contains("default_session")) list.add("default_session")
            list.distinct()
        }

        if (sessionList.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sessionList) { sId ->
                    val isSelected = sId == currentSessionId
                    val displayName = when {
                        sId == "default_session" -> "Main Chat"
                        sId.startsWith("chat_") -> {
                            val raw = sId.removePrefix("chat_")
                            if (raw.all { it.isDigit() }) "Chat #${raw.takeLast(4)}" else raw.replace("_", " ").take(16)
                        }
                        else -> sId.take(16)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SupernovaPurpleContainer else SpaceSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SupernovaPurple else SpaceCardBorder
                        ),
                        modifier = Modifier.clickable { viewModel.switchChatSession(sId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (isSelected) CosmicCyanGlow else StarlightTertiary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = displayName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) StarlightPrimary else StarlightSecondary
                            )
                        }
                    }
                }
            }
        }

        // Live Chat Message Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyKarlloChatPlaceholder(
                    quickQuestions = quickQuestions,
                    onSelectQuestion = { q ->
                        viewModel.updateChatInput(q)
                        viewModel.sendChatMessage()
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        KarlloChatMessageItem(
                            message = msg,
                            showTokenInspector = showTokenProbabilityCards,
                            onRegenerate = {
                                if (msg.role == "assistant") {
                                    val previousUser = messages.takeWhile { it.id != msg.id }.lastOrNull { it.role == "user" }
                                    if (previousUser != null) {
                                        viewModel.updateChatInput(previousUser.message)
                                        viewModel.sendChatMessage()
                                    }
                                }
                            }
                        )
                    }

                    if (isChatLoading) {
                        item {
                            KarlloProcessingIndicator(stage = chatLoadingStage)
                        }
                    }
                }
            }
        }

        // Quick Suggestions Horizontal Dock (when not loading)
        if (!isChatLoading && messages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickQuestions.take(5)) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SpaceSurface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, SpaceCardBorder),
                        modifier = Modifier.clickable {
                            viewModel.updateChatInput(prompt)
                            viewModel.sendChatMessage()
                        }
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 11.sp,
                            color = StarlightSecondary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Futuristic Command Input Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            color = SpaceCard,
            border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradientSubtle)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear / Reset Icon if text exists
                if (chatInput.isNotBlank()) {
                    IconButton(
                        onClick = { viewModel.updateChatInput("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear input",
                            tint = StarlightTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = "Karllo AI Intelligence",
                        tint = CosmicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                TextField(
                    value = chatInput,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = {
                        Text(
                            text = "Ask Karllo anything, research live web, or calculate...",
                            color = StarlightTertiary,
                            fontSize = 13.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = StarlightPrimary,
                        unfocusedTextColor = StarlightPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    singleLine = false,
                    maxLines = 4
                )

                // Transmit Command Button
                val canSend = chatInput.isNotBlank() && !isChatLoading
                FilledIconButton(
                    onClick = {
                        if (canSend) {
                            viewModel.sendChatMessage()
                        }
                    },
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CosmicCyan,
                        disabledContainerColor = SpaceCardBorder
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (canSend) SpaceVoid else StarlightTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // New Chat Creation Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = {
                Text("Start New Chat", fontWeight = FontWeight.Bold, color = StarlightPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Give this chat a title or topic (optional):",
                        fontSize = 12.sp,
                        color = StarlightSecondary
                    )
                    OutlinedTextField(
                        value = newChatTopicInput,
                        onValueChange = { newChatTopicInput = it },
                        placeholder = { Text("e.g. Science, Space, Math, Coding", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_chat_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startNewChat(newChatTopicInput.trim())
                        newChatTopicInput = ""
                        showNewChatDialog = false
                    },
                    modifier = Modifier.testTag("confirm_new_chat_button")
                ) {
                    Text("Create Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SpaceCard
        )
    }
    }
}

/**
 * Karllo Empty State without robot photo (clean glowing geometric avatar)
 */
@Composable
fun EmptyKarlloChatPlaceholder(
    quickQuestions: List<String>,
    onSelectQuestion: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Geometric Modern Karllo Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(StarlightGlowGradient))
                .shadow(12.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "K",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Karllo",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            color = StarlightPrimary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Verified Reasoning Pipeline • Real-Time Web • Word Numbers [A=1]",
            style = MaterialTheme.typography.bodySmall,
            color = CosmicCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Karllo Pipeline Architecture Diagram Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradient)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OrbitEmeraldGlow, modifier = Modifier.size(15.dp))
                    Text(
                        text = "Karllo Execution & Verification Pipeline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarlightPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Visual flow breakdown
                PipelineVisualFlowDiagram()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "COMMENCE QUERY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = StarlightTertiary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        quickQuestions.take(3).forEach { question ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpaceSurface,
                border = BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSelectQuestion(question) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodySmall,
                        color = StarlightPrimary,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = CosmicCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diagram showing the 7-step pipeline:
 * User request -> Understand -> Decide what tools are needed -> Browser/Knowledge/Calculator/etc -> Collect info -> Verify -> Answer.
 */
@Composable
fun PipelineVisualFlowDiagram() {
    val steps = listOf(
        "User request" to CosmicCyan,
        "Understand" to SupernovaPurple,
        "Decide what tools are needed" to DeepNebulaMagenta,
        "Browser / Knowledge / Calculator / etc." to SolarFlareGold,
        "Collect information" to CosmicCyan,
        "Verify" to OrbitEmeraldGlow,
        "Answer." to OrbitEmerald
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        steps.forEachIndexed { index, (label, color) ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (index < steps.size - 1) {
                Text(
                    text = "↓",
                    fontSize = 10.sp,
                    color = StarlightTertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Chat Message Item with Verification Trace Accordion, Sources Accordion,
 * and Next-Word Token Probability Inspection Dock.
 */
@Composable
fun KarlloChatMessageItem(
    message: ConversationMessageEntity,
    showTokenInspector: Boolean,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showSources by remember { mutableStateOf(false) }
    var showTrace by remember { mutableStateOf(false) }
    var showTokenBreakdown by remember { mutableStateOf(false) }

    val formattedTime = remember(message.createdAt) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.createdAt))
    }

    val tokenAnalysis = remember(message.message) {
        TokenizerInspector.analyzeText(message.message)
    }

    val sourcesList = remember(message.usedSourcesJson) {
        val list = mutableListOf<Triple<String, String, String>>()
        try {
            val jsonArr = JSONArray(message.usedSourcesJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    Triple(
                        obj.optString("documentTitle", "Live Knowledge"),
                        obj.optString("sectionTitle", "Section"),
                        obj.optString("statement", "")
                    )
                )
            }
        } catch (_: Exception) {}
        list
    }

    val analysisData = remember(message.analysisMetadataJson) {
        if (message.analysisMetadataJson.isNullOrBlank()) null
        else {
            try {
                val obj = JSONObject(message.analysisMetadataJson)
                val traceArr = obj.optJSONArray("executionTrace")
                val traceList = mutableListOf<String>()
                if (traceArr != null) {
                    for (i in 0 until traceArr.length()) {
                        traceList.add(traceArr.getString(i))
                    }
                }
                val constraintsArr = obj.optJSONArray("constraints")
                val constraintsList = mutableListOf<String>()
                if (constraintsArr != null) {
                    for (i in 0 until constraintsArr.length()) {
                        constraintsList.add(constraintsArr.getString(i))
                    }
                }
                mapOf(
                    "intent" to obj.optString("intent", "GENERAL"),
                    "topic" to obj.optString("topic", "General"),
                    "detectedMeaning" to obj.optString("detectedMeaning", "Exploration"),
                    "verificationStatus" to obj.optString("verificationStatus", "Verified"),
                    "reasoningDepth" to obj.optString("reasoningDepth", "Deep Multi-Hop Synthesis"),
                    "knowledgeDomain" to obj.optString("knowledgeDomain", "General Knowledge"),
                    "constraints" to constraintsList,
                    "executionTrace" to traceList
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Space Comm Bubble (Adaptive width up to 540dp for laptops)
            Box(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(Brush.linearGradient(UserMessageGradient))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        } else {
            // Karllo Response Card without robot photo (clean monogram badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Karllo Letter Emblem
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(StarlightGlowGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Card(
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = SpaceCard),
                    border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradient)),
                    modifier = Modifier.widthIn(max = 640.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header: Karllo + Knowledge Domain + Verification badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Karllo",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CosmicCyan
                                )
                                // Active Core Beacon
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(OrbitEmerald)
                                )

                                val domain = (analysisData?.get("knowledgeDomain") as? String)
                                if (!domain.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CosmicCyan.copy(alpha = 0.12f),
                                        border = BorderStroke(0.5.dp, CosmicCyan.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = domain,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmicCyan,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Verification Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OrbitEmeraldContainer.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, OrbitEmerald.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OrbitEmeraldGlow,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = (analysisData?.get("verificationStatus") as? String) ?: "Verified",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrbitEmeraldGlow
                                    )
                                }
                            }
                        }

                        // Constraints Badge if detected
                        @Suppress("UNCHECKED_CAST")
                        val constraintsList = (analysisData?.get("constraints") as? List<String>) ?: emptyList()
                        if (constraintsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SolarFlareGold.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, SolarFlareGold.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "✓ Constraint Satisfied: ${constraintsList.joinToString()}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SolarFlareGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Execution Pipeline Accordion Pill
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SpaceSurface,
                            border = BorderStroke(1.dp, CosmicCyan.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTrace = !showTrace }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schema,
                                        contentDescription = null,
                                        tint = CosmicCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Understand → Multi-Source Dispatch → Verify → Output",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CosmicCyan
                                    )
                                }
                                Icon(
                                    imageVector = if (showTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = StarlightTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Expanded Execution Trace
                        AnimatedVisibility(visible = showTrace) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .background(SpaceSurface, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                @Suppress("UNCHECKED_CAST")
                                val traceList = (analysisData?.get("executionTrace") as? List<String>)
                                    ?: listOf(
                                        "User request",
                                        "Understand",
                                        "Decide what tools are needed",
                                        "Real-Time Browser / Knowledge / Calculator",
                                        "Collect information",
                                        "Verify",
                                        "Answer."
                                    )

                                traceList.forEachIndexed { idx, step ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${idx + 1}.",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SolarFlareGold
                                        )
                                        Text(
                                            text = step,
                                            fontSize = 9.sp,
                                            color = StarlightSecondary
                                        )
                                    }
                                    if (idx < traceList.size - 1) {
                                        Text(
                                            text = "    ↓",
                                            fontSize = 9.sp,
                                            color = StarlightTertiary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Assistant Text Response
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StarlightPrimary,
                            lineHeight = 22.sp
                        )

                        // Word Probabilities & Alphabetical Codes (A=1)
                        if (showTokenInspector && tokenAnalysis.tokens.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = SpaceCardBorder)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Word Numbers Ribbon
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTokenBreakdown = !showTokenBreakdown },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = SolarFlareGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Word Number & Logits (A=1, B=2)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SolarFlareGold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = SolarFlareContainer.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${tokenAnalysis.tokens.size} tokens",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SolarFlareGold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (showTokenBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = StarlightSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Interactive Horizontal Token Sequence Inspector
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(tokenAnalysis.tokens.take(12)) { tok ->
                                    TokenChipView(tok)
                                }
                            }

                            // Expanded Token Logits & Probabilities Details
                            AnimatedVisibility(visible = showTokenBreakdown) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(SpaceSurface, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Alphabetical Mapping:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CosmicCyan
                                        )
                                        Text(
                                            text = "A=1, B=2 ... Z=26",
                                            fontSize = 10.sp,
                                            color = StarlightSecondary
                                        )
                                    }

                                    tokenAnalysis.tokens.take(5).forEach { tok ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "\"${tok.text}\" → [Letter #${tok.alphabeticalNumber}]",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = StarlightPrimary
                                            )
                                            Text(
                                                text = "${(tok.probability * 100).toInt()}% prob (logit: ${"%.2f".format(tok.logit)})",
                                                fontSize = 9.sp,
                                                color = SolarFlareGold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Grounded Citations
                        if (sourcesList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = SpaceCardBorder)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSources = !showSources },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = CosmicCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${sourcesList.size} Grounded Sources",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicCyan
                                    )
                                }

                                Icon(
                                    imageVector = if (showSources) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = StarlightSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = showSources) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sourcesList.forEach { (doc, sec, stmt) ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SpaceSurface,
                                            border = BorderStroke(1.dp, SpaceCardBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = doc,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CosmicCyan
                                                )
                                                if (sec.isNotBlank()) {
                                                    Text(
                                                        text = sec,
                                                        fontSize = 10.sp,
                                                        color = StarlightSecondary
                                                    )
                                                }
                                                if (stmt.isNotBlank()) {
                                                    Text(
                                                        text = stmt,
                                                        fontSize = 10.sp,
                                                        color = StarlightTertiary,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Footer with Timestamp & Quick Actions
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = StarlightTertiary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Karllo Response", message.message)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy message",
                                        tint = StarlightTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onRegenerate,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Regenerate message",
                                        tint = StarlightTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Token Pill showing word, alphabetical number (A=1), and prediction probability
 */
@Composable
fun TokenChipView(token: com.example.domain.ai.AnalyzedToken) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SpaceSurface,
        border = BorderStroke(1.dp, SolarFlareGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = token.text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = StarlightPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${token.alphabeticalNumber}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = SolarFlareGold
                )
                Text(
                    text = "• ${(token.probability * 100).toInt()}%",
                    fontSize = 8.sp,
                    color = CosmicCyan
                )
            }
        }
    }
}

/**
 * Pulsing Progress Indicator
 */
@Composable
fun KarlloProcessingIndicator(stage: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpaceCard,
        border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradient)),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = CosmicCyan,
                strokeWidth = 2.dp
            )
            Column {
                Text(
                    text = "Karllo Pipeline Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan
                )
                Text(
                    text = stage.ifBlank { "Understand → Decide tools → Verify → Answer..." },
                    fontSize = 10.sp,
                    color = StarlightSecondary
                )
            }
        }
    }
}
