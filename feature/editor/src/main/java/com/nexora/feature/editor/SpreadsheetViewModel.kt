package com.nexora.feature.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.core.data.document.XlsxDocumentRepository
import com.nexora.core.data.document.XlsxSession
import com.nexora.core.model.CellData
import com.nexora.core.model.CellRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val MAX_VISIBLE_ROWS = 200
private const val MAX_VISIBLE_COLUMNS = 26

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class SpreadsheetViewModel(
    private val repository: XlsxDocumentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SpreadsheetUiState())
    val state: StateFlow<SpreadsheetUiState> = _state.asStateFlow()

    private var session: XlsxSession? = null
    private val editingManager = SpreadsheetEditingManager()

    // -----------------------------------------------------------------------
    // Load / Save
    // -----------------------------------------------------------------------

    fun load(contentResolverProvider: () -> android.content.ContentResolver, uri: Uri?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            session?.close()
            session = try {
                if (uri == null) {
                    repository.createEmptySession()
                } else {
                    repository.loadSession(contentResolverProvider(), uri)
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = error.message ?: "Failed to load spreadsheet")
                }
                null
            }

            val activeSession = session ?: return@launch
            val sheetNames = activeSession.sheetNames()
            val (maxRow, maxColumn) = if (sheetNames.isNotEmpty()) {
                activeSession.maxRowColumn(0)
            } else {
                0 to 0
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    sheetNames = sheetNames,
                    activeSheetIndex = 0,
                    maxRow = maxRow,
                    maxColumn = maxColumn,
                    selectionState = CellSelectionState(),
                    editorState = CellEditorState(),
                    formulaBarText = "",
                    cells = emptyMap()
                )
            }
            refreshVisibleCells()
        }
    }

    fun save(contentResolverProvider: () -> android.content.ContentResolver, uri: Uri?) {
        val activeSession = session ?: return
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSession(contentResolverProvider(), uri, activeSession)
        }
    }

    // -----------------------------------------------------------------------
    // Cell selection
    // FIX #7: getCellData moved to IO dispatcher — never blocks main thread.
    // -----------------------------------------------------------------------

    fun selectCell(ref: CellRef) {
        val activeSession = session ?: return
        viewModelScope.launch {
            val cellData = withContext(Dispatchers.IO) {
                activeSession.getCellData(state.value.activeSheetIndex, ref.row, ref.column)
            }
            _state.update { editingManager.selectCell(it, ref, cellData?.raw ?: "") }
        }
    }

    // -----------------------------------------------------------------------
    // Edit lifecycle
    // -----------------------------------------------------------------------

    fun beginEdit(ref: CellRef) {
        val activeSession = session ?: return
        viewModelScope.launch {
            val cellData = withContext(Dispatchers.IO) {
                activeSession.getCellData(state.value.activeSheetIndex, ref.row, ref.column)
            }
            _state.update { editingManager.beginEdit(it, ref, cellData?.raw ?: "") }
        }
    }

    /**
     * Begin editing the currently selected cell by immediately replacing its content
     * with [text]. Called when the user types a printable character while a cell is
     * selected but not yet in edit mode (Excel "overwrite" behaviour).
     *
     * FIX #2: Guards against no-selection state gracefully.
     */
    fun beginEditWithText(text: String) {
        val current = state.value
        val ref = current.selectionState.ref ?: return
        // Override with the typed character — the CellEditorOverlay will position
        // the cursor at the end automatically via TextFieldValue initialisation.
        _state.update { editingManager.beginEdit(it, ref, text) }
    }

    fun updateEditorText(text: String) {
        _state.update { editingManager.updateEditorText(it, text) }
    }

    /**
     * Commit the current in-progress edit and persist it to the session.
     * Formula recalculation and cell cache refresh happen on background threads.
     *
     * FIX #1 (partial): Returns the target cell ref AFTER committing so that
     * [commitEditAndMove] can schedule the move inside the coroutine continuation
     * rather than racing against the state update.
     */
    fun commitEdit() {
        val activeSession = session ?: return
        val current = state.value
        val ref = current.editorState.ref ?: return
        val text = current.editorState.text
        val sheetIndex = current.activeSheetIndex

        // Immediately clear editor state in UI — gives instant visual feedback.
        _state.update { editingManager.commitEdit(it, isRecalculating = true) }

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                activeSession.setCellValue(sheetIndex, ref.row, ref.column, text)
                activeSession.recalculateAll()  // clears + re-evaluates all formula caches
            }
            refreshVisibleCells()
            _state.update { it.copy(isRecalculating = false) }
        }
    }

    /**
     * Commit the current edit and move selection by [rowDelta]/[colDelta].
     *
     * FIX #1: The move now happens synchronously against the state AFTER the
     * commit clears editorState, not racing a coroutine. We capture bounds
     * before launching so they're consistent. Selection move itself is cheap
     * (just a state update + async IO getCellData).
     */
    fun commitEditAndMove(rowDelta: Int, colDelta: Int) {
        val current = state.value
        val editorRef = current.editorState.ref ?: return
        val activeSession = session ?: return
        val text = current.editorState.text
        val sheetIndex = current.activeSheetIndex

        // Compute the destination cell with the bounds we have right now.
        val newRow = (editorRef.row + rowDelta)
            .coerceAtLeast(0)
            .coerceAtMost(current.maxRow.coerceAtLeast(0))
        val newCol = (editorRef.column + colDelta)
            .coerceAtLeast(0)
            .coerceAtMost(current.maxColumn.coerceAtLeast(0))
        val targetRef = CellRef(newRow, newCol)

        // Immediately update editor state (clears overlay) + set provisional selection.
        _state.update {
            editingManager.commitEdit(it, isRecalculating = true)
                .copy(selectionState = CellSelectionState(ref = targetRef, row = newRow, column = newCol))
        }

        viewModelScope.launch {
            // 1. Persist + recalculate in background.
            withContext(Dispatchers.Default) {
                activeSession.setCellValue(sheetIndex, editorRef.row, editorRef.column, text)
                activeSession.recalculateAll()
            }
            // 2. Refresh the visible cell cache.
            refreshVisibleCells()
            // 3. Load the new cell's raw value for the formula bar.
            val newCellData = withContext(Dispatchers.IO) {
                activeSession.getCellData(sheetIndex, newRow, newCol)
            }
            _state.update {
                it.copy(
                    isRecalculating = false,
                    formulaBarText = newCellData?.raw ?: ""
                )
            }
        }
    }

    /**
     * Move selection without committing an edit.
     *
     * FIX #7: getCellData moved to IO thread.
     */
    fun moveSelection(rowDelta: Int, colDelta: Int) {
        val current = state.value
        val ref = current.selectionState.ref ?: return
        val newRow = (ref.row + rowDelta)
            .coerceAtLeast(0)
            .coerceAtMost(current.maxRow.coerceAtLeast(0))
        val newCol = (ref.column + colDelta)
            .coerceAtLeast(0)
            .coerceAtMost(current.maxColumn.coerceAtLeast(0))
        selectCell(CellRef(newRow, newCol))
    }

    fun cancelEdit() {
        _state.update { editingManager.cancelEdit(it) }
    }

    fun updateFormulaBar(text: String) {
        _state.update { editingManager.updateFormulaBar(it, text) }
    }

    fun selectSheet(index: Int) {
        val activeSession = session ?: return
        val (maxRow, maxColumn) = activeSession.maxRowColumn(index)
        _state.update {
            it.copy(
                activeSheetIndex = index,
                maxRow = maxRow,
                maxColumn = maxColumn,
                selectionState = CellSelectionState(),
                editorState = CellEditorState(),
                formulaBarText = "",
                cells = emptyMap()
            )
        }
        refreshVisibleCells()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun refreshVisibleCells() {
        val activeSession = session ?: return
        val current = state.value
        val sheetIndex = current.activeSheetIndex
        val maxRow = current.maxRow.coerceAtLeast(0).coerceAtMost(MAX_VISIBLE_ROWS)
        val maxCol = current.maxColumn.coerceAtLeast(0).coerceAtMost(MAX_VISIBLE_COLUMNS)
        viewModelScope.launch(Dispatchers.IO) {
            val cells = mutableMapOf<CellRef, CellData>()
            for (row in 0..maxRow) {
                for (col in 0..maxCol) {
                    val data = activeSession.getCellData(sheetIndex, row, col) ?: continue
                    if (data.raw.isNotBlank()) {
                        cells[CellRef(row = row, column = col)] = data
                    }
                }
            }
            _state.update { s ->
                if (s.activeSheetIndex != sheetIndex) s else s.copy(cells = cells)
            }
        }
    }

    override fun onCleared() {
        session?.close()
        session = null
        super.onCleared()
    }
}

