package com.nexora.feature.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraGradientBackground
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraSearchField
import com.nexora.core.designsystem.component.NexoraSectionHeader
import com.nexora.core.designsystem.component.NexoraToolbarButton
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile

@Composable
fun FileManagerScreen(
    onOpenFile: (WorkspaceFile) -> Unit
) {
    var selectedType by remember { mutableStateOf("All") }
    var gridMode by remember { mutableStateOf(false) }
    val files = listOf(
        WorkspaceFile(name = "Marketing Plan.docx", path = "/cloud/Docs", type = DocumentType.DOC),
        WorkspaceFile(name = "Sales Analysis.xlsx", path = "/cloud/Sheets", type = DocumentType.SHEET, isPinned = true),
        WorkspaceFile(name = "Product Roadmap.pptx", path = "/local/Slides", type = DocumentType.SLIDE),
        WorkspaceFile(name = "User Guide.pdf", path = "/downloads/PDF", type = DocumentType.PDF),
        WorkspaceFile(name = "Meeting Notes.docx", path = "/cloud/Docs", type = DocumentType.DOC),
        WorkspaceFile(name = "Company Profile.pptx", path = "/local/Slides", type = DocumentType.SLIDE)
    )
    val filteredFiles = if (selectedType == "All") {
        files
    } else {
        files.filter { it.type.label == selectedType }
    }

    NexoraGradientBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "All Documents",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        NexoraToolbarButton(
                            label = if (gridMode) "Grid" else "List",
                            selected = gridMode,
                            onClick = { gridMode = !gridMode }
                        )
                    }
                    NexoraSearchField(placeholder = "Search documents")
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Doc", "Sheet", "Slides", "PDF").forEach { label ->
                        NexoraPill(
                            label = label,
                            selected = selectedType == label,
                            onClick = { selectedType = label }
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StorageTile(
                        title = "Cloud",
                        detail = "2.45 GB synced",
                        color = NexoraSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StorageTile(
                        title = "Offline",
                        detail = "34 files ready",
                        color = NexoraPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                NexoraSectionHeader(title = if (gridMode) "Grid View" else "List View", action = "Sort")
            }

            if (gridMode) {
                items(filteredFiles.chunked(2)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { file ->
                            FileGridCard(
                                file = file,
                                onClick = { onOpenFile(file) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(2 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filteredFiles) { file ->
                    FileListRow(file = file, onClick = { onOpenFile(file) })
                }
            }
        }
    }
}

@Composable
private fun StorageTile(
    title: String,
    detail: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    NexoraCard(modifier = modifier, contentPadding = 13.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FileListRow(
    file: WorkspaceFile,
    onClick: () -> Unit
) {
    NexoraCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(label = file.type.badge, color = file.type.color, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${file.path} - ${file.type.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (file.isPinned) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(NexoraPrimary)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text("...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FileGridCard(
    file: WorkspaceFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        NexoraIconBadge(label = file.type.badge, color = file.type.color, size = 44.dp)
        Column {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.path,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val DocumentType.label: String
    get() = when (this) {
        DocumentType.DOC -> "Doc"
        DocumentType.SHEET -> "Sheet"
        DocumentType.SLIDE -> "Slides"
        DocumentType.PDF -> "PDF"
    }

private val DocumentType.badge: String
    get() = when (this) {
        DocumentType.DOC -> "D"
        DocumentType.SHEET -> "S"
        DocumentType.SLIDE -> "P"
        DocumentType.PDF -> "PDF"
    }

private val DocumentType.color: Color
    @Composable
    get() = when (this) {
        DocumentType.DOC -> Color(0xFF2563EB)
        DocumentType.SHEET -> NexoraSecondary
        DocumentType.SLIDE -> Color(0xFFF97316)
        DocumentType.PDF -> NexoraError
    }
