package com.nexora.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.core.data.session.DocumentSessionRepository
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
    private val sessionRepository: DocumentSessionRepository
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
}

private data class AutosaveRequest(
    val sessionId: String,
    val payload: String
)
