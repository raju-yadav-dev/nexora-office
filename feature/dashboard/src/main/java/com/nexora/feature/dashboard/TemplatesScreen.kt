package com.nexora.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile

@Composable
fun TemplatesScreen(
    onOpenTemplate: (WorkspaceFile) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var previewTemplate by remember { mutableStateOf<Template?>(null) }
    val templates = listOf(
        Template("Resume", "Resume", "Doc", DocumentType.DOC, Color(0xFF2563EB), listOf(Color.White, Color(0xFFE8ECF8))),
        Template("Project Report", "Reports", "PDF", DocumentType.PDF, Color(0xFF1D4ED8), listOf(Color(0xFF0F2557), Color(0xFF3B82F6))),
        Template("Business Plan", "Business", "Doc", DocumentType.DOC, Color(0xFF2563EB), listOf(Color.White, Color(0xFFF2F4F7))),
        Template("Agreement", "Business", "PDF", DocumentType.PDF, NexoraError, listOf(Color.White, Color(0xFFFFE4E6))),
        Template("Class Notes", "Notes", "TXT", DocumentType.TEXT, Color(0xFF14B8A6), listOf(Color.White, Color(0xFFE0F2FE))),
        Template("Invoice", "Invoice", "Sheet", DocumentType.SHEET, NexoraSecondary, listOf(Color.White, Color(0xFFE0FBEF))),
        Template("Study Planner", "Education", "Doc", DocumentType.DOC, Color(0xFF7C3AED), listOf(Color.White, Color(0xFFF3E8FF))),
        Template("Sales Tracker", "Business", "Sheet", DocumentType.SHEET, NexoraSecondary, listOf(Color.White, Color(0xFFE0FBEF))),
        Template("Pitch Deck", "Presentation", "PPT", DocumentType.SLIDE, Color(0xFFF97316), listOf(Color(0xFF341A12), Color(0xFFB45309))),
        Template("Budget Sheet", "Spreadsheet", "Sheet", DocumentType.SHEET, NexoraSecondary, listOf(Color.White, Color(0xFFDCFCE7)))
    )
    val visibleTemplates = templates.filter { template ->
        (selectedCategory == "All" || template.category == selectedCategory) &&
            (searchQuery.isBlank() || template.title.contains(searchQuery, ignoreCase = true))
    }

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
                    NexoraSearchField(
                        placeholder = "Search templates",
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Resume", "Business", "Reports", "Notes", "Invoice", "Education", "Spreadsheet", "Presentation").forEach { category ->
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
                NexoraSectionHeader(
                    title = "Featured Templates",
                    action = "Reset",
                    onAction = {
                        selectedCategory = "All"
                        searchQuery = ""
                    }
                )
            }

            item {
                TemplateGrid(
                    templates = visibleTemplates.take(3),
                    onOpenTemplate = onOpenTemplate,
                    onPreviewTemplate = { previewTemplate = it }
                )
            }

            item {
                NexoraSectionHeader(
                    title = "Trending",
                    action = if (visibleTemplates.size > 3) "Show all" else null,
                    onAction = {
                        selectedCategory = "All"
                    }
                )
            }

            item {
                TemplateGrid(
                    templates = visibleTemplates.drop(3).ifEmpty { visibleTemplates.take(3) },
                    onOpenTemplate = onOpenTemplate,
                    onPreviewTemplate = { previewTemplate = it }
                )
            }

            item {
                NexoraSectionHeader(title = "Recent Templates")
                TemplateGrid(
                    templates = templates.takeLast(3),
                    onOpenTemplate = onOpenTemplate,
                    onPreviewTemplate = { previewTemplate = it }
                )
            }
        }
        previewTemplate?.let { template ->
            TemplatePreviewSheet(
                template = template,
                onDismiss = { previewTemplate = null },
                onUseTemplate = {
                    previewTemplate = null
                    onOpenTemplate(template.toWorkspaceFile())
                }
            )
        }
    }
}

@Composable
private fun TemplateGrid(
    templates: List<Template>,
    onOpenTemplate: (WorkspaceFile) -> Unit,
    onPreviewTemplate: (Template) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (templates.isEmpty()) {
            NexoraCard(contentPadding = 14.dp) {
                Text(
                    text = "No templates found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        templates.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { template ->
                    TemplateCard(
                        template = template,
                        modifier = Modifier.weight(1f),
                        onClick = { onPreviewTemplate(template) }
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
private fun TemplatePreviewSheet(
    template: Template,
    onDismiss: () -> Unit,
    onUseTemplate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        NexoraCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = 16.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NexoraIconBadge(label = template.badge, color = template.accent, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(template.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NexoraPill(label = "Favorite", selected = false)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(template.previewColors))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.align(Alignment.CenterStart)) {
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (index == 0) 0.56f else 0.82f)
                                .height(if (index == 0) 12.dp else 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(template.accent.copy(alpha = if (index == 0) 0.42f else 0.22f))
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NexoraPill(label = "Close", selected = false, modifier = Modifier.weight(1f), onClick = onDismiss)
                NexoraPill(label = "Use Template", selected = true, modifier = Modifier.weight(1f), onClick = onUseTemplate)
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
    val category: String,
    val badge: String,
    val type: DocumentType,
    val accent: Color,
    val previewColors: List<Color>
)

private fun Template.toWorkspaceFile(): WorkspaceFile {
    val extension = when (type) {
        DocumentType.DOC -> "docx"
        DocumentType.SHEET -> "xlsx"
        DocumentType.SLIDE -> "pptx"
        DocumentType.PDF -> "pdf"
        DocumentType.IMAGE -> "png"
        DocumentType.TEXT -> "txt"
    }
    return WorkspaceFile(
        name = "$title.$extension",
        path = "nexora://templates/${title.lowercase().replace(" ", "-")}",
        type = type,
        sizeLabel = "Template"
    )
}
