package com.nexora.feature.dashboard

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.core.designsystem.component.ActionItem
import com.nexora.core.designsystem.component.NexoraActionGrid
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraGradientBackground
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.component.NexoraLogoMark
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraSearchField
import com.nexora.core.designsystem.component.NexoraSectionHeader
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen(
    onOpenFileManager: () -> Unit,
    onOpenEditor: (WorkspaceFile) -> Unit
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var selectedFilter by remember { mutableStateOf("Recent") }
    var searchQuery by remember { mutableStateOf("") }
    val quickActions = listOf(
        QuickCreate("Doc", "D", Color(0xFF2563EB), DocumentType.DOC, "Untitled Document.docx"),
        QuickCreate("Sheet", "S", NexoraSecondary, DocumentType.SHEET, "Untitled Spreadsheet.xlsx"),
        QuickCreate("Slides", "P", Color(0xFFF97316), DocumentType.SLIDE, "Untitled Presentation.pptx"),
        QuickCreate("PDF", "PDF", NexoraError, DocumentType.PDF, "Untitled PDF.pdf")
    )
    val visibleRecentFiles = state.recentFiles
        .filter { file ->
            when (selectedFilter) {
                "Starred" -> file.isPinned
                else -> true
            }
        }
        .filter { file -> searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true) }

    NexoraGradientBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(top = 20.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                DashboardHeader()
            }

            item {
                NexoraSearchField(
                    placeholder = "Search files, tools, templates...",
                    value = searchQuery,
                    onValueChange = { searchQuery = it }
                )
            }

            item {
                ContinueEditingCard(
                    file = visibleRecentFiles.firstOrNull(),
                    onOpenFileManager = onOpenFileManager,
                    onOpenEditor = onOpenEditor
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NexoraSectionHeader(title = "Create")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        quickActions.forEach { action ->
                            QuickCreateButton(
                                item = action,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenEditor(action.toWorkspaceFile()) }
                            )
                        }
                    }
                }
            }

            item {
                NexoraCard(
                    color = NexoraPrimary.copy(alpha = 0.16f),
                    contentPadding = 14.dp,
                    onClick = onOpenFileManager
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexoraIconBadge(label = "Open", color = NexoraPrimaryVariant, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open local files",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Browse device storage and recent documents",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Browse",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NexoraSectionHeader(title = "Recent Work", action = "See all", onAction = onOpenFileManager)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Recent", "Starred").forEach { label ->
                            NexoraPill(
                                label = label,
                                selected = selectedFilter == label,
                                onClick = { selectedFilter = label }
                            )
                        }
                    }
                }
            }

            if (visibleRecentFiles.isEmpty()) {
                item {
                    NexoraCard(contentPadding = 14.dp) {
                        Text(
                            text = "No files match this view",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try another filter or search term.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(visibleRecentFiles) { file ->
                OfficeFileRow(file = file, onClick = { onOpenEditor(file) })
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nexora Office",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Pick up recent work or start a clean document",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NexoraLogoMark(size = 40.dp)
        Spacer(Modifier.width(10.dp))
    }
}

@Composable
private fun ContinueEditingCard(
    file: WorkspaceFile?,
    onOpenFileManager: () -> Unit,
    onOpenEditor: (WorkspaceFile) -> Unit
) {
    NexoraCard(
        color = MaterialTheme.colorScheme.surface,
        contentPadding = 14.dp,
        onClick = { if (file == null) onOpenFileManager() else onOpenEditor(file) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(
                label = file?.type?.badge ?: "Open",
                color = file?.type?.color ?: NexoraPrimary,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (file == null) "Continue editing" else file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file == null) "Open a local file to build your workspace" else "Last opened - ${file.sizeLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("Resume", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun QuickCreateButton(
    item: QuickCreate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NexoraIconBadge(label = item.badge, color = item.color, size = 34.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun OfficeFileRow(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (file.isPinned) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NexoraPrimary)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOfNotNull(file.sizeLabel.takeIf { it.isNotBlank() }, file.lastModified.takeIf { it.isNotBlank() })
                        .ifEmpty { listOf("Updated recently") }
                        .joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class QuickCreate(
    val title: String,
    val badge: String,
    val color: Color,
    val type: DocumentType,
    val fileName: String
) {
    val actionItem: ActionItem
        get() = ActionItem(title, badge, color)

    fun toWorkspaceFile(): WorkspaceFile = WorkspaceFile(
        name = fileName,
        path = "nexora://workspace/create/${title.lowercase()}",
        type = type,
        sizeLabel = "New document"
    )
}

private val DocumentType.badge: String
    get() = when (this) {
        DocumentType.DOC -> "D"
        DocumentType.SHEET -> "S"
        DocumentType.SLIDE -> "P"
        DocumentType.PDF -> "PDF"
        DocumentType.TEXT -> "TXT"
    }

private val DocumentType.color: Color
    get() = when (this) {
        DocumentType.DOC -> Color(0xFF2563EB)
        DocumentType.SHEET -> NexoraSecondary
        DocumentType.SLIDE -> Color(0xFFF97316)
        DocumentType.PDF -> NexoraError
        DocumentType.TEXT -> Color(0xFF14B8A6)
    }
