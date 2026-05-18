package com.nexora.feature.filemanager

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.core.common.logging.NexoraLogger
import com.nexora.core.common.permissions.PermissionManager
import com.nexora.core.common.permissions.PermissionState
import com.nexora.core.data.session.DocumentSessionRepository
import com.nexora.core.data.storage.FileOpenManager
import com.nexora.core.data.storage.RecentFilesRepository
import com.nexora.core.data.storage.SafPermissionMissingException
import com.nexora.core.data.storage.StorageAccessRepository
import com.nexora.core.model.DocumentAccessMode
import com.nexora.core.model.PersistedUriPermission
import com.nexora.core.model.WorkspaceFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val recentFilesRepository: RecentFilesRepository,
    private val storageAccessRepository: StorageAccessRepository,
    private val fileOpenManager: FileOpenManager,
    private val permissionManager: PermissionManager,
    private val sessionRepository: DocumentSessionRepository
) : ViewModel() {

    private val logTag = "FileManagerViewModel"

    private val _state = MutableStateFlow(FileManagerState())
    val state: StateFlow<FileManagerState> = _state.asStateFlow()

    init {
        recentFilesRepository.observeRecentFiles()
            .onEach { files -> _state.value = _state.value.copy(recentFiles = files) }
            .launchIn(viewModelScope)

        storageAccessRepository.observePersistedPermissions()
            .onEach { permissions ->
                _state.value = _state.value.copy(persistedFolders = permissions.filter { it.isTree })
            }
            .launchIn(viewModelScope)
    }

    fun allowedMimeTypes(): Array<String> = fileOpenManager.allowedMimeTypes()

    fun mediaPermissionState(activity: android.app.Activity): PermissionState =
        permissionManager.getMediaPermissionState(activity)

    fun openAppSettings(context: android.content.Context) =
        permissionManager.openAppSettings(context)

    fun openDocument(uri: Uri, onOpened: (WorkspaceFile) -> Unit) {
        viewModelScope.launch {
            val file = runCatching {
                NexoraLogger.i(logTag, "event=saf_selection source=open_document uri=$uri")
                storageAccessRepository.persistUriPermission(uri, DocumentAccessMode.READ_WRITE).getOrThrow()
                fileOpenManager.buildWorkspaceFile(uri)
            }.onFailure { error ->
                NexoraLogger.e(logTag, "event=document_open_failure source=open_document uri=$uri", error)
                _state.value = _state.value.copy(error = userFacingOpenError(error))
            }.getOrNull()

            if (file != null) {
                NexoraLogger.i(logTag, "event=document_open_success source=open_document uri=${file.path}")
                recentFilesRepository.addRecent(file)
                sessionRepository.openSession(file)
                onOpened(file)
            }
        }
    }

    fun openDocuments(uris: List<Uri>, onOpened: (List<WorkspaceFile>) -> Unit) {
        viewModelScope.launch {
            val opened = mutableListOf<WorkspaceFile>()
            uris.forEach { uri ->
                runCatching {
                    NexoraLogger.i(logTag, "event=saf_selection source=open_multiple uri=$uri")
                    storageAccessRepository.persistUriPermission(uri, DocumentAccessMode.READ_WRITE).getOrThrow()
                    fileOpenManager.buildWorkspaceFile(uri)
                }.onSuccess { file ->
                    NexoraLogger.i(logTag, "event=document_open_success source=open_multiple uri=${file.path}")
                    recentFilesRepository.addRecent(file)
                    sessionRepository.openSession(file)
                    opened.add(file)
                }.onFailure { error ->
                    NexoraLogger.e(logTag, "event=document_open_failure source=open_multiple uri=$uri", error)
                    _state.value = _state.value.copy(error = userFacingOpenError(error))
                }
            }
            if (opened.isNotEmpty()) onOpened(opened)
        }
    }

    fun persistFolderAccess(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                NexoraLogger.i(logTag, "event=saf_selection source=open_tree uri=$uri")
                storageAccessRepository.persistTreePermission(uri, DocumentAccessMode.READ_WRITE).getOrThrow()
            }
                .onFailure { error ->
                    NexoraLogger.e(logTag, "event=saf_persist_tree_failure uri=$uri", error)
                    _state.value = _state.value.copy(error = userFacingOpenError(error))
                }
        }
    }

    fun loadFolder(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBrowsing = true, currentFolderUri = uri)
            val entries = runCatching {
                NexoraLogger.d(logTag, "Loading folder contents: $uri")
                withContext(Dispatchers.IO) { listChildren(context, uri) }
            }
                .getOrElse { error ->
                    NexoraLogger.e(logTag, "Failed to load folder: $uri", error)
                    _state.value = _state.value.copy(error = error.message ?: "Failed to load folder")
                    emptyList()
                }
            _state.value = _state.value.copy(entries = entries, isBrowsing = false)
        }
    }

    fun openEntry(context: Context, entry: BrowserEntry, onOpened: (WorkspaceFile) -> Unit) {
        if (entry.isDirectory) {
            entry.uri?.let { loadFolder(context, it) }
            return
        }
        entry.uri?.let { openPersistedSafDocument(it, onOpened) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun listChildren(context: Context, uri: Uri): List<BrowserEntry> {
        val folder = DocumentFile.fromTreeUri(context, uri)
        val files = folder?.listFiles().orEmpty()
        return files.map { file ->
            BrowserEntry(
                name = file.name ?: "Untitled",
                uri = file.uri,
                isDirectory = file.isDirectory,
                mimeType = file.type,
                lastModified = file.lastModified(),
                sizeBytes = if (file.isFile) file.length() else null
            )
        }.sortedWith(compareBy<BrowserEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun openPersistedSafDocument(uri: Uri, onOpened: (WorkspaceFile) -> Unit) {
        viewModelScope.launch {
            val file = runCatching {
                val validation = storageAccessRepository.validatePersistedUriPermission(
                    uri = uri,
                    accessMode = DocumentAccessMode.READ
                )
                if (!validation.allows(DocumentAccessMode.READ)) {
                    throw SafPermissionMissingException(uri, DocumentAccessMode.READ)
                }
                NexoraLogger.i(
                    logTag,
                    "event=document_permission_restore uri=$uri persistedUri=${validation.persistedUri} viaTree=${validation.grantedByTree}"
                )
                fileOpenManager.buildWorkspaceFile(uri)
            }.onFailure { error ->
                NexoraLogger.e(logTag, "event=document_open_failure source=persisted_tree uri=$uri", error)
                _state.value = _state.value.copy(error = userFacingOpenError(error))
            }.getOrNull()

            if (file != null) {
                NexoraLogger.i(logTag, "event=document_open_success source=persisted_tree uri=${file.path}")
                recentFilesRepository.addRecent(file)
                sessionRepository.openSession(file)
                onOpened(file)
            }
        }
    }

    private fun userFacingOpenError(error: Throwable): String =
        when (error) {
            is SafPermissionMissingException ->
                "Access to this document needs to be restored. Reopen it from the file picker to continue."
            is SecurityException ->
                "Android did not grant lasting access to this document. Reopen it from the file picker."
            else ->
                "Could not open this document. Try reopening it from the file picker."
        }
}

data class FileManagerState(
    val recentFiles: List<WorkspaceFile> = emptyList(),
    val persistedFolders: List<PersistedUriPermission> = emptyList(),
    val entries: List<BrowserEntry> = emptyList(),
    val currentFolderUri: Uri? = null,
    val isBrowsing: Boolean = false,
    val error: String? = null
)

data class BrowserEntry(
    val name: String,
    val uri: Uri?,
    val isDirectory: Boolean,
    val mimeType: String?,
    val lastModified: Long?,
    val sizeBytes: Long?
)
