package com.nexora.feature.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
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
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import java.time.Instant

@Composable
fun FileManagerScreen(
    onOpenFile: (WorkspaceFile) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("All") }
    var gridMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var newestFirst by remember { mutableStateOf(true) }
    var localFiles by remember { mutableStateOf<List<WorkspaceFile>>(emptyList()) }
    val sampleFiles = remember { demoWorkspaceFiles() }
    val openLocalFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.persistReadPermission(uri)
            val file = uri.toWorkspaceFile(context)
            localFiles = listOf(file) + localFiles.filterNot { it.path == file.path }
            onOpenFile(file)
        }
    }

    val files = localFiles + sampleFiles
    val filteredFiles = files
        .filter { file -> selectedType == "All" || file.type.label == selectedType }
        .filter { file ->
            searchQuery.isBlank() ||
                file.name.contains(searchQuery, ignoreCase = true) ||
                file.path.contains(searchQuery, ignoreCase = true)
        }
        .let { visible ->
            if (newestFirst) visible.sortedByDescending { it.lastModified } else visible.sortedBy { it.name }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Files",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Local, cloud and recent work in one Pro view",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        NexoraToolbarButton(
                            label = if (gridMode) "Grid" else "List",
                            selected = gridMode,
                            onClick = { gridMode = !gridMode }
                        )
                    }
                    NexoraSearchField(
                        placeholder = "Search documents",
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )
                }
            }

            item {
                OpenLocalFileCard(onOpenLocalFile = { openLocalFileLauncher.launch(openableMimeTypes) })
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Doc", "Sheet", "Slides", "PDF", "Text").forEach { label ->
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
                        title = "Local",
                        detail = "${localFiles.size} opened",
                        color = NexoraPrimaryVariant,
                        modifier = Modifier.weight(1f)
                    )
                    StorageTile(
                        title = "Pro",
                        detail = "All tools active",
                        color = NexoraSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                NexoraSectionHeader(
                    title = if (gridMode) "Grid View" else "List View",
                    action = if (newestFirst) "Newest" else "A-Z",
                    onAction = { newestFirst = !newestFirst }
                )
            }

            if (filteredFiles.isEmpty()) {
                item {
                    NexoraCard(contentPadding = 14.dp) {
                        Text(
                            text = "No files found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Open a local file or change the active filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (gridMode) {
                items(filteredFiles.chunked(2)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
private fun OpenLocalFileCard(
    onOpenLocalFile: () -> Unit
) {
    NexoraCard(
        color = NexoraPrimary.copy(alpha = 0.15f),
        contentPadding = 14.dp,
        onClick = onOpenLocalFile
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(label = "Open", color = NexoraPrimary, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open local file",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pick DOCX, XLSX, PPTX, PDF, TXT or CSV from this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            NexoraToolbarButton(label = "Browse", selected = true, onClick = onOpenLocalFile)
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
            NexoraIconBadge(label = file.type.badge, color = file.type.color, size = 40.dp)
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
                    if (file.isPinned || file.path.startsWith("content://")) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NexoraPrimary)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${file.type.label} - ${file.sizeLabel} - ${file.locationLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("Open", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
                text = "${file.sizeLabel} - ${file.locationLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun demoWorkspaceFiles(): List<WorkspaceFile> = listOf(
    WorkspaceFile(
        name = "Marketing Plan.docx",
        path = "/cloud/Docs",
        type = DocumentType.DOC,
        sizeLabel = "12.4 MB"
    ),
    WorkspaceFile(
        name = "Sales Analysis.xlsx",
        path = "/cloud/Sheets",
        type = DocumentType.SHEET,
        sizeLabel = "850 KB",
        isPinned = true
    ),
    WorkspaceFile(
        name = "Product Roadmap.pptx",
        path = "/local/Slides",
        type = DocumentType.SLIDE,
        sizeLabel = "3.2 MB"
    ),
    WorkspaceFile(
        name = "User Guide.pdf",
        path = "/downloads/PDF",
        type = DocumentType.PDF,
        sizeLabel = "1.5 MB"
    ),
    WorkspaceFile(
        name = "Meeting Notes.docx",
        path = "/cloud/Docs",
        type = DocumentType.DOC,
        sizeLabel = "2.1 MB"
    ),
    WorkspaceFile(
        name = "Company Profile.pptx",
        path = "/local/Slides",
        type = DocumentType.SLIDE,
        sizeLabel = "4.7 MB"
    )
)

private fun Uri.toWorkspaceFile(context: Context): WorkspaceFile {
    val name = context.queryDisplayName(this) ?: lastPathSegment?.substringAfterLast("/") ?: "Local file"
    val type = documentTypeFromNameOrMime(name, context.contentResolver.getType(this))
    val size = context.querySize(this)?.toReadableSize() ?: "Local file"
    return WorkspaceFile(
        name = name,
        path = toString(),
        type = type,
        lastModified = Instant.now().toString(),
        sizeLabel = size,
        isPinned = true
    )
}

private fun Context.persistReadPermission(uri: Uri) {
    runCatching {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}

private fun Context.queryDisplayName(uri: Uri): String? = contentResolver
    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    ?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }

private fun Context.querySize(uri: Uri): Long? = contentResolver
    .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
    ?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
    }

private fun Long.toReadableSize(): String {
    val kb = this / 1024f
    val mb = kb / 1024f
    return if (mb >= 1f) {
        "${"%.1f".format(mb)} MB"
    } else {
        "${kb.toInt().coerceAtLeast(1)} KB"
    }
}

private val WorkspaceFile.locationLabel: String
    get() = when {
        path.startsWith("content://") -> "Device"
        path.startsWith("/cloud") -> "Cloud"
        path.startsWith("/local") -> "Offline"
        path.startsWith("/downloads") -> "Downloads"
        else -> path
    }

private fun documentTypeFromNameOrMime(name: String, mimeType: String?): DocumentType = when {
    mimeType?.contains("pdf", ignoreCase = true) == true || name.endsWith(".pdf", ignoreCase = true) -> DocumentType.PDF
    mimeType?.contains("spreadsheet", ignoreCase = true) == true ||
        mimeType?.contains("excel", ignoreCase = true) == true ||
        name.endsWith(".xls", ignoreCase = true) ||
        name.endsWith(".xlsx", ignoreCase = true) -> DocumentType.SHEET
    mimeType?.contains("presentation", ignoreCase = true) == true ||
        mimeType?.contains("powerpoint", ignoreCase = true) == true ||
        name.endsWith(".ppt", ignoreCase = true) ||
        name.endsWith(".pptx", ignoreCase = true) -> DocumentType.SLIDE
    mimeType?.startsWith("text/", ignoreCase = true) == true ||
        mimeType?.contains("json", ignoreCase = true) == true ||
        mimeType?.contains("xml", ignoreCase = true) == true ||
        name.endsWith(".txt", ignoreCase = true) ||
        name.endsWith(".csv", ignoreCase = true) ||
        name.endsWith(".json", ignoreCase = true) ||
        name.endsWith(".xml", ignoreCase = true) ||
        name.endsWith(".md", ignoreCase = true) -> DocumentType.TEXT
    else -> DocumentType.DOC
}

private val DocumentType.label: String
    get() = when (this) {
        DocumentType.DOC -> "Doc"
        DocumentType.SHEET -> "Sheet"
        DocumentType.SLIDE -> "Slides"
        DocumentType.PDF -> "PDF"
        DocumentType.TEXT -> "Text"
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
    @Composable
    get() = when (this) {
        DocumentType.DOC -> Color(0xFF2563EB)
        DocumentType.SHEET -> NexoraSecondary
        DocumentType.SLIDE -> Color(0xFFF97316)
        DocumentType.PDF -> NexoraError
        DocumentType.TEXT -> NexoraPrimaryVariant
    }

private val openableMimeTypes = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "text/plain",
    "text/csv",
    "text/markdown",
    "application/json",
    "text/xml",
    "application/xml"
)
