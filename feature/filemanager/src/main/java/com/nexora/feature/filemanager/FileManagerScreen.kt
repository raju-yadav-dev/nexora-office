package com.nexora.feature.filemanager

import android.app.Activity
import android.os.Build
import android.provider.DocumentsContract
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraSearchField
import com.nexora.core.designsystem.component.NexoraToolbarButton
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FileManagerScreen(
    onOpenFile: (WorkspaceFile) -> Unit
) {
    val context = LocalContext.current
    val viewModel: FileManagerViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var selectedType by remember { mutableStateOf("Recent") }
    var viewMode by remember { mutableStateOf(FileViewMode.List) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(FileSortMode.Date) }
    var permissionRefreshKey by remember { mutableStateOf(0) }
    val activity = context as? Activity
    val openLocalFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { picked ->
            viewModel.openDocument(picked, onOpenFile)
        }
    }
    val openMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.openDocuments(uris) { opened ->
                opened.firstOrNull()?.let(onOpenFile)
            }
        }
    }
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.persistFolderAccess(it)
            viewModel.loadFolder(context, it)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefreshKey += 1
    }

    val files = state.recentFiles
    val filteredFiles = files
        .filter { file -> file.matchesFilter(selectedType) }
        .filter { file ->
            searchQuery.isBlank() ||
                file.name.contains(searchQuery, ignoreCase = true) ||
                file.path.contains(searchQuery, ignoreCase = true)
        }
        .let { visible ->
            when (sortMode) {
                FileSortMode.Name -> visible.sortedBy { it.name.lowercase() }
                FileSortMode.Date -> visible.sortedByDescending { it.lastModified }
                FileSortMode.Size -> visible.sortedBy { it.sizeLabel }
                FileSortMode.Type -> visible.sortedBy { it.type.name }
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 14.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FileManagerHeader(
                    visibleCount = filteredFiles.size,
                    totalCount = files.size,
                    viewMode = viewMode,
                    onCycleViewMode = { viewMode = viewMode.next() }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NexoraSearchField(
                        placeholder = "Search documents",
                        value = searchQuery,
                        onValueChange = { searchQuery = it }
                    )
                    FileActionBar(
                        onOpenLocalFile = { openLocalFileLauncher.launch(viewModel.allowedMimeTypes()) },
                        onOpenMultiple = { openMultipleLauncher.launch(viewModel.allowedMimeTypes()) },
                        onOpenFolder = { openFolderLauncher.launch(null) }
                    )
                }
            }

            if (state.error != null) {
                item {
                    NexoraCard(color = NexoraError.copy(alpha = 0.15f), contentPadding = 14.dp) {
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoraError
                        )
                        Spacer(Modifier.height(6.dp))
                        NexoraToolbarButton(label = "Dismiss", onClick = { viewModel.clearError() })
                    }
                }
            }

            item {
                StorageBrowserCard(
                    persistedFolders = state.persistedFolders,
                    onBrowseStorage = { openFolderLauncher.launch(null) },
                    onBrowseDownloads = { openFolderLauncher.launch(initialDownloadsUri()) },
                    onBrowsePrimary = { openFolderLauncher.launch(initialPrimaryUri()) },
                    onOpenPersistedFolder = { uri -> viewModel.loadFolder(context, uri) }
                )
            }

            state.currentFolderUri?.let { currentFolderUri ->
                item {
                    FileResultsHeader(
                        title = "Folder",
                        countLabel = if (state.isBrowsing) "Loading" else "${state.entries.size} items",
                        action = "Refresh",
                        onAction = { viewModel.loadFolder(context, currentFolderUri) }
                    )
                }
                if (state.entries.isEmpty() && !state.isBrowsing) {
                    item {
                        NexoraCard(contentPadding = 14.dp) {
                            Text(
                                text = "No items found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "This folder is empty or not accessible.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(state.entries) { _, entry ->
                        FileBrowserRow(
                            entry = entry,
                            onClick = { viewModel.openEntry(context, entry, onOpenFile) }
                        )
                    }
                }
            }

            item {
                val permissionState = remember(permissionRefreshKey, activity) {
                    activity?.let { viewModel.mediaPermissionState(it) }
                }
                if (permissionState != null && !permissionState.isGranted) {
                    PermissionCard(
                        state = permissionState,
                        onRequest = { permissionLauncher.launch(permissionState.requiredPermissions.toTypedArray()) },
                        onOpenSettings = { viewModel.openAppSettings(context) }
                    )
                }
            }

            item {
                FileExplorerControls(
                    selectedType = selectedType,
                    sortMode = sortMode,
                    viewMode = viewMode,
                    onFilterSelected = { selectedType = it },
                    onSortSelected = { sortMode = it },
                    onViewModeSelected = { viewMode = it }
                )
            }

            item {
                FileResultsHeader(
                    title = selectedType,
                    countLabel = "${filteredFiles.size} files",
                    action = sortMode.label,
                    onAction = { sortMode = sortMode.next() }
                )
            }

            if (filteredFiles.isEmpty()) {
                item {
                    EmptyFileState(
                        query = searchQuery,
                        onOpenLocalFile = { openLocalFileLauncher.launch(viewModel.allowedMimeTypes()) }
                    )
                }
            } else if (viewMode == FileViewMode.Grid) {
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
                    FileListRow(
                        file = file,
                        compact = viewMode == FileViewMode.Compact,
                        onClick = { onOpenFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileManagerHeader(
    visibleCount: Int,
    totalCount: Int,
    viewMode: FileViewMode,
    onCycleViewMode: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Files",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$visibleCount shown - $totalCount recent",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        NexoraToolbarButton(label = viewMode.label, onClick = onCycleViewMode)
    }
}

@Composable
private fun FileActionBar(
    onOpenLocalFile: () -> Unit,
    onOpenMultiple: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NexoraToolbarButton(label = "Open file", selected = true, onClick = onOpenLocalFile)
        NexoraToolbarButton(label = "Multi-select", onClick = onOpenMultiple)
        NexoraToolbarButton(label = "Open folder", onClick = onOpenFolder)
    }
}

@Composable
private fun FileExplorerControls(
    selectedType: String,
    sortMode: FileSortMode,
    viewMode: FileViewMode,
    onFilterSelected: (String) -> Unit,
    onSortSelected: (FileSortMode) -> Unit,
    onViewModeSelected: (FileViewMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Recent", "Favorites", "DOCX", "XLSX", "PDF", "PPTX", "TXT", "Images", "Local", "Cloud").forEach { label ->
                NexoraPill(
                    label = label,
                    selected = selectedType == label,
                    onClick = { onFilterSelected(label) }
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FileSortMode.entries.forEach { mode ->
                NexoraPill(
                    label = "Sort: ${mode.label}",
                    selected = sortMode == mode,
                    onClick = { onSortSelected(mode) }
                )
            }
            FileViewMode.entries.forEach { mode ->
                NexoraPill(
                    label = mode.label,
                    selected = viewMode == mode,
                    onClick = { onViewModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun FileResultsHeader(
    title: String,
    countLabel: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = action,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@Composable
private fun EmptyFileState(
    query: String,
    onOpenLocalFile: () -> Unit
) {
    NexoraCard(contentPadding = 14.dp) {
        Text(
            text = if (query.isBlank()) "No files yet" else "No matching files",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (query.isBlank()) {
                "Open a document to add it to recent files."
            } else {
                "Try another search or clear the active filter."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        NexoraToolbarButton(label = "Open file", selected = true, onClick = onOpenLocalFile)
    }
}

@Composable
private fun PermissionCard(
    state: com.nexora.core.common.permissions.PermissionState,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    NexoraCard(color = NexoraPrimary.copy(alpha = 0.12f), contentPadding = 14.dp) {
        Text(
            text = "Media access",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.shouldShowRationale) {
                "Allow access to import images, video, or audio into documents."
            } else {
                "Grant media access for richer documents."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        if (state.isPermanentlyDenied) {
            NexoraToolbarButton(label = "Open Settings", selected = true, onClick = onOpenSettings)
        } else {
            NexoraToolbarButton(label = "Grant Access", selected = true, onClick = onRequest)
        }
    }
}

@Composable
private fun StorageBrowserCard(
    persistedFolders: List<com.nexora.core.model.PersistedUriPermission>,
    onBrowseStorage: () -> Unit,
    onBrowseDownloads: () -> Unit,
    onBrowsePrimary: () -> Unit,
    onOpenPersistedFolder: (android.net.Uri) -> Unit
) {
    NexoraCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (persistedFolders.isEmpty()) "Browse device folders" else "${persistedFolders.size} pinned folders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            NexoraToolbarButton(label = "Browse", selected = true, onClick = onBrowseStorage)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NexoraToolbarButton(label = "Downloads", onClick = onBrowseDownloads)
            NexoraToolbarButton(label = "Internal", onClick = onBrowsePrimary)
        }
        if (persistedFolders.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                persistedFolders.forEach { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenPersistedFolder(android.net.Uri.parse(permission.uri)) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = permission.uri.substringAfterLast("/"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Open", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileBrowserRow(
    entry: BrowserEntry,
    onClick: () -> Unit
) {
    NexoraCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badge = if (entry.isDirectory) "FOL" else entry.mimeType.toBrowserBadge()
            NexoraIconBadge(label = badge, color = if (entry.isDirectory) NexoraPrimary else entry.mimeType.toBrowserColor(), size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.detailLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (entry.isDirectory) "Browse" else "Open",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun initialDownloadsUri(): android.net.Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching {
            DocumentsContract.buildRootUri("com.android.providers.downloads.documents", "downloads")
        }.getOrNull()
    } else {
        null
    }
}

private fun initialPrimaryUri(): android.net.Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching {
            DocumentsContract.buildRootUri("com.android.externalstorage.documents", "primary")
        }.getOrNull()
    } else {
        null
    }
}

private val BrowserEntry.detailLabel: String
    get() {
        if (isDirectory) return "Folder"
        val typeLabel = mimeType?.substringAfterLast("/")?.uppercase() ?: "Document"
        val sizeLabel = sizeBytes?.let(::formatBytes) ?: "Unknown size"
        return "$typeLabel - $sizeLabel"
    }

private fun String?.toBrowserBadge(): String = when {
    this?.contains("pdf", ignoreCase = true) == true -> "PDF"
    this?.contains("spreadsheet", ignoreCase = true) == true -> "S"
    this?.contains("presentation", ignoreCase = true) == true -> "P"
    this?.contains("word", ignoreCase = true) == true -> "D"
    this?.contains("text", ignoreCase = true) == true -> "TXT"
    else -> "FILE"
}

private fun String?.toBrowserColor(): Color = when {
    this?.contains("pdf", ignoreCase = true) == true -> NexoraError
    this?.contains("spreadsheet", ignoreCase = true) == true -> NexoraSecondary
    this?.contains("presentation", ignoreCase = true) == true -> Color(0xFFF97316)
    this?.contains("text", ignoreCase = true) == true -> NexoraPrimaryVariant
    else -> NexoraPrimary
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.1f GB", mb / 1024.0)
}

@Composable
private fun FileListRow(
    file: WorkspaceFile,
    compact: Boolean,
    onClick: () -> Unit
) {
    NexoraCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = if (compact) 8.dp else 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraIconBadge(label = file.type.badge, color = file.type.color, size = if (compact) 32.dp else 38.dp)
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
                if (!compact) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${file.type.label} - ${file.sizeLabel} - ${file.locationLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(">", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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


private val WorkspaceFile.locationLabel: String
    get() = when {
        path.startsWith("content://") -> "Device"
        path.startsWith("/cloud") -> "Cloud"
        path.startsWith("/local") -> "Offline"
        path.startsWith("/downloads") -> "Downloads"
        else -> path
    }

private fun WorkspaceFile.matchesFilter(filter: String): Boolean = when (filter) {
    "Recent" -> true
    "Favorites" -> isPinned
    "DOCX" -> type == DocumentType.DOC
    "XLSX" -> type == DocumentType.SHEET
    "PDF" -> type == DocumentType.PDF
    "PPTX" -> type == DocumentType.SLIDE
    "TXT" -> type == DocumentType.TEXT
    "Images" -> type == DocumentType.IMAGE
    "Local" -> path.startsWith("content://") || path.startsWith("/local") || path.startsWith("/downloads")
    "Cloud" -> path.startsWith("/cloud")
    else -> true
}

private enum class FileViewMode(val label: String) {
    List("List"),
    Grid("Grid"),
    Compact("Compact");

    fun next(): FileViewMode = entries[(ordinal + 1) % entries.size]
}

private enum class FileSortMode(val label: String) {
    Date("Date"),
    Name("Name"),
    Size("Size"),
    Type("Type");

    fun next(): FileSortMode = entries[(ordinal + 1) % entries.size]
}


private val DocumentType.label: String
    get() = when (this) {
        DocumentType.DOC -> "Doc"
        DocumentType.SHEET -> "Sheet"
        DocumentType.SLIDE -> "Slides"
        DocumentType.PDF -> "PDF"
        DocumentType.IMAGE -> "Image"
        DocumentType.TEXT -> "Text"
    }

private val DocumentType.badge: String
    get() = when (this) {
        DocumentType.DOC -> "D"
        DocumentType.SHEET -> "S"
        DocumentType.SLIDE -> "P"
        DocumentType.PDF -> "PDF"
        DocumentType.IMAGE -> "IMG"
        DocumentType.TEXT -> "TXT"
    }

private val DocumentType.color: Color
    @Composable
    get() = when (this) {
        DocumentType.DOC -> Color(0xFF2563EB)
        DocumentType.SHEET -> NexoraSecondary
        DocumentType.SLIDE -> Color(0xFFF97316)
        DocumentType.PDF -> NexoraError
        DocumentType.IMAGE -> Color(0xFF0EA5E9)
        DocumentType.TEXT -> NexoraPrimaryVariant
    }
