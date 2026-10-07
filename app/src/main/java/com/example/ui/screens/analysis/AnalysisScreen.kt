package com.example.ui.screens.analysis

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.KnowledgeConceptEntity
import com.example.data.local.entity.KnowledgeFactEntity
import com.example.data.local.entity.KnowledgeRelationshipEntity
import com.example.domain.ai.TokenAnalysisInfo
import com.example.domain.ai.TokenizerInspector
import com.example.ui.DashboardStats
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AnalysisFilter(val label: String) {
    ALL("Cosmic Overview"),
    TOKEN_LOGITS("Word Tokens (A=1)"),
    TREE("Visual Tree"),
    CONCEPTS("Concepts"),
    FACTS("Facts"),
    RELATIONSHIPS("Relationships"),
    VARIATIONS("Variations"),
    CONFLICTS("Conflicts")
}

@Composable
fun AnalysisScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val concepts by viewModel.concepts.collectAsState()
    val facts by viewModel.facts.collectAsState()
    val relationships by viewModel.relationships.collectAsState()
    val versions by viewModel.versions.collectAsState()
    val runs by viewModel.trainingRuns.collectAsState()
    var selectedFilter by remember { mutableStateOf(AnalysisFilter.ALL) }

    val lastRunTime = remember(runs) {
        val last = runs.firstOrNull()?.completedAt ?: runs.firstOrNull()?.startedAt
        if (last != null) SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(last))
        else "Recently"
    }

    // Interactive Token Test State
    var sampleSentence by remember { mutableStateOf("AI models predict next words with probability numbers") }
    val tokenInfo = remember(sampleSentence) {
        TokenizerInspector.analyzeText(sampleSentence)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Space Analysis Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SpaceCard,
                border = BorderStroke(1.dp, CosmicCyan),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = null,
                        tint = CosmicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Astrophysical Knowledge & Tokens",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StarlightPrimary
                )
                Text(
                    text = "Semantic graph, token probability numbers (A=1), & grounded AST",
                    style = MaterialTheme.typography.bodySmall,
                    color = StarlightSecondary
                )
            }
        }

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(AnalysisFilter.entries.toTypedArray()) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SupernovaPurpleContainer,
                        selectedLabelColor = CosmicCyanGlow,
                        containerColor = SpaceCard,
                        labelColor = StarlightSecondary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedFilter == filter) CosmicCyan else SpaceCardBorder
                    )
                )
            }
        }

        // List & Dashboard
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Interactive Token Probabilities & Alphabetical Codes Feature Card (A=1)
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.TOKEN_LOGITS) {
                item {
                    TokenLogitsInteractiveCard(
                        sampleSentence = sampleSentence,
                        tokenInfo = tokenInfo,
                        onSentenceChange = { sampleSentence = it }
                    )
                }
            }

            // Dashboard Metrics Overview Card
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.TREE) {
                item {
                    SpaceDashboardMetricsCard(
                        stats = stats,
                        versionStr = "v${versions.firstOrNull()?.versionNumber ?: 1}",
                        lastRunTime = lastRunTime
                    )
                }

                item {
                    VisualKnowledgeTreeCard()
                }
            }

            // Concepts Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.CONCEPTS) {
                item {
                    SectionHeader(
                        title = "Extracted Concepts (${concepts.size})",
                        icon = Icons.Default.Category
                    )
                }
                items(concepts, key = { "concept_${it.id}" }) { concept ->
                    ConceptCard(concept = concept)
                }
            }

            // Relationships Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.RELATIONSHIPS) {
                item {
                    SectionHeader(
                        title = "Semantic Relationships (${relationships.size})",
                        icon = Icons.Default.Share
                    )
                }
                items(relationships, key = { "rel_${it.id}" }) { rel ->
                    RelationshipCard(relationship = rel)
                }
            }

            // Facts Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.FACTS) {
                item {
                    SectionHeader(
                        title = "Extracted Facts (${facts.size})",
                        icon = Icons.Default.FormatQuote
                    )
                }
                items(facts, key = { "fact_${it.id}" }) { fact ->
                    FactCard(fact = fact)
                }
            }

            // Variations Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.VARIATIONS) {
                item {
                    QuestionVariationsShowcase()
                }
            }

            // Conflicts Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.CONFLICTS) {
                item {
                    ConflictsInfoCard()
                }
            }
        }
    }
}

