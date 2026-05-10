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
import com.nexora.core.designsystem.component.NexoraMetricCard
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraSearchField
import com.nexora.core.designsystem.component.NexoraSectionHeader
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile

@Composable
fun DashboardScreen(
    onOpenFileManager: () -> Unit,
    onOpenEditor: (WorkspaceFile) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Recent") }
    var searchQuery by remember { mutableStateOf("") }
    val quickActions = listOf(
        QuickCreate("Doc", "D", Color(0xFF2563EB), DocumentType.DOC, "Untitled Document.docx"),
        QuickCreate("Sheet", "S", NexoraSecondary, DocumentType.SHEET, "Untitled Spreadsheet.xlsx"),
        QuickCreate("Slides", "P", Color(0xFFF97316), DocumentType.SLIDE, "Untitled Presentation.pptx"),
        QuickCreate("PDF", "PDF", NexoraError, DocumentType.PDF, "Untitled PDF.pdf")
    )
    val recentFiles = listOf(
        OfficeFile("Project Proposal.docx", "12.4 MB", "2m ago", "D", Color(0xFF2563EB), true, true, DocumentType.DOC),
        OfficeFile("Monthly Report.xlsx", "850 KB", "1h ago", "S", NexoraSecondary, false, true, DocumentType.SHEET),
        OfficeFile("Business Plan.pptx", "3.2 MB", "Yesterday", "P", Color(0xFFF97316), true, false, DocumentType.SLIDE),
        OfficeFile("Contract Agreement.pdf", "1.5 MB", "Yesterday", "PDF", NexoraError, false, false, DocumentType.PDF),
        OfficeFile("User Guide.docx", "2.1 MB", "2 days ago", "D", Color(0xFF2563EB), false, true, DocumentType.DOC)
    )
    val visibleRecentFiles = recentFiles
        .filter { file ->
            when (selectedFilter) {
                "Starred" -> file.starred
                "Cloud" -> file.cloudBacked
                else -> true
            }
        }
        .filter { file ->
            searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true)
        }

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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NexoraMetricCard(
                        label = "Today",
                        value = "18",
                        accent = NexoraPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    NexoraMetricCard(
                        label = "Cloud",
                        value = "2.45 GB",
                        accent = NexoraSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NexoraSectionHeader(title = "Quick Create")
                    NexoraActionGrid(
                        items = quickActions.map { it.actionItem },
                        onClick = { action ->
                            quickActions
                                .firstOrNull { it.title == action.title }
                                ?.let { onOpenEditor(it.toWorkspaceFile()) }
                        }
                    )
                }
            }

            item {
                NexoraCard(
                    color = NexoraPrimary.copy(alpha = 0.16f),
                    contentPadding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexoraIconBadge(label = "AI", color = NexoraPrimaryVariant, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nexora Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Full Pro tools active - autosave ready - cloud ready",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Open",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable {
                                onOpenEditor(
                                    WorkspaceFile(
                                        name = "Workspace.docx",
                                        path = "nexora://workspace/home",
                                        type = DocumentType.DOC,
                                        sizeLabel = "Pro workspace"
                                    )
                                )
                            }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NexoraSectionHeader(title = "Recent Files", action = "See all", onAction = onOpenFileManager)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Recent", "Starred", "Cloud").forEach { label ->
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
                OfficeFileRow(file = file, onClick = { onOpenEditor(file.toWorkspaceFile()) })
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
                text = "Pro productivity workspace",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NexoraLogoMark(size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NexoraPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("Pro", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OfficeFileRow(
    file: OfficeFile,
    onClick: () -> Unit
) {
    NexoraCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(label = file.badge, color = file.color, size = 38.dp)
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
                    if (file.starred) {
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
                    text = "${file.size} - ${file.modified}",
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

private data class OfficeFile(
    val name: String,
    val size: String,
    val modified: String,
    val badge: String,
    val color: Color,
    val starred: Boolean,
    val cloudBacked: Boolean,
    val type: DocumentType
)

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
        sizeLabel = "New Pro file"
    )
}

private fun OfficeFile.toWorkspaceFile(): WorkspaceFile = WorkspaceFile(
    name = name,
    path = if (cloudBacked) "/cloud/Recent" else "/local/Recent",
    type = type,
    sizeLabel = size,
    isPinned = starred
)
