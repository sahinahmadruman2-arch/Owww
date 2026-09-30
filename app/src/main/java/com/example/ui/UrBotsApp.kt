package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.analysis.AnalysisScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.knowledge.KnowledgeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.teach.TeachScreen
import com.example.ui.screens.training.TrainingScreen

data class NavTabItem(
    val tab: UrBotsTab,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrBotsApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Handle system back navigation to return to Chat if on other screens
    if (currentTab != UrBotsTab.CHAT) {
        BackHandler {
            viewModel.setTab(UrBotsTab.CHAT)
        }
    }

    val tabItems = listOf(
        NavTabItem(UrBotsTab.CHAT, "Chat", Icons.Default.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        NavTabItem(UrBotsTab.TEACH, "Teach", Icons.Default.School, Icons.Outlined.School),
        NavTabItem(UrBotsTab.KNOWLEDGE, "Knowledge", Icons.Default.AutoStories, Icons.Outlined.AutoStories),
        NavTabItem(UrBotsTab.LIBRARY, "Library", Icons.Default.Folder, Icons.Outlined.Folder),
        NavTabItem(UrBotsTab.ANALYSIS, "Analysis", Icons.Default.Hub, Icons.Outlined.Hub),
        NavTabItem(UrBotsTab.TRAINING, "Training", Icons.Default.ModelTraining, Icons.Outlined.ModelTraining)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UrBots7",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = currentTab.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabItems.forEach { item ->
                    val isSelected = currentTab == item.tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(item.tab) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        modifier = Modifier.testTag("nav_tab_${item.title.lowercase()}")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                UrBotsTab.CHAT -> ChatScreen(viewModel = viewModel)
                UrBotsTab.TEACH -> TeachScreen(viewModel = viewModel)
                UrBotsTab.KNOWLEDGE -> KnowledgeScreen(viewModel = viewModel)
                UrBotsTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                UrBotsTab.ANALYSIS -> AnalysisScreen(viewModel = viewModel)
                UrBotsTab.TRAINING -> TrainingScreen(viewModel = viewModel)
            }
        }
    }
}
