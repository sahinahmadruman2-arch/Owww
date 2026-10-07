package com.example.ui.screens.feeds

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.LiveFeedEntryEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveFeedsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val liveFeeds by viewModel.liveFeeds.collectAsState()
    val searchInput by viewModel.liveFeedSearchInput.collectAsState()
    val isSearching by viewModel.isLiveSearching.collectAsState()
    val searchStatus by viewModel.liveSearchStatus.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, REDDIT, YOUTUBE

    val trendingTopics = listOf(
        "Artificial Intelligence Breakthroughs",
        "What does exhaustion mean and how to cure it?",
        "Quantum Computing Explained",
        "Roblox Engine & Optimization",
        "Kotlin Compose Best Practices"
    )

    val filteredFeeds = remember(liveFeeds, selectedFilter) {
        when (selectedFilter) {
            "REDDIT" -> liveFeeds.filter { it.sourceType == "REDDIT" }
            "YOUTUBE" -> liveFeeds.filter { it.sourceType == "YOUTUBE" }
            else -> liveFeeds
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
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
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.RssFeed,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
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
                                text = "Live Reddit & YouTube Feeds",
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
                                    text = "AUTONOMOUS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonEmeraldGlow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Live community intelligence harvested directly from Reddit discussions and YouTube video knowledge. Autonomously saved into Room so you never need to train!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Live Search Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { viewModel.updateLiveFeedSearch(it) },
                    placeholder = { Text("Search Reddit & YouTube live topics...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_feed_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.TravelExplore, contentDescription = null, tint = ElectricCyan)
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

                Button(
                    onClick = { viewModel.searchLiveWeb() },
                    enabled = searchInput.isNotBlank() && !isSearching,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("search_live_feed_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Quick Trending Topics Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1-Click Auto-Ingest Trending Topics:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(trendingTopics) { topic ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianCardBorder),
                            modifier = Modifier.clickable {
                                viewModel.harvestTrendingTopic(topic)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                                Text(text = topic, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Status Card during active harvesting
        if (searchStatus != null || isSearching) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ElectricCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = searchStatus ?: "Syncing live intelligence...",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyanGlow,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Filter Pills & Clear Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${liveFeeds.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "REDDIT",
                        onClick = { selectedFilter = "REDDIT" },
                        label = { Text("Reddit (${liveFeeds.count { it.sourceType == "REDDIT" }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "YOUTUBE",
                        onClick = { selectedFilter = "YOUTUBE" },
                        label = { Text("YouTube (${liveFeeds.count { it.sourceType == "YOUTUBE" }})", fontSize = 11.sp) }
                    )
                }

                if (liveFeeds.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearLiveFeeds() }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear all", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Live Feed Cards List
        if (filteredFeeds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.SensorsOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(46.dp))
                        Text(
                            text = "No live feeds fetched yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Type a search term above or tap a trending topic to automatically harvest Reddit & YouTube into UrBots7's persistent brain.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            items(filteredFeeds, key = { it.id }) { item ->
                LiveFeedCard(
                    item = item,
                    onOpenUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    onDelete = { viewModel.deleteLiveFeed(item.id) }
                )
            }
        }
    }
}

@Composable
fun LiveFeedCard(
    item: LiveFeedEntryEntity,
    onOpenUrl: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isReddit = item.sourceType == "REDDIT"
    val sourceBadgeColor = if (isReddit) Color(0xFFFF4500) else Color(0xFFFF0000)

    val formattedDate = remember(item.createdAt) {
        SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(item.createdAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, Brush.linearGradient(CardBorderGradientSubtle)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Source & Channel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = sourceBadgeColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, sourceBadgeColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = if (isReddit) "REDDIT" else "YOUTUBE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = sourceBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = item.authorOrChannel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonEmeraldContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonEmeraldGlow, modifier = Modifier.size(10.dp))
                            Text(text = "AUTO-INGESTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonEmeraldGlow)
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Thumbnail for YouTube or Image Posts
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            // Snippet
            if (item.content.isNotBlank()) {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 3,
                    lineHeight = 16.sp
                )
            }

            // Metrics & Analysis Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.metricText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianCardBorder)
                        ) {
                            Text(
                                text = item.metricText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianCardBorder)
                    ) {
                        Text(
                            text = "${item.credibilityScore}% EVIDENCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmeraldGlow,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.url.isNotBlank()) {
                    TextButton(
                        onClick = { onOpenUrl(item.url) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "Open Source", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = ElectricCyan)
                    }
                }
            }
        }
    }
}