// ---------------------------------------------------------------------------
// UI State models
// ---------------------------------------------------------------------------

data class SpreadsheetUiState(
    val isLoading: Boolean = false,
    val isRecalculating: Boolean = false,
    val error: String? = null,
    val sheetNames: List<String> = emptyList(),
    val activeSheetIndex: Int = 0,
    val maxRow: Int = 0,
    val maxColumn: Int = 0,
    val selectionState: CellSelectionState = CellSelectionState(),
    val editorState: CellEditorState = CellEditorState(),
    val formulaBarText: String = "",
    val cells: Map<CellRef, CellData> = emptyMap()
)

data class CellSelectionState(
    val ref: CellRef? = null,
    val row: Int? = null,
    val column: Int? = null
)

data class CellEditorState(
    val isEditing: Boolean = false,
    val ref: CellRef? = null,
    val text: String = ""
)

// ---------------------------------------------------------------------------
// SpreadsheetEditingManager — pure state transitions, no side effects
// ---------------------------------------------------------------------------

class SpreadsheetEditingManager {

    fun selectCell(state: SpreadsheetUiState, ref: CellRef, rawValue: String): SpreadsheetUiState =
        state.copy(
            selectionState = CellSelectionState(ref = ref, row = ref.row, column = ref.column),
            formulaBarText = rawValue
        )