/**
 * Interactive Token Logit & Probability Inspector (A=1, B=2, Softmax Rankings)
 */
@Composable
fun TokenLogitsInteractiveCard(
    sampleSentence: String,
    tokenInfo: TokenAnalysisInfo,
    onSentenceChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, Brush.horizontalGradient(CardBorderGradient)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = SolarFlareGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "HOW OTHER AI WORKS: TOKEN NUMBERS & PROBABILITIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SolarFlareGold,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SolarFlareContainer
                ) {
                    Text(
                        text = "A = 1, B = 2 ... Z = 26",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarFlareGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Modern LLMs (like GPT, Gemini, Llama) don't read English directly. They convert every word into Token IDs, assign alphabetical/numerical rankings, and predict the next token by calculating probability distributions via softmax logit scores.",
                fontSize = 11.sp,
                color = StarlightSecondary,
                lineHeight = 16.sp
            )

            // Sentence Input Field to experiment live
            OutlinedTextField(
                value = sampleSentence,
                onValueChange = onSentenceChange,
                label = { Text("Test any sentence live", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicCyan,
                    unfocusedBorderColor = SpaceCardBorder,
                    focusedTextColor = StarlightPrimary,
                    unfocusedTextColor = StarlightPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Horizontal token stream view
            Text(
                text = "TOKEN STREAM & LETTER CODES:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = StarlightTertiary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tokenInfo.tokens) { tok ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SpaceSurface,
                        border = BorderStroke(1.dp, SolarFlareGold.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tok.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = StarlightPrimary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Code #${tok.alphabeticalNumber}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarFlareGold
                                )
                                Text(
                                    text = "ID: ${tok.tokenId}",
                                    fontSize = 8.sp,
                                    color = StarlightTertiary
                                )
                            }
                            Text(
                                text = "prob ${(tok.probability * 100).toInt()}%",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrbitEmerald
                            )
                        }
                    }
                }
            }

            // Top predictions showcase
            if (tokenInfo.topAlternatives.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SpaceSurface,
                    border = BorderStroke(1.dp, SpaceCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Next-Word Softmax Ranking (Top Predicted Tokens):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicCyan
                        )
                        tokenInfo.topAlternatives.take(3).forEachIndexed { i, alt ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${i + 1} Word: \"${alt.token}\" (Letter #${TokenizerInspector.letterToNumber(alt.token.firstOrNull() ?: 'A')})",
                                    fontSize = 11.sp,
                                    color = StarlightPrimary
                                )
                                Text(
                                    text = "p=${String.format(Locale.US, "%.1f", alt.probability * 100)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (i == 0) OrbitEmerald else StarlightSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceDashboardMetricsCard(stats: DashboardStats, versionStr: String, lastRunTime: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEEP SPACE KNOWLEDGE METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SupernovaPurpleContainer
                ) {
                    Text(
                        text = versionStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicCyanGlow,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Documents", stats.documents.toString())
                MetricItem("Chunks", stats.chunks.toString())
                MetricItem("Concepts", stats.concepts.toString())
                MetricItem("Facts", stats.facts.toString())
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Relations", stats.relationships.toString())
                MetricItem("Live Web", stats.liveFeeds.toString())
                MetricItem("Autonomous", "100% Sync")
                MetricItem("Last Analyzed", lastRunTime)
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = StarlightTertiary, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = StarlightPrimary)
    }
}

