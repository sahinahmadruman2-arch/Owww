package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.ConversationMessageEntity
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

    val quickQuestions = listOf(
        "Hi",
        "What does \"I'm full\" mean?",
        "I have a problem.",
        "My Roblox game isn't working.",
        "How can I ask for help?",
        "I can barely keep my eyes open because I'm so tired.",
        "What does hello mean?",
        "How should I reply to hello?"
    )

    LaunchedEffect(messages.size, isChatLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Top Action & Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricIndigoContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Grounded Semantic Reasoning",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricCyan
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ObsidianCard)
                    .testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Clear chat",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Quick Suggestions Horizontal Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(quickQuestions) { q ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianCardBorder.copy(alpha = 0.8f)),
                    modifier = Modifier.clickable { viewModel.askPrompt(q) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = q,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Messages List Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isChatLoading) {
                EmptyChatPlaceholder(onSelectQuery = { viewModel.askPrompt(it) })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onRegenerate = { viewModel.regenerateLastMessage() }
                        )
                    }

                    if (isChatLoading) {
                        item {
                            ThinkingStateIndicator(stage = chatLoadingStage)
                        }
                    }
                }
            }
        }

        // Floating Modern Glass Input Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = ObsidianCard
            ),
            border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradient))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(20.dp)
                )

                TextField(
                    value = chatInput,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = {
                        Text(
                            "Type a question, feeling, or manual Q&A pair...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 4
                )

                if (chatInput.isNotBlank()) {
                    IconButton(
                        onClick = { viewModel.updateChatInput("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear input",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Glowing Send Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (chatInput.isNotBlank() && !isChatLoading)
                                Brush.linearGradient(UserMessageGradient)
                            else
                                Brush.linearGradient(listOf(ObsidianCardBorder, ObsidianCardBorder))
                        )
                        .clickable(enabled = chatInput.isNotBlank() && !isChatLoading) {
                            viewModel.sendChatMessage()
                        }
                        .testTag("send_chat_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (chatInput.isNotBlank() && !isChatLoading) Color.White else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingStateIndicator(stage: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ObsidianCard
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(AccentGlowGradient)),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = ElectricCyan
            )
            Column {
                Text(
                    text = "UrBots7 Neural Reasoning",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = stage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyChatPlaceholder(onSelectQuery: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Graphic Card
        Card(
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradient)),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_urbots_hero),
                    contentDescription = "UrBots7 Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ObsidianBackground.copy(alpha = 0.85f),
                                    ObsidianBackground
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "UrBots7 Neural AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Multi-Strategy Semantic Intelligence • Local Knowledge Persistence",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Core Capabilities Highlight Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CapabilityBadge("Whole Meaning Intent", Icons.Outlined.Psychology, Modifier.weight(1f))
            CapabilityBadge("Manual Q&A Training", Icons.Outlined.School, Modifier.weight(1f))
            CapabilityBadge("Zero Hallucinations", Icons.Outlined.Shield, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sample Query CTA Button (maintains testTag)
        Button(
            onClick = { onSelectQuery("I have a problem.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("try_sample_query_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricIndigo
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Benchmark Query: 'I have a problem.'", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CapabilityBadge(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, ObsidianCardBorder.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ConversationMessageEntity,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showSources by remember { mutableStateOf(false) }

    val formattedTime = remember(message.createdAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.createdAt))
    }

    val sourcesList = remember(message.usedSourcesJson) {
        val list = mutableListOf<Triple<String, String, String>>()
        try {
            val jsonArr = JSONArray(message.usedSourcesJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    Triple(
                        obj.optString("documentTitle", "Knowledge Base"),
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
                    "topic" to obj.optString("topic", "General"),
                    "detectedMeaning" to obj.optString("detectedMeaning", "User communication")
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
            // USER MESSAGE BUBBLE
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
            // ASSISTANT MESSAGE BUBBLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini Avatar Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, ElectricCyan, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_urbots_avatar),
                        contentDescription = "UrBots7",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Message Body Card
                Card(
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = ObsidianCard
                    ),
                    border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradient)),
                    modifier = Modifier.widthIn(max = 330.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header with Intent Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UrBots7",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )

                            analysisData?.get("intent")?.let { intentName ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricIndigoContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = intentName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricIndigo,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )

                        // Grounding Citations Accordion
                        if (sourcesList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = ObsidianCardBorder)
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
                                        imageVector = Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Grounded Sources (${sourcesList.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonEmerald
                                    )
                                }
                                Icon(
                                    imageVector = if (showSources) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = showSources) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    sourcesList.forEach { (doc, section, statement) ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ObsidianSurface,
                                            border = BorderStroke(1.dp, ObsidianCardBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "• $doc → $section",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ElectricCyan
                                                )
                                                if (statement.isNotBlank()) {
                                                    Text(
                                                        text = "\"$statement\"",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Footer with Timestamp & Action Buttons
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
                                color = TextTertiary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("UrBots7 Answer", message.message)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy message",
                                        tint = TextTertiary,
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
                                        tint = TextTertiary,
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
