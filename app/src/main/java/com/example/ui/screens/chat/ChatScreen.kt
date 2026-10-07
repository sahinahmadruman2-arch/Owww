package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Global toggle for Token Probabilities (A=1, top word prediction probabilities)
    var showTokenProbabilityCards by remember { mutableStateOf(true) }

    val quickSpaceQuestions = listOf(
        "Cosmic origin of stars & nebulae",
        "How do transformers predict words with probabilities?",
        "What does A = 1 token mapping mean?",
        "Latest discoveries from James Webb Space Telescope",
        "How does real-time browser research work in UrBots7?",
        "Explain gravitational waves and black holes",
        "Hi, UrBots cosmos explorer"
    )

    LaunchedEffect(messages.size, isChatLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(SpaceNebulaGradient))
            .padding(horizontal = 14.dp)
    ) {
        // Space Header with Token Probability & Live Web HUD
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
                        text = "Cosmic Web Research & AST",
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
                            text = if (showTokenProbabilityCards) "Words Prob [A=1]" else "Word Numbers Off",
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
        }

        // Live Chat Message Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptySpaceChatPlaceholder(
                    quickQuestions = quickSpaceQuestions,
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
                        SpaceChatMessageItem(
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
                            SpaceProcessingIndicator(stage = chatLoadingStage)
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
                items(quickSpaceQuestions.take(5)) { prompt ->
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

        // Futuristic Space Command Input Box
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
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = "Deep Cosmos",
                        tint = CosmicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                TextField(
                    value = chatInput,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = {
                        Text(
                            text = "Ask UrBots7 anything (research web, space, words...)",
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
}

/**
 * Deep Space Empty State with Constellations & Fast Quick-Start Prompts
 */
@Composable
fun EmptySpaceChatPlaceholder(
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
        // Space Avatar Core
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .border(2.dp, Brush.sweepGradient(StarlightGlowGradient), CircleShape)
                .shadow(16.dp, CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_space_ai_avatar),
                contentDescription = "UrBots7 Space AI",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "UrBots7 Cosmic Intelligence",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
            color = StarlightPrimary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Real-Time Browser Engine • Next-Word Token Probabilities • SQLite Store",
            style = MaterialTheme.typography.bodySmall,
            color = CosmicCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Space Sci-Fi Features Overview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradient)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SolarFlareGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Next-Word Prediction & Alphabetical Numbers (A = 1)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarlightPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "See how modern LLMs tokenize text into IDs, calculate token probabilities, and map letters to alphabetical numbers (A=1, B=2) alongside live browser research.",
                    fontSize = 11.sp,
                    color = StarlightSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "COMMENCE EXPLORATION QUERY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = StarlightTertiary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested Queries List
        quickQuestions.take(4).forEach { question ->
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodySmall,
                        color = StarlightPrimary,
                        fontSize = 12.sp
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
 * Chat Message Item with Deep Space Aesthetics, Sources Accordion,
 * and Next-Word Token Probability Inspection Dock.
 */
@Composable
fun SpaceChatMessageItem(
    message: ConversationMessageEntity,
    showTokenInspector: Boolean,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showSources by remember { mutableStateOf(false) }
    var showTokenBreakdown by remember { mutableStateOf(false) }

    val formattedTime = remember(message.createdAt) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.createdAt))
    }

    // Token Analysis calculation (A=1, B=2, probabilities)
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
                mapOf(
                    "intent" to obj.optString("intent", "GENERAL"),
                    "topic" to obj.optString("topic", "Cosmos"),
                    "detectedMeaning" to obj.optString("detectedMeaning", "Exploration")
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
            // User Space Comm Bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(Brush.linearGradient(UserMessageGradient))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
            // Bot Response Card in Deep Space Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Astronaut Space Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, CosmicCyan, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_space_ai_avatar),
                        contentDescription = "UrBots7 Space AI",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
                    modifier = Modifier.widthIn(max = 330.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header: Bot ID + Intent Badge + Word Rank Pill
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
                                    text = "UrBots7",
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
                            }

                            analysisData?.get("intent")?.let { intentName ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SupernovaPurpleContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = intentName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SupernovaPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
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

                        // ============================================================
                        // NEW FEATURE: Token Probabilities & Alphabetical Codes (A=1)
                        // ============================================================
                        if (showTokenInspector && tokenAnalysis.tokens.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = SpaceCardBorder)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Word Numbers & Prediction Ribbon
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
                                        color = SolarFlareGold.copy(alpha = 0.15f)
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
                                        .border(1.dp, SpaceCardBorder, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Model Perplexity: ${String.format(Locale.US, "%.2f", tokenAnalysis.perplexity)}",
                                            fontSize = 10.sp,
                                            color = StarlightSecondary
                                        )
                                        Text(
                                            text = "Avg Token Prob: ${(tokenAnalysis.averageConfidence * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrbitEmerald
                                        )
                                    }

                                    HorizontalDivider(color = SpaceCardBorder.copy(alpha = 0.5f))

                                    Text(
                                        text = "How Other AI Models Rank Top Next-Words:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicCyan
                                    )

                                    tokenAnalysis.tokens.take(4).forEach { tok ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "\"${tok.text}\"",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = StarlightPrimary
                                                )
                                                Text(
                                                    text = "→ Letter #${tok.alphabeticalNumber} [ID ${tok.tokenId}]",
                                                    fontSize = 9.sp,
                                                    color = SolarFlareGold
                                                )
                                            }
                                            Text(
                                                text = "p=${(tok.probability * 100).toInt()}% (Top 1)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OrbitEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Grounding Citations Accordion (Live Research)
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
                                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = OrbitEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Live Grounding & Research (${sourcesList.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = OrbitEmerald
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
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    sourcesList.forEach { (doc, section, statement) ->
                                        val isReddit = doc.contains("Reddit", ignoreCase = true)
                                        val isYouTube = doc.contains("YouTube", ignoreCase = true)
                                        val isWeb = doc.contains("Browser", ignoreCase = true) || doc.contains("Wikipedia", ignoreCase = true) || doc.contains("Web", ignoreCase = true)

                                        val badgeColor = when {
                                            isReddit -> RedditOrange
                                            isYouTube -> YouTubeRed
                                            isWeb -> WebBrowserBlue
                                            else -> CosmicCyan
                                        }
                                        val badgeIcon = when {
                                            isReddit -> Icons.Outlined.RssFeed
                                            isYouTube -> Icons.Outlined.PlayCircle
                                            isWeb -> Icons.Outlined.Language
                                            else -> Icons.Default.Book
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SpaceSurface,
                                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
                                                    Text(
                                                        text = "$doc → $section",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeColor,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (statement.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "\"$statement\"",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = StarlightSecondary,
                                                        fontSize = 11.sp
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
                                        val clip = ClipData.newPlainText("UrBots7 Response", message.message)
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
            // Word
            Text(
                text = token.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StarlightPrimary
            )
            // Number mapping (A=1) + Probability
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${token.alphabeticalNumber}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarFlareGold
                )
                Text(
                    text = "${(token.probability * 100).toInt()}%",
                    fontSize = 8.sp,
                    color = OrbitEmerald
                )
            }
        }
    }
}

/**
 * High-Tech Pulsing Space Progress Indicator
 */
@Composable
fun SpaceProcessingIndicator(stage: String) {
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
                    text = "Cosmic Engine Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan
                )
                Text(
                    text = stage.ifBlank { "Querying live web & synthesizing token distribution..." },
                    fontSize = 10.sp,
                    color = StarlightSecondary
                )
            }
        }
    }
}
