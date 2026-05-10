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

@Composable
fun ToolsScreen(
    onOpenEditor: () -> Unit
) {
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
                    NexoraSearchField(placeholder = "Search PDF, image, scan, convert...")
                }
            }

            item {
                NexoraCard(
                    color = NexoraPrimary.copy(alpha = 0.18f),
                    contentPadding = 15.dp,
                    onClick = onOpenEditor
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
                ToolSection(title = "PDF Tools", tools = pdfTools, onOpenEditor = onOpenEditor)
            }
            item {
                ToolSection(title = "Image Tools", tools = imageTools, onOpenEditor = onOpenEditor)
            }
            item {
                ToolSection(title = "File Tools", tools = fileTools, onOpenEditor = onOpenEditor)
            }
        }
    }
}

@Composable
private fun ToolSection(
    title: String,
    tools: List<ActionItem>,
    onOpenEditor: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NexoraSectionHeader(title = title)
        NexoraActionGrid(items = tools, onClick = { onOpenEditor() })
        Spacer(Modifier.height(2.dp))
    }
}
