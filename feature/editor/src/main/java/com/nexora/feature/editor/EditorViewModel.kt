package com.nexora.feature.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.core.common.logging.NexoraLogger
import com.nexora.core.data.session.DocumentSessionRepository
import com.nexora.core.data.storage.FileOpenManager
import com.nexora.core.data.storage.FolderSelectedForOpenException
import com.nexora.core.data.storage.DocumentUnavailableException
import com.nexora.core.data.storage.DocumentUnreadableException
import com.nexora.core.data.storage.EmptyDocumentException
import com.nexora.core.data.storage.RecentFilesRepository
import com.nexora.core.data.storage.SafPermissionMissingException
import com.nexora.core.data.storage.StorageAccessRepository
import com.nexora.core.model.DocumentAccessMode
import com.nexora.core.model.DocumentSession
import com.nexora.core.model.DocumentType
import com.nexora.core.model.EditorTab
import com.nexora.core.model.WorkspaceFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "EditorViewModel"

data class EditorUiState(
    val tabs: List<EditorTab> = emptyList(),
    val activeTabId: String? = null,
    val sessions: List<DocumentSession> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val sessionRepository: DocumentSessionRepository,
    private val storageAccessRepository: StorageAccessRepository,
    private val recentFilesRepository: RecentFilesRepository,
    private val fileOpenManager: FileOpenManager
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val autosaveRequests = MutableSharedFlow<AutosaveRequest>(extraBufferCapacity = 32)

    init {
        sessionRepository.observeSessions()
            .onEach { sessions ->
                val tabs = sessions.map { it.toTab() }
                val activeTabId = _state.value.activeTabId ?: tabs.firstOrNull()?.fileId
                _state.value = _state.value.copy(tabs = tabs, activeTabId = activeTabId, sessions = sessions)
            }
            .launchIn(viewModelScope)

        bindAutosavePipeline()
    }

    fun openFromFile(file: WorkspaceFile) {
        viewModelScope.launch {
            val session = sessionRepository.openSession(file)
            _state.value = _state.value.copy(activeTabId = session.sessionId)
            sessionRepository.touch(session.sessionId)
        }
    }

    fun hasDocumentAccess(uri: Uri, accessMode: DocumentAccessMode = DocumentAccessMode.READ): Boolean {
        val validation = storageAccessRepository.validatePersistedUriPermission(uri, accessMode)
        val allowed = validation.allows(accessMode)
        NexoraLogger.i(
            TAG,
            "event=document_permission_check uri=$uri mode=${accessMode.name} allowed=$allowed persistedUri=${validation.persistedUri} viaTree=${validation.grantedByTree}"
        )
        return allowed
    }

    fun requireDocumentAccess(uri: Uri, accessMode: DocumentAccessMode = DocumentAccessMode.READ) {
        val validation = storageAccessRepository.validatePersistedUriPermission(uri, accessMode)
        if (!validation.allows(accessMode)) {
            NexoraLogger.w(
                TAG,
                "event=document_permission_missing uri=$uri mode=${accessMode.name} read=${validation.readGranted} write=${validation.writeGranted}"
            )
            throw SafPermissionMissingException(uri, accessMode)
        }
        NexoraLogger.i(
            TAG,
            "event=document_permission_restore uri=$uri mode=${accessMode.name} persistedUri=${validation.persistedUri} viaTree=${validation.grantedByTree}"
        )
    }

    suspend fun attachSafDocumentToSession(
        sessionId: String,
        uri: Uri,
        fallbackTitle: String,
        fallbackType: DocumentType
    ): Result<WorkspaceFile> =
        runCatching {
            NexoraLogger.i(
                TAG,
                "event=saf_recovery_selection sessionId=$sessionId uri=$uri type=${fallbackType.name}"
            )
            storageAccessRepository.persistUriPermission(uri, DocumentAccessMode.READ_WRITE).getOrThrow()
            uri.requirePersistedReadWrite()
            val resolvedFile = fileOpenManager.buildWorkspaceFile(uri)
            val file = resolvedFile.copy(
                name = resolvedFile.name.ifBlank { fallbackTitle },
                type = resolvedFile.type
            )
            recentFilesRepository.addRecent(file)
            sessionRepository.updateFileMetadata(
                sessionId = sessionId,
                uri = file.path,
                title = file.name,
                type = file.type
            )
            sessionRepository.touch(sessionId)
            NexoraLogger.i(
                TAG,
                "event=session_document_restored sessionId=$sessionId uri=${file.path} type=${file.type.name}"
            )
            file
        }.onFailure { error ->
            NexoraLogger.e(TAG, "event=session_document_restore_failure sessionId=$sessionId uri=$uri", error)
            _state.value = _state.value.copy(error = userFacingOpenError(error))
        }

    fun openUntitled(type: DocumentType, title: String) {
        viewModelScope.launch {
            val session = sessionRepository.openOrCreateUntitled(type, title)
            _state.value = _state.value.copy(activeTabId = session.sessionId)
            sessionRepository.touch(session.sessionId)
        }
    }

    fun activateTab(tab: EditorTab) {
        viewModelScope.launch {
            _state.value = _state.value.copy(activeTabId = tab.fileId)
            sessionRepository.touch(tab.fileId)
        }
    }

    fun markDirty(sessionId: String, dirty: Boolean) {
        viewModelScope.launch {
            sessionRepository.markDirty(sessionId, dirty)
        }
    }

    fun scheduleAutosave(sessionId: String, payload: String) {
        autosaveRequests.tryEmit(AutosaveRequest(sessionId, payload))
    }

    fun updateFileMetadata(sessionId: String, uri: String, title: String, type: DocumentType) {
        viewModelScope.launch {
            sessionRepository.updateFileMetadata(sessionId, uri, title, type)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    @OptIn(FlowPreview::class)
    private fun bindAutosavePipeline() {
        autosaveRequests
            .debounce(1200)
            .onEach { request ->
                sessionRepository.updateAutosave(request.sessionId, request.payload, request.payload)
            }
            .launchIn(viewModelScope)
    }

    private fun DocumentSession.toTab(): EditorTab = EditorTab(
        fileId = sessionId,
        title = title,
        dirty = dirty,
        type = type,
        sourcePath = fileUri ?: ""
    )

    private fun Uri.requirePersistedReadWrite() {
        storageAccessRepository.validatePersistedUriPermission(this, DocumentAccessMode.READ_WRITE)
            .takeIf { it.allows(DocumentAccessMode.READ_WRITE) }
            ?: throw SafPermissionMissingException(this, DocumentAccessMode.READ_WRITE)
    }

    private fun userFacingOpenError(error: Throwable): String =
        when (error) {
            is SafPermissionMissingException ->
                "Document access needs to be restored. Reopen the document from the file picker to continue."
            is SecurityException ->
                "Android did not grant lasting access to this document. Reopen it from the file picker."
            is FolderSelectedForOpenException ->
                "That is a folder. Open it in Files, then choose a document inside."
            is DocumentUnavailableException ->
                "This file is no longer available. It may have been moved or deleted."
            is DocumentUnreadableException ->
                "This file cannot be read. Try reopening it from Android's file picker."
            is EmptyDocumentException ->
                "This file appears to be empty."
            else ->
                "Could not open this document. Try reopening it from the file picker."
        }
}

private data class AutosaveRequest(
    val sessionId: String,
    val payload: String
)
