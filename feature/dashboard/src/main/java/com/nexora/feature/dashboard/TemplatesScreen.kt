package com.nexora.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraGradientBackground
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraSearchField
import com.nexora.core.designsystem.component.NexoraSectionHeader
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraSecondary

@Composable
fun TemplatesScreen(
    onOpenTemplate: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val templates = listOf(
        Template("Resume", "Doc", Color(0xFF2563EB), listOf(Color.White, Color(0xFFE8ECF8))),
        Template("Project Report", "PDF", Color(0xFF1D4ED8), listOf(Color(0xFF0F2557), Color(0xFF3B82F6))),
        Template("Business Plan", "Doc", Color(0xFF2563EB), listOf(Color.White, Color(0xFFF2F4F7))),
        Template("Agreement", "PDF", NexoraError, listOf(Color.White, Color(0xFFFFE4E6))),
        Template("Sales Tracker", "Sheet", NexoraSecondary, listOf(Color.White, Color(0xFFE0FBEF))),
        Template("Pitch Deck", "PPT", Color(0xFFF97316), listOf(Color(0xFF341A12), Color(0xFFB45309)))
    )

    NexoraGradientBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Templates",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    NexoraSearchField(placeholder = "Search templates")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Resume", "Report", "Business").forEach { category ->
                            NexoraPill(
                                label = category,
                                selected = category == selectedCategory,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                }
            }

            item {
                NexoraSectionHeader(title = "Recommended", action = "See all")
            }

            item {
                TemplateGrid(
                    templates = templates.take(3),
                    onOpenTemplate = onOpenTemplate
                )
            }

            item {
                NexoraSectionHeader(title = "Documents", action = "See all")
            }

            item {
                TemplateGrid(
                    templates = templates.drop(3).take(3),
                    onOpenTemplate = onOpenTemplate
                )
            }
        }
    }
}

@Composable
private fun TemplateGrid(
    templates: List<Template>,
    onOpenTemplate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        templates.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { template ->
                    TemplateCard(
                        template = template,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTemplate
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(template.previewColors))
                .padding(10.dp)
        ) {
            NexoraIconBadge(
                label = template.badge,
                color = template.accent,
                size = 30.dp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 3) 0.62f else 1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(template.accent.copy(alpha = 0.24f))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = template.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = template.badge,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class Template(
    val title: String,
    val badge: String,
    val accent: Color,
    val previewColors: List<Color>
)
