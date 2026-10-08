package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.analysis.AnalysisScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.feeds.LiveFeedsScreen
import com.example.ui.screens.knowledge.KnowledgeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.teach.TeachScreen
import com.example.ui.screens.training.TrainingScreen
import com.example.ui.theme.*

data class NavTabItem(
    val tab: UrBotsTab,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeText: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrBotsApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val versions by viewModel.versions.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val latestVersion = versions.firstOrNull()?.versionNumber ?: 1

    // Handle system back navigation to return to Chat if on secondary screens
    if (currentTab != UrBotsTab.CHAT) {
        BackHandler {
            viewModel.setTab(UrBotsTab.CHAT)
        }
    }

    val tabItems = listOf(
        NavTabItem(UrBotsTab.CHAT, "Cosmos", Icons.Default.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        NavTabItem(UrBotsTab.FEEDS, "Live Orbit", Icons.Default.RssFeed, Icons.Outlined.RssFeed, badgeText = if (stats.liveFeeds > 0) "${stats.liveFeeds}" else null),
        NavTabItem(UrBotsTab.TEACH, "Teach", Icons.Default.School, Icons.Outlined.School, badgeText = if (stats.manualTraining > 0) "${stats.manualTraining}" else null),
        NavTabItem(UrBotsTab.KNOWLEDGE, "Archives", Icons.Default.AutoStories, Icons.Outlined.AutoStories, badgeText = if (stats.documents > 0) "${stats.documents}" else null),
        NavTabItem(UrBotsTab.ANALYSIS, "Tokens & AST", Icons.Default.Hub, Icons.Outlined.Hub),
        NavTabItem(UrBotsTab.LIBRARY, "Library", Icons.Default.Folder, Icons.Outlined.Folder),
        NavTabItem(UrBotsTab.TRAINING, "Neural Lab", Icons.Default.ModelTraining, Icons.Outlined.ModelTraining),
        NavTabItem(UrBotsTab.SETTINGS, "Station", Icons.Default.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        topBar = {
            Surface(
                color = SpaceSurface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradientSubtle))
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sleek Karllo Monogram Emblem
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(StarlightGlowGradient))
                                    .shadow(elevation = 8.dp, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "K",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Karllo",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StarlightPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    // Deep Space Active Status Pill
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = OrbitEmeraldContainer.copy(alpha = 0.85f),
                                        border = BorderStroke(1.dp, OrbitEmerald.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(OrbitEmerald)
                                            )
                                            Text(
                                                text = "ORBIT ONLINE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = OrbitEmeraldGlow,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Space AI • Live Browser Research • Words [A=1] • v$latestVersion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StarlightSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        // Current Station Module Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SupernovaPurpleContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, SupernovaPurple.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = currentTab.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CosmicCyanGlow,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            // High-Tech Deep Space Dock
            Surface(
                color = SpaceSurface,
                tonalElevation = 10.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(SpaceCardBorder, SupernovaPurple.copy(alpha = 0.3f), SpaceCardBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    val navScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(navScrollState)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("main_bottom_nav"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabItems.forEach { item ->
                            val isSelected = currentTab == item.tab

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) SupernovaPurpleContainer.copy(alpha = 0.9f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, CosmicCyan.copy(alpha = 0.8f)) else null,
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setTab(item.tab) }
                                    .testTag("nav_tab_${item.title.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .defaultMinSize(minHeight = 44.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeText != null) {
                                                Badge(
                                                    containerColor = if (isSelected) OrbitEmerald else SupernovaPurple,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(text = item.badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title,
                                            tint = if (isSelected) CosmicCyanGlow else StarlightSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        Text(
                                            text = item.title,
                                            maxLines = 1,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StarlightPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(SpaceNebulaGradient))
                .padding(innerPadding)
        ) {
            when (currentTab) {
                UrBotsTab.CHAT -> ChatScreen(viewModel = viewModel)
                UrBotsTab.FEEDS -> LiveFeedsScreen(viewModel = viewModel)
                UrBotsTab.TEACH -> TeachScreen(viewModel = viewModel)
                UrBotsTab.KNOWLEDGE -> KnowledgeScreen(viewModel = viewModel)
                UrBotsTab.ANALYSIS -> AnalysisScreen(viewModel = viewModel)
                UrBotsTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                UrBotsTab.TRAINING -> TrainingScreen(viewModel = viewModel)
                UrBotsTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
