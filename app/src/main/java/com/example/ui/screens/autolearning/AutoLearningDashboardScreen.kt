package com.example.ui.screens.autolearning

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class AutoLearningSection(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.Dashboard),
    LIVE_ACTIVITY("Live Activity", Icons.Outlined.Sensors),
    KNOWLEDGE_EXPLORER("Knowledge", Icons.Outlined.AutoStories),
    USER_MEMORY("User Memory", Icons.Outlined.Psychology),
    WEB_LEARNING("Web Learning", Icons.Outlined.Language),
    SELF_IMPROVEMENT("Self-Improvement", Icons.Outlined.BugReport),
    CONTROLS("Controls", Icons.Outlined.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLearningDashboardScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(AutoLearningSection.OVERVIEW) }

    Surface(
        color = SpaceSurface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(SpaceNebulaGradient))
        ) {
            // Header Top Bar
            Surface(
                color = SpaceSurface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradientSubtle))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CosmicCyan, SupernovaPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Auto Learning Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = StarlightPrimary
                            )
                            Text(
                                text = "Continuous Autonomous Improvement & Learning Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = StarlightSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_auto_learning")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StarlightPrimary
                        )
                    }
                }
            }

            // Section Tabs Navigation
            val tabScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScrollState)
                    .background(SpaceCardSurface.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoLearningSection.values().forEach { section ->
                    val isSelected = selectedSection == section
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSection = section },
                        label = {
                            Text(
                                text = section.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SupernovaPurpleContainer,
                            selectedLabelColor = CosmicCyanGlow,
                            selectedLeadingIconColor = CosmicCyanGlow,
                            containerColor = SpaceCardSurface,
                            labelColor = StarlightSecondary,
                            iconColor = StarlightSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = CosmicCyan,
                            borderColor = SpaceCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tab_${section.name.lowercase()}")
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedSection) {
                    AutoLearningSection.OVERVIEW -> OverviewSectionView(viewModel = viewModel)
                    AutoLearningSection.LIVE_ACTIVITY -> LiveActivitySectionView(viewModel = viewModel)
                    AutoLearningSection.KNOWLEDGE_EXPLORER -> KnowledgeExplorerSectionView(viewModel = viewModel)
                    AutoLearningSection.USER_MEMORY -> UserMemorySectionView(viewModel = viewModel)
                    AutoLearningSection.WEB_LEARNING -> WebLearningSectionView(viewModel = viewModel)
                    AutoLearningSection.SELF_IMPROVEMENT -> SelfImprovementSectionView(viewModel = viewModel)
                    AutoLearningSection.CONTROLS -> ControlsSectionView(viewModel = viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. OVERVIEW SECTION
// -------------------------------------------------------------
@Composable
private fun OverviewSectionView(viewModel: MainViewModel) {
    val settings by viewModel.learningSettings.collectAsState()
    val queuedCount by viewModel.queuedTasksCount.collectAsState()
    val runningCount by viewModel.runningTasksCount.collectAsState()
    val completedCount by viewModel.completedTasksCount.collectAsState()
    val failedCount by viewModel.failedTasksCount.collectAsState()
    val sharedCount by viewModel.sharedKnowledgeCount.collectAsState()
    val memoriesCount by viewModel.userMemoriesCount.collectAsState()
    val pendingCandidates by viewModel.pendingCandidatesCount.collectAsState()
    val tasks by viewModel.learningTasks.collectAsState()
    val lastCompletedTask = tasks.firstOrNull { it.status == "COMPLETED" }

    val isAutoEnabled = settings?.isAutoLearningEnabled ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
            border = BorderStroke(1.dp, if (isAutoEnabled) OrbitEmerald.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Learning Engine Status",
                            style = MaterialTheme.typography.titleSmall,
                            color = StarlightSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isAutoEnabled) OrbitEmerald else AmberWarning)
                            )
                            Text(
                                text = if (isAutoEnabled) "ACTIVE & RUNNING" else "PAUSED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isAutoEnabled) OrbitEmeraldGlow else AmberWarning
                            )
                        }
                    }

                    StatusBadge(status = if (isAutoEnabled) "RUNNING" else "PAUSED")
                }

                HorizontalDivider(color = SpaceCardBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FeatureStatusItem(
                        label = "Web Research",
                        enabled = settings?.isWebLearningEnabled ?: true
                    )
                    FeatureStatusItem(
                        label = "Memory Extraction",
                        enabled = settings?.isMemoryExtractionEnabled ?: true
                    )
                    FeatureStatusItem(
                        label = "Security Approval",
                        enabled = settings?.requireApprovalForSensitive ?: true
                    )
                }
            }
        }

        // Tasks Statistics Grid
        Text(
            text = "Autonomous Task Queue Status",
            style = MaterialTheme.typography.labelLarge,
            color = CosmicCyanGlow,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricStatCard(title = "Queued", value = "$queuedCount", color = CosmicCyan, modifier = Modifier.weight(1f))
            MetricStatCard(title = "Running", value = "$runningCount", color = OrbitEmerald, modifier = Modifier.weight(1f))
            MetricStatCard(title = "Completed", value = "$completedCount", color = SupernovaPurple, modifier = Modifier.weight(1f))
            MetricStatCard(title = "Failed", value = "$failedCount", color = CrimsonError, modifier = Modifier.weight(1f))
        }

        // Knowledge & Memory Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                border = BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Shared Knowledge Items", style = MaterialTheme.typography.labelSmall, color = StarlightSecondary)
                    Text(text = "$sharedCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = CosmicCyanGlow)
                    Text(text = "Verified reusable knowledge", style = MaterialTheme.typography.bodySmall, color = StarlightTertiary, fontSize = 10.sp)
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                border = BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "User Memories Saved", style = MaterialTheme.typography.labelSmall, color = StarlightSecondary)
                    Text(text = "$memoriesCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = SupernovaPurple)
                    Text(text = "Private profile & context", style = MaterialTheme.typography.bodySmall, color = StarlightTertiary, fontSize = 10.sp)
                }
            }
        }

        // Memories Awaiting Review & Last Task
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
            border = BorderStroke(1.dp, SpaceCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Pending Review Candidates", style = MaterialTheme.typography.labelMedium, color = StarlightSecondary)
                    Text(text = "$pendingCandidates awaiting verification", fontWeight = FontWeight.Bold, color = if (pendingCandidates > 0) AmberWarning else StarlightTertiary, fontSize = 12.sp)
                }
                HorizontalDivider(color = SpaceCardBorder.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Last Successful Task", style = MaterialTheme.typography.labelMedium, color = StarlightSecondary)
                    Text(
                        text = lastCompletedTask?.title ?: "None executed yet",
                        fontWeight = FontWeight.Bold,
                        color = OrbitEmeraldGlow,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. LIVE ACTIVITY FEED
// -------------------------------------------------------------
@Composable
private fun LiveActivitySectionView(viewModel: MainViewModel) {
    val events by viewModel.learningEvents.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Real-Time Engine Activity (${events.size} logged events)",
                style = MaterialTheme.typography.titleSmall,
                color = CosmicCyanGlow,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { viewModel.clearLearningEvents() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear logs", tint = StarlightSecondary, modifier = Modifier.size(18.dp))
            }
        }

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.HistoryEdu, contentDescription = null, tint = StarlightTertiary, modifier = Modifier.size(48.dp))
                    Text("No learning events logged yet.", color = StarlightSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Start chatting to see live learning activity from the backend.", color = StarlightTertiary, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(events, key = { it.id }) { event ->
                    LearningEventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun LearningEventCard(event: LearningEventEntity) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(event.timestamp))

    val (badgeColor, icon) = when (event.eventType) {
        "CONVERSATION_ANALYSIS" -> CosmicCyan to Icons.Default.ChatBubbleOutline
        "COREFERENCE_RESOLVED" -> SupernovaPurple to Icons.Default.Hub
        "SKILL_UPDATED" -> OrbitEmerald to Icons.AutoMirrored.Filled.TrendingUp
        "CANDIDATE_CREATED" -> AmberWarning to Icons.Default.Lightbulb
        "WEB_RESEARCH" -> CosmicCyanGlow to Icons.Default.Language
        "SOURCE_VERIFIED" -> OrbitEmerald to Icons.Default.Verified
        "QUALITY_EVALUATED" -> SupernovaPurple to Icons.AutoMirrored.Filled.FactCheck
        "DEDUPLICATION_CHECK" -> CosmicCyan to Icons.Default.FilterList
        "INDEX_UPDATED" -> OrbitEmerald to Icons.Default.Sync
        "PATCH_PROPOSED" -> AmberWarning to Icons.Default.Build
        "TEST_EXECUTED" -> OrbitEmerald to Icons.Default.PlayArrow
        else -> StarlightSecondary to Icons.Default.Info
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.eventType.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                    Text(text = timeStr, fontSize = 10.sp, color = StarlightTertiary)
                }
                Text(
                    text = event.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StarlightPrimary
                )
                if (event.details.isNotBlank()) {
                    Text(
                        text = event.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = StarlightSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. KNOWLEDGE EXPLORER
// -------------------------------------------------------------
@Composable
private fun KnowledgeExplorerSectionView(viewModel: MainViewModel) {
    val knowledgeList by viewModel.sharedKnowledge.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shared Knowledge Base (${knowledgeList.size})",
                style = MaterialTheme.typography.titleSmall,
                color = CosmicCyanGlow,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SupernovaPurpleContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = CosmicCyanGlow)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Knowledge", fontSize = 11.sp, color = CosmicCyanGlow)
            }
        }

        if (knowledgeList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = StarlightTertiary, modifier = Modifier.size(44.dp))
                    Text("No shared knowledge stored yet.", color = StarlightSecondary)
                    Text("Ingest documents, research topics, or add verified facts directly.", color = StarlightTertiary, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(knowledgeList, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                        border = BorderStroke(1.dp, SpaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.isShared) OrbitEmerald.copy(alpha = 0.2f) else SupernovaPurple.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (item.isShared) "SHARED" else "PRIVATE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isShared) OrbitEmeraldGlow else CosmicCyanGlow,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.category,
                                        fontSize = 11.sp,
                                        color = StarlightSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSharedKnowledge(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StarlightPrimary
                            )
                            Text(
                                text = item.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = StarlightSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = OrbitEmerald, modifier = Modifier.size(14.dp))
                                    Text(text = item.verificationStatus, fontSize = 10.sp, color = OrbitEmeraldGlow)
                                }
                                if (item.sourceUrl != null) {
                                    Text(text = item.sourceDomain ?: "Web Source", fontSize = 10.sp, color = CosmicCyan, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newSummary by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("General Knowledge") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SpaceCardSurface,
            title = { Text("Add Shared Knowledge Item", color = StarlightPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title / Concept") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSummary,
                        onValueChange = { newSummary = it },
                        label = { Text("Summary / Verified Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newSummary.isNotBlank()) {
                            viewModel.addSharedKnowledge(newTitle, newSummary, newCategory)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SupernovaPurple)
                ) {
                    Text("Save to Knowledge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = StarlightSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 4. USER MEMORY SECTION
// -------------------------------------------------------------
@Composable
private fun UserMemorySectionView(viewModel: MainViewModel) {
    val memories by viewModel.userMemories.collectAsState()
    val skills by viewModel.userSkillProfiles.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        MemoryCategories.CONVERSATION_CONTEXT,
        MemoryCategories.USER_PREFERENCES,
        MemoryCategories.USER_SKILLS,
        MemoryCategories.ACTIVE_PROJECTS,
        MemoryCategories.IMPORTANT_FACTS,
        MemoryCategories.CORRECTIONS_AND_FEEDBACK,
        MemoryCategories.GENERAL_KNOWLEDGE,
        MemoryCategories.LEARNING_CANDIDATES
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Private User Memories & Skill Profile",
                style = MaterialTheme.typography.titleSmall,
                color = CosmicCyanGlow,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { showAddMemoryDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SupernovaPurpleContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = CosmicCyanGlow)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Memory", fontSize = 11.sp, color = CosmicCyanGlow)
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All (${memories.size})", fontSize = 11.sp) }
            )
            categories.forEach { cat ->
                val count = memories.count { it.category == cat }
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text("$cat ($count)", fontSize = 11.sp) }
                )
            }
        }

        val filteredMemories = if (selectedCategory == null) memories else memories.filter { it.category == selectedCategory }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // User Skill Profiles Header & List
            if (skills.isNotEmpty() && (selectedCategory == null || selectedCategory == MemoryCategories.USER_SKILLS)) {
                item {
                    Text(
                        text = "User Skill Profile Estimates",
                        style = MaterialTheme.typography.labelMedium,
                        color = OrbitEmeraldGlow,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(skills, key = { it.id }) { skill ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                        border = BorderStroke(1.dp, OrbitEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = skill.topic, fontWeight = FontWeight.Bold, color = StarlightPrimary)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = OrbitEmeraldContainer
                                    ) {
                                        Text(
                                            text = skill.level,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = OrbitEmeraldGlow,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteSkillProfile(skill.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(text = "Style: ${skill.preferredExplanationStyle} • Confidence: ${(skill.confidence * 100).toInt()}%", fontSize = 11.sp, color = StarlightSecondary)
                            if (skill.lastEvidence.isNotBlank()) {
                                Text(text = "Evidence: \"${skill.lastEvidence}\"", fontSize = 10.sp, color = StarlightTertiary)
                            }
                        }
                    }
                }
            }

            // Saved Memories
            item {
                Text(
                    text = "Saved Memory Entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmicCyanGlow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (filteredMemories.isEmpty()) {
                item {
                    Text(
                        text = "No saved memories in this category yet.",
                        color = StarlightTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(filteredMemories, key = { it.id }) { mem ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                        border = BorderStroke(1.dp, SpaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = SupernovaPurpleContainer) {
                                        Text(text = mem.category, fontSize = 9.sp, color = CosmicCyanGlow, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(text = mem.key, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StarlightPrimary)
                                }
                                Text(text = mem.value, fontSize = 12.sp, color = StarlightSecondary)
                            }
                            IconButton(onClick = { viewModel.deleteUserMemory(mem.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = CrimsonError, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMemoryDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf(MemoryCategories.IMPORTANT_FACTS) }

        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            containerColor = SpaceCardSurface,
            title = { Text("Add User Memory", color = StarlightPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Key / Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value / Statement") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (key.isNotBlank() && value.isNotBlank()) {
                            viewModel.addUserMemory(cat, key, value)
                            showAddMemoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SupernovaPurple)
                ) {
                    Text("Save Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) { Text("Cancel", color = StarlightSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 5. WEB LEARNING SECTION
// -------------------------------------------------------------
@Composable
private fun WebLearningSectionView(viewModel: MainViewModel) {
    val sources by viewModel.knowledgeSources.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Permitted Web Research Sources (${sources.size} Harvested)",
            style = MaterialTheme.typography.titleSmall,
            color = CosmicCyanGlow,
            fontWeight = FontWeight.Bold
        )

        if (sources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Language, contentDescription = null, tint = StarlightTertiary, modifier = Modifier.size(44.dp))
                    Text("No web research sources recorded yet.", color = StarlightSecondary)
                    Text("Search in Cosmos Chat or Live Orbit to research topics.", color = StarlightTertiary, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sources, key = { it.id }) { src ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                        border = BorderStroke(1.dp, SpaceCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(4.dp), color = CosmicCyan.copy(alpha = 0.2f)) {
                                    Text(text = src.domain, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CosmicCyanGlow, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (src.isSavedToKnowledge) OrbitEmerald.copy(alpha = 0.2f) else StarlightSecondary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (src.isSavedToKnowledge) "SAVED TO KNOWLEDGE" else "REFERENCED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (src.isSavedToKnowledge) OrbitEmeraldGlow else StarlightSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(text = src.title, fontWeight = FontWeight.Bold, color = StarlightPrimary)
                            Text(text = src.snippet, fontSize = 11.sp, color = StarlightSecondary)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = dateFormat.format(Date(src.retrievalTimestamp)), fontSize = 10.sp, color = StarlightTertiary)
                                Text(text = src.url, fontSize = 10.sp, color = CosmicCyan, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. SELF-IMPROVEMENT & SAFE CODE PATCHES
// -------------------------------------------------------------
@Composable
private fun SelfImprovementSectionView(viewModel: MainViewModel) {
    val diagnostics by viewModel.diagnosticReports.collectAsState()
    val evaluations by viewModel.responseEvaluations.collectAsState()
    val patches by viewModel.proposedPatches.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Autonomous Self-Improvement & Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                color = CosmicCyanGlow,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { viewModel.runDiagnostics() },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run Diagnostics", fontSize = 11.sp)
            }
        }

        // Diagnostics Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
            border = BorderStroke(1.dp, SpaceCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Diagnostic Reports & Error Detection", fontWeight = FontWeight.Bold, color = StarlightPrimary)
                if (diagnostics.isEmpty()) {
                    Text(text = "No anomalies detected. All components functioning normally.", fontSize = 11.sp, color = StarlightSecondary)
                } else {
                    diagnostics.take(3).forEach { d ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (d.severity == "ERROR") CrimsonError else OrbitEmerald))
                            Text(text = "[${d.component}] ${d.errorMessage}", fontSize = 11.sp, color = StarlightSecondary)
                        }
                    }
                }
            }
        }

        // Response Evaluations
        Text(text = "Recent Response Quality Evaluations", fontWeight = FontWeight.Bold, color = CosmicCyanGlow, fontSize = 12.sp)
        evaluations.take(3).forEach { ev ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                border = BorderStroke(1.dp, SpaceCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Query: \"${ev.query}\"", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StarlightPrimary, maxLines = 1)
                        StatusBadge(status = if (ev.passed) "PASSED" else "NEEDS_RETRY")
                    }
                    Text(text = "Relevance: ${(ev.relevanceScore * 100).toInt()}% • Context: ${(ev.contextAccuracyScore * 100).toInt()}% • Follow: ${(ev.instructionFollowScore * 100).toInt()}%", fontSize = 10.sp, color = StarlightSecondary)
                }
            }
        }

        // Proposed Code Patches (Requires Owner Approval)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Staged Code Patches (${patches.size})", fontWeight = FontWeight.Bold, color = CosmicCyanGlow, fontSize = 12.sp)
            TextButton(
                onClick = {
                    viewModel.proposePatch(
                        title = "Optimize Coreference Resolution Index",
                        targetFile = "SemanticRetrievalEngine.kt",
                        diff = "+ // Index optimization for high-density coreference lookup\n+ cache.put(resolvedEntity)",
                        description = "Caches resolved project and topic references across session turns."
                    )
                }
            ) {
                Text("+ Propose Staged Patch", fontSize = 11.sp, color = CosmicCyan)
            }
        }

        if (patches.isEmpty()) {
            Text(text = "No pending code patches. Production code is stable.", color = StarlightTertiary, fontSize = 11.sp)
        } else {
            patches.forEach { patch ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                    border = BorderStroke(1.dp, SupernovaPurple.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = patch.title, fontWeight = FontWeight.Bold, color = StarlightPrimary)
                            StatusBadge(status = patch.status)
                        }
                        Text(text = "Target: ${patch.targetFile}", fontSize = 11.sp, color = StarlightSecondary)
                        Text(text = patch.description, fontSize = 11.sp, color = StarlightTertiary)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = patch.patchDiff,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = OrbitEmeraldGlow,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        if (patch.status == "PROPOSED") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.reviewPatch(patch.id, approve = false) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Reject", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.reviewPatch(patch.id, approve = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrbitEmerald)
                                ) {
                                    Text("Approve & Stage", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. CONTROLS SECTION
// -------------------------------------------------------------
@Composable
private fun ControlsSectionView(viewModel: MainViewModel) {
    val settings by viewModel.learningSettings.collectAsState()
    val candidates by viewModel.learningCandidates.collectAsState()

    val isAutoEnabled = settings?.isAutoLearningEnabled ?: true
    val isWebEnabled = settings?.isWebLearningEnabled ?: true
    val isMemoryEnabled = settings?.isMemoryExtractionEnabled ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Autonomous Learning Engine Controls",
            style = MaterialTheme.typography.titleSmall,
            color = CosmicCyanGlow,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
            border = BorderStroke(1.dp, SpaceCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ControlSwitchRow(
                    title = "Automatic Learning Engine",
                    subtitle = "Learn from conversations, user context, and answers continuously.",
                    checked = isAutoEnabled,
                    onCheckedChange = { viewModel.toggleAutoLearning(it) }
                )
                HorizontalDivider(color = SpaceCardBorder.copy(alpha = 0.4f))
                ControlSwitchRow(
                    title = "Web Learning (Browser Research)",
                    subtitle = "Allow autonomous research on Wikipedia, DuckDuckGo, and live feeds.",
                    checked = isWebEnabled,
                    onCheckedChange = { viewModel.toggleWebLearning(it) }
                )
                HorizontalDivider(color = SpaceCardBorder.copy(alpha = 0.4f))
                ControlSwitchRow(
                    title = "Automatic Memory Extraction",
                    subtitle = "Extract preferences, skills, and projects without storing raw logs.",
                    checked = isMemoryEnabled,
                    onCheckedChange = { viewModel.toggleMemoryExtraction(it) }
                )
            }
        }

        // Maintenance & Queue Actions
        Text(text = "Task Queue & Memory Maintenance", fontWeight = FontWeight.Bold, color = CosmicCyanGlow, fontSize = 12.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    viewModel.enqueueTask("DUPLICATE_DETECTION", "Scan Duplicate Knowledge", "Detect redundant facts and cluster concepts.")
                    viewModel.runNextQueuedTask()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Deduplicate", fontSize = 11.sp)
            }

            FilledTonalButton(
                onClick = {
                    viewModel.enqueueTask("MEMORY_CLEANUP", "Clean Expired Memories", "Prune orphaned temporary conversation context.")
                    viewModel.runNextQueuedTask()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cleanup", fontSize = 11.sp)
            }

            FilledTonalButton(
                onClick = {
                    viewModel.enqueueTask("SEMANTIC_INDEXING", "Refresh Semantic Index", "Recalculate symbol embeddings.")
                    viewModel.runNextQueuedTask()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Reindex", fontSize = 11.sp)
            }
        }

        // Review Pending Candidates
        Text(text = "Pending Learning Candidates (${candidates.size})", fontWeight = FontWeight.Bold, color = CosmicCyanGlow, fontSize = 12.sp)
        if (candidates.isEmpty()) {
            Text(text = "No candidates awaiting review.", color = StarlightTertiary, fontSize = 11.sp)
        } else {
            candidates.forEach { cand ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
                    border = BorderStroke(1.dp, SpaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = cand.title, fontWeight = FontWeight.Bold, color = StarlightPrimary, fontSize = 12.sp)
                            StatusBadge(status = cand.status)
                        }
                        Text(text = cand.extractedContent, fontSize = 11.sp, color = StarlightSecondary)
                        if (cand.status == "PENDING_REVIEW") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { viewModel.reviewCandidate(cand.id, approve = false) }) {
                                    Text("Reject", color = CrimsonError, fontSize = 11.sp)
                                }
                                TextButton(onClick = { viewModel.reviewCandidate(cand.id, approve = true) }) {
                                    Text("Approve to Shared Knowledge", color = OrbitEmeraldGlow, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Danger Zone: Clear Memories
        OutlinedButton(
            onClick = { viewModel.clearAllMemories() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Eligible User Memories", fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPOSABLES
// -------------------------------------------------------------
@Composable
private fun MetricStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardSurface),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, fontSize = 10.sp, color = StarlightSecondary)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, text) = when (status.uppercase()) {
        "RUNNING" -> OrbitEmerald to "Running"
        "PAUSED" -> AmberWarning to "Paused"
        "PENDING_REVIEW", "WAITING FOR APPROVAL" -> CosmicCyan to "Waiting Review"
        "PROPOSED" -> AmberWarning to "Awaiting Approval"
        "COMPLETED", "APPROVED", "PASSED" -> OrbitEmerald to "Completed"
        "FAILED", "REJECTED" -> CrimsonError to "Failed"
        else -> StarlightSecondary to status
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FeatureStatusItem(label: String, enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 10.sp, color = StarlightSecondary)
        Text(
            text = if (enabled) "ENABLED" else "DISABLED",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) OrbitEmeraldGlow else AmberWarning
        )
    }
}

@Composable
private fun ControlSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = StarlightPrimary, fontSize = 13.sp)
            Text(text = subtitle, color = StarlightSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrbitEmerald,
                uncheckedThumbColor = StarlightSecondary,
                uncheckedTrackColor = SpaceCardSurface
            )
        )
    }
}
