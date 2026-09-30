package com.example.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.KnowledgeConceptEntity
import com.example.data.local.entity.KnowledgeFactEntity
import com.example.data.local.entity.KnowledgeRelationshipEntity
import com.example.ui.MainViewModel

enum class AnalysisFilter(val label: String) {
    ALL("All"),
    CONCEPTS("Concepts"),
    FACTS("Facts"),
    RELATIONSHIPS("Relationships"),
    VARIATIONS("Question Variations"),
    CONFLICTS("Conflicts & Ambiguities")
}

@Composable
fun AnalysisScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val concepts by viewModel.concepts.collectAsState()
    val facts by viewModel.facts.collectAsState()
    val relationships by viewModel.relationships.collectAsState()
    var selectedFilter by remember { mutableStateOf(AnalysisFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Hub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Knowledge Graph & Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${concepts.size} concepts • ${facts.size} facts • ${relationships.size} relationships",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    label = { Text(filter.label) }
                )
            }
        }

        // List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
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
                    RelationshipCard(rel = rel)
                }
            }

            // Facts Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.FACTS) {
                item {
                    SectionHeader(
                        title = "Structured Facts & Statements (${facts.size})",
                        icon = Icons.Default.Lightbulb
                    )
                }
                items(facts, key = { "fact_${it.id}" }) { fact ->
                    FactCard(fact = fact)
                }
            }

            // Question Variations Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.VARIATIONS) {
                item {
                    SectionHeader(
                        title = "Semantic Question Variations",
                        icon = Icons.Default.HelpOutline
                    )
                }
                item {
                    QuestionVariationsShowcase()
                }
            }

            // Conflicts & Ambiguities Section
            if (selectedFilter == AnalysisFilter.ALL || selectedFilter == AnalysisFilter.CONFLICTS) {
                item {
                    SectionHeader(
                        title = "Conflicts & Ambiguity Management",
                        icon = Icons.Default.WarningAmber
                    )
                }
                item {
                    ConflictsInfoCard()
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ConceptCard(concept: KnowledgeConceptEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(concept.topic, fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = concept.definition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (concept.examplesJson != "[]" && concept.examplesJson.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Examples: ${concept.examplesJson.replace("[\"", "").replace("\"]", "").replace("\",\"", ", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RelationshipCard(rel: KnowledgeRelationshipEntity) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = rel.fromConcept,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = rel.relationshipType.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = rel.toConcept,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun FactCard(fact: KnowledgeFactEntity) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fact.statement,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(fact.category, fontSize = 10.sp) }
                )
            }
        }
    }
}

@Composable
fun QuestionVariationsShowcase() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Systematically Generated Question Variations:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ConflictsInfoCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Conflict Resolution Protocol:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "If multiple ingested documents present conflicting statements (e.g. Doc A claims 'X is always Y' while Doc B claims 'X is never Y'), UrBots7 preserves both perspectives and formulates the response:\n" +
                        "\"Source A says X, while Source B says Y.\" No silent overwrite or artificial merge occurs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
