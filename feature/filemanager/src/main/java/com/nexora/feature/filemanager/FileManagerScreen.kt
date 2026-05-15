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
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FileManagerScreen(
    onOpenFile: (WorkspaceFile) -> Unit
) {
    val context = LocalContext.current
    val viewModel: FileManagerViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var selectedType by remember { mutableStateOf("All") }
    var gridMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var newestFirst by remember { mutableStateOf(true) }
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
        uri?.let { viewModel.persistFolderAccess(it) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefreshKey += 1
    }

    val files = state.recentFiles
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
                OpenLocalFileCard(
                    onOpenLocalFile = { openLocalFileLauncher.launch(viewModel.allowedMimeTypes()) },
                    onOpenMultiple = { openMultipleLauncher.launch(viewModel.allowedMimeTypes()) },
                    onOpenFolder = { openFolderLauncher.launch(null) }
                )
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
                    NexoraSectionHeader(
                        title = "Folder contents",
                        action = if (state.isBrowsing) "Loading" else "Refresh",
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
                        title = "Recents",
                        detail = "${state.recentFiles.size} opened",
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
    onOpenLocalFile: () -> Unit,
    onOpenMultiple: () -> Unit,
    onOpenFolder: () -> Unit
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
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NexoraToolbarButton(label = "Multi", selected = true, onClick = onOpenMultiple)
            NexoraToolbarButton(label = "Folder", selected = false, onClick = onOpenFolder)
        }
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
private fun StorageBrowserCard(
    persistedFolders: List<com.nexora.core.model.PersistedUriPermission>,
    onBrowseStorage: () -> Unit,
    onBrowseDownloads: () -> Unit,
    onBrowsePrimary: () -> Unit,
    onOpenPersistedFolder: (android.net.Uri) -> Unit
) {
    NexoraCard(color = NexoraSecondary.copy(alpha = 0.12f), contentPadding = 14.dp) {
        Text(
            text = "Storage locations",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NexoraToolbarButton(label = "Browse", selected = true, onClick = onBrowseStorage)
            NexoraToolbarButton(label = "Downloads", selected = false, onClick = onBrowseDownloads)
            NexoraToolbarButton(label = "Internal", selected = false, onClick = onBrowsePrimary)
        }
        if (persistedFolders.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Pinned folders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                persistedFolders.forEach { permission ->
                    NexoraCard(contentPadding = 10.dp, onClick = {
                        onOpenPersistedFolder(android.net.Uri.parse(permission.uri))
                    }) {
                        Text(
                            text = permission.uri.substringAfterLast("/"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tree access granted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badge = if (entry.isDirectory) "DIR" else "FILE"
            NexoraIconBadge(label = badge, color = NexoraPrimary, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = entry.mimeType ?: if (entry.isDirectory) "Folder" else "Document"
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (entry.isDirectory) "Open" else "Open",
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


private val WorkspaceFile.locationLabel: String
    get() = when {
        path.startsWith("content://") -> "Device"
        path.startsWith("/cloud") -> "Cloud"
        path.startsWith("/local") -> "Offline"
        path.startsWith("/downloads") -> "Downloads"
        else -> path
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
