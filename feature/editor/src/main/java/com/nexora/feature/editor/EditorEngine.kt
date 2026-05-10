package com.nexora.feature.editor

import com.nexora.core.model.EditorTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EditorState(
    val tabs: List<EditorTab> = emptyList(),
    val activeTabId: String? = null,
    val autosaveEnabled: Boolean = true
)

/**
 * Core editor runtime state holder.
 *
 * This engine is intentionally UI-agnostic so it can later back
 * document, spreadsheet, and presentation editing surfaces.
 */
class EditorEngine {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun openTab(tab: EditorTab) {
        _state.update { current ->
            val existing = current.tabs.any { it.fileId == tab.fileId }
            current.copy(
                tabs = if (existing) {
                    current.tabs.map { if (it.fileId == tab.fileId) tab else it }
                } else {
                    current.tabs + tab
                },
                activeTabId = tab.fileId
            )
        }
    }

    fun activateTab(fileId: String) {
        _state.update { current ->
            current.copy(activeTabId = fileId)
        }
    }

    fun markDirty(fileId: String, dirty: Boolean) {
        _state.update { current ->
            current.copy(
                tabs = current.tabs.map { tab ->
                    if (tab.fileId == fileId) tab.copy(dirty = dirty) else tab
                }
            )
        }
    }
}
