package com.nexora.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.core.designsystem.component.ActionItem
import com.nexora.core.designsystem.component.NexoraActionGrid
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraGradientBackground
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.component.NexoraSearchField
import com.nexora.core.designsystem.component.NexoraSectionHeader
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile

@Composable
fun ToolsScreen(
    onOpenEditor: (WorkspaceFile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val pdfTools = listOf(
        ActionItem("Merge PDF", "M", NexoraError),
        ActionItem("Split PDF", "S", NexoraError),
        ActionItem("Compress", "C", NexoraError),
        ActionItem("PDF to Word", "W", Color(0xFF2563EB))
    )
    val imageTools = listOf(
        ActionItem("Image to PDF", "IP", Color(0xFF2563EB)),
        ActionItem("PDF to Excel", "X", NexoraSecondary),
        ActionItem("PDF to PPT", "P", Color(0xFFF97316)),
        ActionItem("Scan OCR", "OCR", NexoraPrimary)
    )
    val fileTools = listOf(
        ActionItem("Converter", "CV", NexoraSecondary),
        ActionItem("Compress", "Z", NexoraPrimaryVariant),
        ActionItem("Extract Text", "TXT", Color(0xFF0EA5E9)),
        ActionItem("More", "+", MaterialTheme.colorScheme.onSurfaceVariant)
    )
    val matchesSearch: (ActionItem) -> Boolean = { item ->
        searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
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
                        text = "Tools",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    NexoraSearchField(
                        placeholder = "Search PDF, image, scan, convert...",
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )
                }
            }

            item {
                NexoraCard(
                    color = NexoraPrimary.copy(alpha = 0.18f),
                    contentPadding = 15.dp,
                    onClick = {
                        onOpenEditor(
                            WorkspaceFile(
                                name = "Productivity Suite.docx",
                                path = "nexora://tools/productivity-suite",
                                type = DocumentType.DOC,
                                sizeLabel = "Pro toolkit"
                            )
                        )
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexoraIconBadge(label = "Pro", color = NexoraPrimary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Productivity Suite",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Batch PDF, OCR and file conversion tools",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                ToolSection(title = "PDF Tools", tools = pdfTools.filter(matchesSearch), onOpenEditor = onOpenEditor)
            }
            item {
                ToolSection(title = "Image Tools", tools = imageTools.filter(matchesSearch), onOpenEditor = onOpenEditor)
            }
            item {
                ToolSection(title = "File Tools", tools = fileTools.filter(matchesSearch), onOpenEditor = onOpenEditor)
            }
        }
    }
}

@Composable
private fun ToolSection(
    title: String,
    tools: List<ActionItem>,
    onOpenEditor: (WorkspaceFile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NexoraSectionHeader(title = title)
        if (tools.isEmpty()) {
            NexoraCard(contentPadding = 12.dp) {
                Text(
                    text = "No tools found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            NexoraActionGrid(items = tools, onClick = { onOpenEditor(it.toToolFile()) })
        }
        Spacer(Modifier.height(2.dp))
    }
}

private fun ActionItem.toToolFile(): WorkspaceFile {
    val type = when {
        title.contains("PDF", ignoreCase = true) -> DocumentType.PDF
        title.contains("Excel", ignoreCase = true) -> DocumentType.SHEET
        title.contains("PPT", ignoreCase = true) -> DocumentType.SLIDE
        else -> DocumentType.DOC
    }
    val extension = when (type) {
        DocumentType.DOC -> "docx"
        DocumentType.SHEET -> "xlsx"
        DocumentType.SLIDE -> "pptx"
        DocumentType.PDF -> "pdf"
        DocumentType.TEXT -> "txt"
    }
    return WorkspaceFile(
        name = "$title.$extension",
        path = "nexora://tools/${title.lowercase().replace(" ", "-")}",
        type = type,
        sizeLabel = "Pro tool"
    )
}