@Composable
fun VisualKnowledgeTreeCard() {
    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = CosmicCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Visual Knowledge Structure (AST)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StarlightPrimary
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = StarlightSecondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TreeLevelItem(level = 0, label = "Root Corpus: UrBots7 Cosmic Knowledge Base")
                    TreeLevelItem(level = 1, label = "Category 1: Social Greetings & Communication")
                    TreeLevelItem(level = 2, label = "Concept: 'Hi' → Casual greeting, reply options")
                    TreeLevelItem(level = 2, label = "Concept: 'Hello' → Universal greeting")
                    TreeLevelItem(level = 1, label = "Category 2: Real-Time Web Research & Feeds")
                    TreeLevelItem(level = 2, label = "YouTube Transcripts & Summaries (Room DB)")
                    TreeLevelItem(level = 2, label = "Reddit Live Posts & Discussion Threads")
                    TreeLevelItem(level = 2, label = "Live DuckDuckGo / Wikipedia Web Grounding")
                    TreeLevelItem(level = 1, label = "Category 3: Transformer Tokenizer (A=1 Word Ranking)")
                }
            }
        }
    }
}

@Composable
fun TreeLevelItem(level: Int, label: String) {
    val indent = (level * 16).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (level == 0) CosmicCyan else if (level == 1) SupernovaPurple else SolarFlareGold)
        )
        Text(
            text = label,
            fontSize = if (level == 0) 12.sp else 11.sp,
            fontWeight = if (level <= 1) FontWeight.Bold else FontWeight.Normal,
            color = if (level == 0) StarlightPrimary else if (level == 1) CosmicCyanGlow else StarlightSecondary
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmicCyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = StarlightPrimary
        )
    }
}

@Composable
fun ConceptCard(concept: KnowledgeConceptEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SupernovaPurpleContainer
                ) {
                    Text(
                        text = concept.topic,
                        style = MaterialTheme.typography.labelSmall,
                        color = SupernovaPurple,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = concept.definition,
                style = MaterialTheme.typography.bodySmall,
                color = StarlightSecondary
            )
        }
    }
}

@Composable
fun RelationshipCard(relationship: KnowledgeRelationshipEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = CosmicCyanContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CosmicCyan
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = relationship.fromConcept,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = StarlightPrimary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CosmicCyan
                    )
                    Text(
                        text = relationship.toConcept,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = StarlightPrimary
                    )
                }
                Text(
                    text = "Type: ${relationship.relationshipType} • ${relationship.description}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StarlightTertiary
                )
            }
        }
    }
}

@Composable
fun FactCard(fact: KnowledgeFactEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = fact.statement,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = StarlightPrimary
            )
            Text(
                text = "Topic: ${fact.topic}",
                style = MaterialTheme.typography.labelSmall,
                color = StarlightTertiary
            )
        }
    }
}

@Composable
fun QuestionVariationsShowcase() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, SpaceCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Systematically Generated Question Variations:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SupernovaPurple
            )
            Text(
                text = "For every fact, UrBots7 creates varied linguistic representations so users asking with different vocabulary get accurate answers:\n" +
                        "• \"What should I reply when someone says hi?\"\n" +
                        "• \"Someone greeted me. What can I say?\"\n" +
                        "• \"How can I respond if someone says hi?\"\n" +
                        "• \"Is hi casual?\"\n" +
                        "• \"What is the difference between hi and hello?\"\n" +
                        "• \"What can I say when meeting someone for the first time?\"",
                style = MaterialTheme.typography.bodySmall,
                color = StarlightSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ConflictsInfoCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = BorderStroke(1.dp, MarsCrimson.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Conflict Resolution Protocol:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MarsCrimson
            )
            Text(
                text = "If multiple ingested documents present conflicting statements (e.g. Doc A claims 'X is always Y' while Doc B claims 'X is never Y'), UrBots7 preserves both perspectives and formulates the response:\n" +
                        "\"Source A says X, while Source B says Y.\" No silent overwrite or artificial merge occurs.",
                style = MaterialTheme.typography.bodySmall,
                color = StarlightSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