    fun beginEdit(state: SpreadsheetUiState, ref: CellRef, rawValue: String): SpreadsheetUiState =
        state.copy(
            selectionState = CellSelectionState(ref = ref, row = ref.row, column = ref.column),
            editorState = CellEditorState(isEditing = true, ref = ref, text = rawValue),
            formulaBarText = rawValue
        )

    fun updateEditorText(state: SpreadsheetUiState, text: String): SpreadsheetUiState =
        state.copy(
            editorState = state.editorState.copy(text = text),
            formulaBarText = text
        )

    fun updateFormulaBar(state: SpreadsheetUiState, text: String): SpreadsheetUiState =
        if (state.editorState.isEditing) {
            updateEditorText(state, text)
        } else if (state.selectionState.ref != null) {
            state.copy(
                editorState = CellEditorState(
                    isEditing = true,
                    ref = state.selectionState.ref,
                    text = text
                ),
                formulaBarText = text
            )
        } else {
            state.copy(formulaBarText = text)
        }

    fun commitEdit(state: SpreadsheetUiState, isRecalculating: Boolean): SpreadsheetUiState =
        state.copy(
            editorState = CellEditorState(),
            isRecalculating = isRecalculating
        )

    fun cancelEdit(state: SpreadsheetUiState): SpreadsheetUiState =
        state.copy(editorState = CellEditorState())
}

// ---------------------------------------------------------------------------
// ViewModelFactory
// ---------------------------------------------------------------------------

class SpreadsheetViewModelFactory(
    private val repository: XlsxDocumentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpreadsheetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpreadsheetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
