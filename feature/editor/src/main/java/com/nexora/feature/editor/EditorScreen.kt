package com.nexora.feature.editor

import android.net.Uri
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.animateColorAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraLogoMark
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraToolbarButton
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.common.logging.NexoraLogger
import com.nexora.core.data.document.DocxDocumentRepository
import com.nexora.core.data.document.PptxDocumentRepository
import com.nexora.core.data.document.HybridPdfRenderSession
import com.nexora.core.data.document.PdfPageRenderer
import com.nexora.core.data.document.TextDocumentRepository
import com.nexora.core.data.document.XlsxDocumentRepository
import com.nexora.core.model.DocumentType
import com.nexora.core.model.DocxDocument
import com.nexora.core.model.CellRef
import com.nexora.core.model.DocumentBlock
import com.nexora.core.model.HeadingBlock
import com.nexora.core.model.ImageBlock
import com.nexora.core.model.ListStyle
import com.nexora.core.model.ParagraphAlignment
import com.nexora.core.model.ParagraphBlock
import com.nexora.core.model.PptxDocument
import com.nexora.core.model.PptxImageElement
import com.nexora.core.model.PptxTextElement
import com.nexora.core.model.TableBlock
import com.nexora.core.model.TextRun
import com.nexora.core.model.EditorTab
import com.nexora.core.model.WorkspaceFile
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    openedFile: WorkspaceFile? = null,
    onDone: () -> Unit = {}
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val editorViewModel: EditorViewModel = hiltViewModel()
    val docxRepository = remember { DocxDocumentRepository() }
    val xlsxRepository = remember { XlsxDocumentRepository() }
    val pptxRepository = remember { PptxDocumentRepository() }
    val textRepository = remember { TextDocumentRepository() }
    val spreadsheetViewModel: SpreadsheetViewModel = viewModel(
        factory = SpreadsheetViewModelFactory(xlsxRepository)
    )
    val scope = rememberCoroutineScope()
    val state by editorViewModel.state.collectAsState()
    val activeTab = state.tabs.firstOrNull { it.fileId == state.activeTabId }
    val activeSession = state.sessions.firstOrNull { it.sessionId == activeTab?.fileId }
    var activeMode by remember(openedFile?.id) {
        mutableStateOf(openedFile?.type.toEditorMode())
    }
    var selectedTool by remember { mutableStateOf("Select") }
    var toolbarPosition by remember { mutableStateOf(ToolbarPosition.Floating) }
    var chromeVisible by remember { mutableStateOf(true) }
    var toolsExpanded by remember { mutableStateOf(false) }
    var distractionFree by remember { mutableStateOf(true) }
    var fullscreenMode by remember { mutableStateOf(false) }
    var docxState by remember { mutableStateOf(DocxUiState()) }
    var pptxState by remember { mutableStateOf(PptxUiState()) }
    var textState by remember { mutableStateOf(TextUiState()) }
    val spreadsheetState by spreadsheetViewModel.state.collectAsState()
    var editorError by remember { mutableStateOf<String?>(null) }

    val docxSaveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ) { uri ->
        val tab = activeTab ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val updatedDocument = docxState.document?.let { existing ->
                    if (docxState.isEditing) docxState.editText.toDocxDocument() else existing
                } ?: return@runCatching
                docxRepository.saveDocx(contentResolver, uri, updatedDocument)
                editorViewModel.updateFileMetadata(tab.fileId, uri.toString(), tab.title, DocumentType.DOC)
                editorViewModel.markDirty(tab.fileId, false)
            }.onFailure { error ->
                NexoraLogger.e("EditorScreen", "Save As DOCX failed", error)
                editorError = error.message ?: "Save As failed"
            }
        }
    }

    val textSaveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val tab = activeTab ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                textRepository.saveText(contentResolver, uri, textState.content)
                editorViewModel.updateFileMetadata(tab.fileId, uri.toString(), tab.title, DocumentType.TEXT)
                editorViewModel.markDirty(tab.fileId, false)
            }.onFailure { error ->
                NexoraLogger.e("EditorScreen", "Save As text failed", error)
                editorError = error.message ?: "Save As failed"
            }
        }
    }

    val pdfSaveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val tab = activeTab ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val sourceUri = tab.sourcePath.takeIf { it.startsWith("content://") }?.let(Uri::parse)
                    ?: error("PDF source is not available")
                copyDocument(contentResolver, sourceUri, uri)
                editorViewModel.updateFileMetadata(tab.fileId, uri.toString(), tab.title, DocumentType.PDF)
                editorViewModel.markDirty(tab.fileId, false)
            }.onFailure { error ->
                NexoraLogger.e("EditorScreen", "Save As PDF failed", error)
                editorError = error.message ?: "Save As failed"
            }
        }
    }

    val xlsxSaveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val tab = activeTab ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                spreadsheetViewModel.save({ contentResolver }, uri)
                editorViewModel.updateFileMetadata(tab.fileId, uri.toString(), tab.title, DocumentType.SHEET)
                editorViewModel.markDirty(tab.fileId, false)
            }.onFailure { error ->
                NexoraLogger.e("EditorScreen", "Save As XLSX failed", error)
                editorError = error.message ?: "Save As failed"
            }
        }
    }

    // FIX #5: Wrap in remember so the controller reference is stable across recompositions.
    // Lambda fields in data classes are never structurally equal, so the old inline
    // construction rebuilt the controller (and fully recomposed SpreadsheetEditor) every frame.
    val spreadsheetController = remember(spreadsheetViewModel) {
        SpreadsheetInteractionController(
            onCellSelected = { ref -> spreadsheetViewModel.selectCell(ref) },
            onCellTap = { ref -> spreadsheetViewModel.beginEdit(ref) },
            onBeginEdit = { ref -> spreadsheetViewModel.beginEdit(ref) },
            onBeginEditWithText = { text ->
                spreadsheetViewModel.beginEditWithText(text)
            },
            onEditorTextChange = { updated ->
                spreadsheetViewModel.updateEditorText(updated)
            },
            onCommitEdit = { spreadsheetViewModel.commitEdit() },
            onCommitEditAndMove = { rowDelta, colDelta ->
                spreadsheetViewModel.commitEditAndMove(rowDelta, colDelta)
            },
            onCancelEdit = { spreadsheetViewModel.cancelEdit() },
            onMoveSelection = { rowDelta, colDelta ->
                spreadsheetViewModel.moveSelection(rowDelta, colDelta)
            },
            onFormulaBarChange = { updated ->
                spreadsheetViewModel.updateFormulaBar(updated)
            },
            onSheetSelected = { sheetIndex ->
                spreadsheetViewModel.selectSheet(sheetIndex)
            }
        )
    }

    LaunchedEffect(openedFile?.id) {
        openedFile?.let { file ->
            if (file.path.startsWith("content://")) {
                editorViewModel.openFromFile(file)
            } else {
                editorViewModel.openUntitled(file.type, file.name)
            }
        }
    }

    LaunchedEffect(activeTab?.fileId) {
        activeTab?.let { activeMode = it.type.toEditorMode() }
    }

    // Mark the active spreadsheet tab dirty when a committed edit finishes recalculating.
    // isRecalculating transitions false->true on commit start, and true->false on completion.
    LaunchedEffect(spreadsheetState.isRecalculating) {
        if (!spreadsheetState.isRecalculating && spreadsheetState.sheetNames.isNotEmpty()) {
            activeTab?.fileId?.let { fileId -> editorViewModel.markDirty(fileId, true) }
        }
    }

    LaunchedEffect(activeTab?.fileId, activeTab?.sourcePath, activeTab?.type, activeSession?.autosavePayload) {
        val uri = activeTab?.sourcePath
            ?.takeIf { it.startsWith("content://") }
            ?.let(Uri::parse)
        val autosavePayload = activeSession?.autosavePayload
        when (activeTab?.type) {
            DocumentType.DOC -> {
                if (uri != null) {
                    docxState = DocxUiState(isLoading = true)
                    docxState = runCatching {
                        val document = docxRepository.loadDocx(contentResolver, uri)
                        DocxUiState(
                            isLoading = false,
                            document = document,
                            editText = document.toPlainText()
                        )
                    }.getOrElse { error ->
                        if (!autosavePayload.isNullOrBlank()) {
                            DocxUiState(
                                isLoading = false,
                                document = autosavePayload.toDocxDocument(),
                                editText = autosavePayload,
                                isEditing = true
                            )
                        } else {
                            DocxUiState(isLoading = false, error = error.message ?: "Failed to load document")
                        }
                    }
                } else {
                    val title = activeTab?.title ?: "Untitled Document"
                    val document = DocxDocument(
                        blocks = listOf(
                            ParagraphBlock(runs = listOf(TextRun(title)))
                        )
                    )
                    docxState = if (!autosavePayload.isNullOrBlank()) {
                        DocxUiState(document = autosavePayload.toDocxDocument(), editText = autosavePayload, isEditing = true)
                    } else {
                        DocxUiState(document = document, editText = document.toPlainText())
                    }
                }
                pptxState = PptxUiState()
                textState = TextUiState()
            }
            DocumentType.SHEET -> {
                spreadsheetViewModel.load({ contentResolver }, uri)
                docxState = DocxUiState()
                pptxState = PptxUiState()
                textState = TextUiState()
            }
            DocumentType.SLIDE -> {
                if (uri != null) {
                    pptxState = PptxUiState(isLoading = true)
                    pptxState = runCatching {
                        val document = pptxRepository.loadPptx(contentResolver, uri)
                        PptxUiState(document = document)
                    }.getOrElse { error ->
                        PptxUiState(error = error.message ?: "Failed to load presentation")
                    }
                } else {
                    pptxState = PptxUiState(document = PptxDocument())
                }
                docxState = DocxUiState()
                textState = TextUiState()
            }
            DocumentType.TEXT -> {
                if (uri != null) {
                    textState = TextUiState(isLoading = true)
                    textState = runCatching {
                        val content = textRepository.loadText(contentResolver, uri)
                        TextUiState(content = content)
                    }.getOrElse { error ->
                        if (!autosavePayload.isNullOrBlank()) {
                            TextUiState(content = autosavePayload)
                        } else {
                            TextUiState(error = error.message ?: "Failed to load text")
                        }
                    }
                } else {
                    textState = TextUiState(content = autosavePayload ?: "")
                }
                docxState = DocxUiState()
                pptxState = PptxUiState()
            }
            else -> {
                docxState = DocxUiState()
                pptxState = PptxUiState()
                textState = TextUiState()
            }
        }
    }

    val readingMode = fullscreenMode || distractionFree
    val showEditorTools = chromeVisible && toolsExpanded && activeMode != EditorMode.Pdf
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val saveAsAction = {
                val tab = activeTab
                if (tab != null) {
                    when (tab.type) {
                        DocumentType.DOC -> docxSaveAsLauncher.launch(tab.title)
                        DocumentType.PDF -> pdfSaveAsLauncher.launch(tab.title)
                        DocumentType.TEXT -> textSaveAsLauncher.launch(tab.title)
                        DocumentType.SHEET -> xlsxSaveAsLauncher.launch(tab.title)
                        else -> editorError = "Save As is not available for this document type yet."
                    }
                }
            }
            val saveAction = saveAction@{
                val tab = activeTab
                if (tab != null) {
                    val uri = tab.sourcePath
                        .takeIf { it.startsWith("content://") }
                        ?.let(Uri::parse)
                    when (tab.type) {
                        DocumentType.DOC -> {
                            val updatedDocument = docxState.document?.let { existing ->
                                if (docxState.isEditing) docxState.editText.toDocxDocument() else existing
                            }
                            if (uri != null && updatedDocument != null) {
                                scope.launch {
                                    docxRepository.saveDocx(contentResolver, uri, updatedDocument)
                                    editorViewModel.markDirty(tab.fileId, false)
                                }
                                return@saveAction
                            }
                        }
                        DocumentType.SHEET -> {
                            spreadsheetViewModel.save({ contentResolver }, uri)
                            editorViewModel.markDirty(tab.fileId, false)
                            return@saveAction
                        }
                        DocumentType.SLIDE -> {
                            val document = pptxState.document
                            if (uri != null && document != null) {
                                scope.launch {
                                    pptxRepository.savePptx(contentResolver, uri, document)
                                    editorViewModel.markDirty(tab.fileId, false)
                                }
                                return@saveAction
                            }
                        }
                        DocumentType.TEXT -> {
                            if (uri != null) {
                                scope.launch {
                                    textRepository.saveText(contentResolver, uri, textState.content)
                                    editorViewModel.markDirty(tab.fileId, false)
                                }
                                return@saveAction
                            }
                        }
                        else -> Unit
                    }
                    editorViewModel.markDirty(tab.fileId, false)
                }
            }
            if (!readingMode || chromeVisible) {
                EditorHeader(
                    activeMode = activeMode,
                    activeTab = activeTab,
                    toolbarPosition = toolbarPosition,
                    fullscreenMode = fullscreenMode,
                    distractionFree = distractionFree,
                    onDone = onDone,
                    onSaveAs = saveAsAction,
                    onSave = saveAction,
                    onToggleChrome = { chromeVisible = !chromeVisible },
                    onToggleTools = { toolsExpanded = !toolsExpanded },
                    onToolbarPositionChange = { toolbarPosition = it },
                    onToggleFullscreen = {
                        fullscreenMode = !fullscreenMode
                        chromeVisible = !fullscreenMode
                    },
                    onToggleDistractionFree = {
                        distractionFree = !distractionFree
                        chromeVisible = !distractionFree
                    },
                )
            }
            if (editorError != null) {
                NexoraCard(color = NexoraError.copy(alpha = 0.12f), contentPadding = 12.dp) {
                    Text(
                        text = editorError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraError
                    )
                    Spacer(Modifier.height(6.dp))
                    NexoraToolbarButton(label = "Dismiss", onClick = { editorError = null })
                }
            }
            if ((!readingMode || chromeVisible) && state.tabs.size > 1) {
                OpenTabStrip(
                    tabs = state.tabs,
                    activeTabId = state.activeTabId,
                    onSelectTab = { tab ->
                        editorViewModel.activateTab(tab)
                        activeMode = tab.type.toEditorMode()
                    }
                )
            }
            if (showEditorTools && toolbarPosition == ToolbarPosition.Top) {
                    EditorToolbar(
                        activeMode = activeMode,
                        selectedTool = selectedTool,
                        onToolSelected = { selectedTool = it }
                    )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clickable {
                        chromeVisible = true
                        if (readingMode) toolsExpanded = false
                    }
            ) {
                when (activeMode) {
                    EditorMode.Document -> DocumentEditor(
                        activeTab = activeTab,
                        state = docxState,
                        onToggleEdit = { docxState = docxState.copy(isEditing = !docxState.isEditing) },
                        onEditTextChange = {
                            docxState = docxState.copy(editText = it)
                            activeTab?.fileId?.let { fileId ->
                                editorViewModel.markDirty(fileId, true)
                                editorViewModel.scheduleAutosave(fileId, it)
                            }
                        }
                    )
                    EditorMode.Spreadsheet -> SpreadsheetEditor(
                        activeTab = activeTab,
                        state = spreadsheetState,
                        controller = spreadsheetController
                    )
                    EditorMode.Presentation -> PresentationEditor(
                        activeTab = activeTab,
                        state = pptxState,
                        onSlideSelected = { index ->
                            pptxState = pptxState.copy(activeSlideIndex = index)
                        },
                        onTextUpdated = { slideIndex, elementId, newText ->
                            pptxState = pptxState.updateText(slideIndex, elementId, newText)
                            activeTab?.fileId?.let { fileId ->
                                editorViewModel.markDirty(fileId, true)
                            }
                        }
                    )
                    EditorMode.Pdf -> PdfReader(activeTab = activeTab)
                    EditorMode.Text -> TextEditor(
                        activeTab = activeTab,
                        state = textState,
                        onContentChanged = { updated ->
                            textState = textState.copy(content = updated)
                            activeTab?.fileId?.let { fileId ->
                                editorViewModel.markDirty(fileId, true)
                                editorViewModel.scheduleAutosave(fileId, updated)
                            }
                        }
                    )
                }
                if (readingMode && !chromeVisible) {
                    NexoraToolbarButton(
                        label = "More",
                        selected = true,
                        onClick = { chromeVisible = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
                if (showEditorTools && toolbarPosition == ToolbarPosition.Floating) {
                    FloatingEditorToolbar(
                        activeMode = activeMode,
                        selectedTool = selectedTool,
                        onToolSelected = { selectedTool = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    )
                }
            }

            if (showEditorTools && toolbarPosition == ToolbarPosition.Bottom) {
                EditorToolbar(
                    activeMode = activeMode,
                    selectedTool = selectedTool,
                    onToolSelected = { selectedTool = it }
                )
            }
        }
    }
}

@Composable
private fun EditorHeader(
    activeMode: EditorMode,
    activeTab: EditorTab?,
    toolbarPosition: ToolbarPosition,
    fullscreenMode: Boolean,
    distractionFree: Boolean,
    onDone: () -> Unit,
    onSaveAs: () -> Unit,
    onSave: () -> Unit,
    onToggleChrome: () -> Unit,
    onToggleTools: () -> Unit,
    onToolbarPositionChange: (ToolbarPosition) -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleDistractionFree: () -> Unit
) {
    val title = activeTab?.title ?: activeMode.fileName
    val subtitle = when {
        fullscreenMode -> "Fullscreen document view"
        distractionFree -> "Distraction-free reading"
        activeTab?.dirty == true -> "Unsaved changes"
        activeTab?.sourcePath?.startsWith("content://") == true -> "Local file"
        else -> "Autosaved"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexoraToolbarButton(label = "<", onClick = onDone)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            NexoraToolbarButton(label = if (activeTab?.dirty == true) "Save*" else "Saved", selected = activeTab?.dirty == true, onClick = onSave)
            Spacer(Modifier.width(6.dp))
            NexoraToolbarButton(label = "Edit", selected = false, onClick = onToggleTools)
            Spacer(Modifier.width(6.dp))
            NexoraToolbarButton(label = "...", onClick = onSaveAs)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NexoraPill(label = "Read", selected = distractionFree, onClick = onToggleDistractionFree)
            NexoraPill(label = "Full", selected = fullscreenMode, onClick = onToggleFullscreen)
            NexoraPill(label = "Hide", selected = false, onClick = onToggleChrome)
            ToolbarPosition.entries.forEach { position ->
                NexoraPill(
                    label = position.label,
                    selected = toolbarPosition == position,
                    onClick = { onToolbarPositionChange(position) }
                )
            }
        }
    }
}

@Composable
private fun OpenTabStrip(
    tabs: List<EditorTab>,
    activeTabId: String?,
    onSelectTab: (EditorTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEach { tab ->
            NexoraPill(
                label = if (tab.dirty) "${tab.title} *" else tab.title,
                selected = tab.fileId == activeTabId,
                onClick = { onSelectTab(tab) }
            )
        }
    }
}

@Composable
private fun DocumentEditor(
    activeTab: EditorTab?,
    state: DocxUiState,
    onToggleEdit: () -> Unit,
    onEditTextChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Loading document...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoraError
                        )
                    }
                }
                state.isEditing -> {
                    OutlinedTextField(
                        value = state.editText,
                        onValueChange = onEditTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF111827)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = NexoraPrimary
                        )
                    )
                }
                state.document != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        DocxDocumentView(
                            document = state.document,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Open a DOCX file to start editing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        NexoraToolbarButton(
            label = if (state.isEditing) "Preview" else "Edit",
            selected = state.isEditing,
            onClick = onToggleEdit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        )
    }
}

@Composable
private fun DocxDocumentView(document: DocxDocument, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 10.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                document.blocks.forEach { block ->
                    when (block) {
                        is HeadingBlock -> {
                            Text(
                                text = runsToAnnotatedString(block.runs),
                                style = when (block.level) {
                                    1 -> MaterialTheme.typography.headlineMedium
                                    2 -> MaterialTheme.typography.headlineSmall
                                    else -> MaterialTheme.typography.titleLarge
                                },
                                color = Color(0xFF111827)
                            )
                        }
                        is ParagraphBlock -> {
                            Text(
                                text = runsToAnnotatedString(block.runs, block.listStyle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF111827),
                                textAlign = block.alignment.toTextAlign()
                            )
                        }
                        is TableBlock -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                block.rows.forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { cell ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(1.dp, Color(0xFFD4DAE6))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = cell,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF111827)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is ImageBlock -> {
                            Text(
                                text = "${block.description} (image)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

private fun runsToAnnotatedString(runs: List<TextRun>, listStyle: ListStyle = ListStyle.NONE): AnnotatedString {
    return AnnotatedString.Builder().apply {
        if (listStyle == ListStyle.BULLET) {
            append("• ")
        }
        runs.forEach { run ->
            val style = SpanStyle(
                fontWeight = if (run.bold) FontWeight.Bold else null,
                fontStyle = if (run.italic) FontStyle.Italic else null,
                textDecoration = if (run.underline) TextDecoration.Underline else null
            )
            withStyle(style) { append(run.text) }
        }
    }.toAnnotatedString()
}

private fun ParagraphAlignment.toTextAlign(): TextAlign = when (this) {
    ParagraphAlignment.CENTER -> TextAlign.Center
    ParagraphAlignment.END -> TextAlign.End
    ParagraphAlignment.JUSTIFY -> TextAlign.Justify
    ParagraphAlignment.START -> TextAlign.Start
}

private fun DocxDocument.toPlainText(): String {
    val builder = StringBuilder()
    blocks.forEach { block ->
        when (block) {
            is HeadingBlock -> builder.append(block.runs.joinToString("") { it.text })
            is ParagraphBlock -> {
                if (block.listStyle == ListStyle.BULLET) builder.append("• ")
                builder.append(block.runs.joinToString("") { it.text })
            }
            is TableBlock -> {
                block.rows.forEach { row ->
                    builder.append(row.joinToString("\t"))
                    builder.append("\n")
                }
                builder.append("\n")
                return@forEach
            }
            is ImageBlock -> builder.append("[Image: ${block.description}]")
            else -> Unit
        }
        builder.append("\n\n")
    }
    return builder.toString().trimEnd()
}

private fun String.toDocxDocument(): DocxDocument {
    val paragraphs = split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { text ->
            ParagraphBlock(runs = listOf(TextRun(text)))
        }
    return DocxDocument(blocks = paragraphs)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpreadsheetEditor(
    activeTab: EditorTab?,
    state: SpreadsheetUiState,
    controller: SpreadsheetInteractionController
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val horizontalScroll = rememberScrollState()
    val density = LocalDensity.current
    val cellWidthPx = with(density) { 110.dp.toPx() }
    val rowHeaderWidthPx = with(density) { 46.dp.toPx() }

    NexoraCard(contentPadding = 0.dp, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = activeTab?.title ?: "Spreadsheet",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF111827)
            )

            when {
                state.isLoading -> {
                    Text("Loading spreadsheet...", color = Color(0xFF6B7280))
                }
                state.error != null -> {
                    Text(state.error, color = NexoraError)
                }
                state.sheetNames.isEmpty() -> {
                    Text("Open an XLSX file to start editing.", color = Color(0xFF6B7280))
                }
                else -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.sheetNames.forEachIndexed { index, sheetName ->
                            NexoraPill(
                                label = sheetName.ifBlank { "Sheet ${index + 1}" },
                                selected = index == state.activeSheetIndex,
                                onClick = { controller.onSheetSelected(index) }
                            )
                        }
                    }

                    val maxRow = maxOf(state.maxRow + 1, 30).coerceAtMost(200)
                    val maxColumn = maxOf(state.maxColumn + 1, 12).coerceAtMost(26)
                    val selectedCell = state.selectionState.ref

                    OutlinedTextField(
                        value = state.formulaBarText,
                        onValueChange = { updated ->
                            if (selectedCell != null) controller.onFormulaBarChange(updated)
                        },
                        label = { Text("Formula / Value") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                controller.onCommitEdit()
                                focusManager.clearFocus()
                            }
                        )
                    )

                    if (state.isRecalculating) {
                        Text("Recalculating...", color = Color(0xFF6B7280))
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val viewportWidthPx = with(density) { maxWidth.toPx() - rowHeaderWidthPx }
                        // FIX #3: Key only on selectedCell so scroll doesn't fire on every
                        // recalculation that bumps maxRow/maxColumn.
                        // Keep header in the list so item indexes remain stable.
                        LaunchedEffect(selectedCell) {
                            val ref = selectedCell ?: return@LaunchedEffect
                            listState.animateScrollToItem(ref.row.coerceAtLeast(0))
                            val cellLeft = ref.column * cellWidthPx
                            val cellRight = cellLeft + cellWidthPx
                            val current = horizontalScroll.value.toFloat()
                            val target = when {
                                cellLeft < current -> cellLeft
                                cellRight > current + viewportWidthPx -> cellRight - viewportWidthPx
                                else -> current
                            }
                            horizontalScroll.animateScrollTo(target.toInt().coerceAtLeast(0))
                            focusRequester.requestFocus()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                                .focusable()
                                .onPreviewKeyEvent { event ->
                                    val nativeEvent = event.nativeKeyEvent
                                    if (nativeEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                                    val ref = state.selectionState.ref
                                    val shift = nativeEvent.isShiftPressed
                                    val editing = state.editorState.isEditing
                                    when (nativeEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            if (editing) controller.onCommitEditAndMove(-1, 0) else controller.onMoveSelection(-1, 0)
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (editing) controller.onCommitEditAndMove(1, 0) else controller.onMoveSelection(1, 0)
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (editing) controller.onCommitEditAndMove(0, -1) else controller.onMoveSelection(0, -1)
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (editing) controller.onCommitEditAndMove(0, 1) else controller.onMoveSelection(0, 1)
                                            true
                                        }
                                        KeyEvent.KEYCODE_TAB -> {
                                            if (editing) {
                                                controller.onCommitEditAndMove(0, if (shift) -1 else 1)
                                            } else {
                                                controller.onMoveSelection(0, if (shift) -1 else 1)
                                            }
                                            true
                                        }
                                        KeyEvent.KEYCODE_ENTER -> {
                                            if (editing) {
                                                controller.onCommitEditAndMove(if (shift) -1 else 1, 0)
                                            } else if (ref != null) {
                                                controller.onBeginEdit(ref)
                                            }
                                            true
                                        }
                                        KeyEvent.KEYCODE_ESCAPE -> {
                                            if (editing) {
                                                controller.onCancelEdit()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        else -> {
                                            // FIX #12: Guard against modifier keys that can produce
                                            // non-zero unicodeChar values (e.g., AltGr combos).
                                            if (!editing && ref != null) {
                                                val isModifier = nativeEvent.isShiftPressed ||
                                                    nativeEvent.isCtrlPressed ||
                                                    nativeEvent.isAltPressed ||
                                                    nativeEvent.isMetaPressed
                                                if (!isModifier) {
                                                    val unicode = nativeEvent.unicodeChar
                                                    if (unicode > 0 && !Character.isISOControl(unicode)) {
                                                        controller.onBeginEditWithText(unicode.toChar().toString())
                                                        return@onPreviewKeyEvent true
                                                    }
                                                }
                                            }
                                            false
                                        }
                                    }
                                },
                            state = listState,
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(1) {
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .width(46.dp)
                                            .height(36.dp)
                                            .border(1.dp, Color(0xFFD4DAE6))
                                            .background(Color(0xFFF8FAFC))
                                    )
                                    Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                                        repeat(maxColumn) { col ->
                                            val highlight = state.selectionState.column == col
                                            Box(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(36.dp)
                                                    .border(1.dp, Color(0xFFD4DAE6))
                                                    .background(if (highlight) NexoraPrimary.copy(alpha = 0.18f) else Color(0xFFEFF6FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = columnName(col),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(0xFF1D4ED8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            items(maxRow) { rowIndex ->
                                // FIX #4: Row-level read of selectionState — only rows whose
                                // highlight status changes will recompose their cells.
                                val rowSelected = state.selectionState.row == rowIndex
                                Row {
                                    // Sticky row-number header (horizontally fixed).
                                    Box(
                                        modifier = Modifier
                                            .width(46.dp)
                                            .height(36.dp)
                                            .border(1.dp, Color(0xFFD4DAE6))
                                            .background(
                                                if (rowSelected) NexoraPrimary.copy(alpha = 0.14f)
                                                else Color(0xFFF8FAFC)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${rowIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rowSelected) NexoraPrimary else Color(0xFF6B7280)
                                        )
                                    }
                                    Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                                        repeat(maxColumn) { colIndex ->
                                            val ref = CellRef(row = rowIndex, column = colIndex)
                                            val cell = state.cells[ref]
                                            val isSelected = selectedCell == ref
                                            val isEditing = state.editorState.isEditing &&
                                                state.editorState.ref == ref
                                            val colSelected = state.selectionState.column == colIndex
                                            val highlight = rowSelected || colSelected
                                            // FIX #4: Direct conditional styling — no animate*AsState
                                            // inside items lambda. At 200x26 cells the old code tracked
                                            // 10,400 simultaneous animation states, causing frame drops.
                                            val borderWidth = if (isSelected) 2.dp else 1.dp
                                            val borderColor = if (isSelected) NexoraPrimary else Color(0xFFD4DAE6)
                                            val bgColor = when {
                                                isSelected -> NexoraPrimary.copy(alpha = 0.10f)
                                                highlight  -> Color(0xFFF0F4FF)
                                                else       -> Color.White
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(36.dp)
                                                    .border(borderWidth, borderColor)
                                                    .background(bgColor)
                                                    .combinedClickable(
                                                        onClick = {
                                                            controller.onCellSelected(ref)
                                                            focusRequester.requestFocus()
                                                        },
                                                        onDoubleClick = {
                                                            controller.onCellSelected(ref)
                                                            controller.onCellTap(ref)
                                                            focusRequester.requestFocus()
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (isEditing) {
                                                    CellEditorOverlay(
                                                        value = state.editorState.text,
                                                        onValueChange = controller.onEditorTextChange,
                                                        onCommit = {
                                                            controller.onCommitEdit()
                                                            focusManager.clearFocus()
                                                        }
                                                    )
                                                } else {
                                                    Text(
                                                        text = cell?.display.orEmpty(),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF111827),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// FIX #5: Regular class, not data class. Lambda fields are never structurally equal
// across recompositions, so a data class here makes @Stable ineffective and causes
// SpreadsheetEditor to fully recompose every time EditorScreen emits new state.
// The caller should wrap construction in remember { } with a stable ViewModel reference.
@Stable
class SpreadsheetInteractionController(
    val onCellSelected: (CellRef) -> Unit,
    val onCellTap: (CellRef) -> Unit,
    val onBeginEdit: (CellRef) -> Unit,
    val onBeginEditWithText: (String) -> Unit,
    val onEditorTextChange: (String) -> Unit,
    val onCommitEdit: () -> Unit,
    val onCommitEditAndMove: (Int, Int) -> Unit,
    val onCancelEdit: () -> Unit,
    val onMoveSelection: (Int, Int) -> Unit,
    val onFormulaBarChange: (String) -> Unit,
    val onSheetSelected: (Int) -> Unit
)

@Composable
private fun CellEditorOverlay(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    // Cursor at end when the overlay first appears (covers the "overwrite" char scenario).
    var fieldValue by remember(value) {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = value,
                selection = androidx.compose.ui.text.TextRange(value.length)
            )
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    // FIX #11: BasicTextField instead of OutlinedTextField removes Material decoration
    // padding that caused the row height to visually expand/contract during edit transitions.
    androidx.compose.foundation.text.BasicTextField(
        value = fieldValue,
        onValueChange = { newVal ->
            fieldValue = newVal
            onValueChange(newVal.text)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF111827)),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(NexoraPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { onCommit() }
        )
    )

}

@Composable
private fun PresentationEditor(
    activeTab: EditorTab?,
    state: PptxUiState,
    onSlideSelected: (Int) -> Unit,
    onTextUpdated: (Int, Int, String) -> Unit
) {
    NexoraCard(contentPadding = 0.dp, modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7FA))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isLoading -> {
                    Text("Loading presentation...", color = Color(0xFF6B7280))
                }
                state.error != null -> {
                    Text(state.error, color = NexoraError)
                }
                state.document == null -> {
                    Text("Open a PPTX file to start editing.", color = Color(0xFF6B7280))
                }
                else -> {
                    val document = state.document
                    val slides = document.slides
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slides.forEachIndexed { index, _ ->
                            val selected = index == state.activeSlideIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) NexoraPrimary.copy(alpha = 0.2f) else Color.White)
                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                                    .clickable { onSlideSelected(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${index + 1}", color = Color(0xFF111827))
                            }
                        }
                    }

                    val slide = slides.getOrNull(state.activeSlideIndex)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = activeTab?.title ?: "Presentation",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF111827)
                        )
                        if (slide == null) {
                            Text("No slides available.", color = Color(0xFF6B7280))
                        } else {
                            slide.elements.forEach { element ->
                                when (element) {
                                    is PptxTextElement -> {
                                        OutlinedTextField(
                                            value = element.text,
                                            onValueChange = { updated ->
                                                onTextUpdated(slide.index, element.id, updated)
                                            },
                                            label = { Text("Text box") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    is PptxImageElement -> {
                                        NexoraCard(contentPadding = 10.dp) {
                                            Text(
                                                text = "Image: ${element.description}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF6B7280)
                                            )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfReader(activeTab: EditorTab?) {
    val uri = activeTab?.sourcePath
        ?.takeIf { it.startsWith("content://") }
        ?.let(Uri::parse)

    if (uri == null) {
        NexoraCard(contentPadding = 14.dp, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NexoraLogoMark(size = 38.dp)
                Text(
                    text = "Open a PDF file to preview pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
        return
    }

    PdfViewer(uri = uri)
}

@Composable
private fun TextEditor(
    activeTab: EditorTab?,
    state: TextUiState,
    onContentChanged: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.isLoading -> Text("Loading text file...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.error != null -> Text(state.error, color = NexoraError)
                else -> {
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = onContentChanged,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp)),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfViewer(uri: Uri) {
    val context = LocalContext.current
    val session = remember(uri) { HybridPdfRenderSession(context, context.contentResolver, uri) }
    var pageCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        errorMessage = null
        runCatching {
            session.open()
            pageCount = session.pageCount()
        }.onFailure { error ->
            errorMessage = error.message ?: "Failed to open PDF"
            pageCount = 0
        }
        isLoading = false
    }

    DisposableEffect(uri) {
        onDispose { session.close() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Loading PDF pages...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = errorMessage ?: "Failed to load PDF",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoraError
                )
            }
        } else if (pageCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "PDF contains no pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(List(pageCount) { it }) { index, page ->
                    PdfPage(session = session, index = page, pageNumber = index + 1)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(session: PdfPageRenderer, index: Int, pageNumber: Int) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.toPx().toInt().coerceAtLeast(1) }
        val bitmapState by produceState<android.graphics.Bitmap?>(
            initialValue = null,
            key1 = index,
            key2 = targetWidthPx
        ) {
            value = session.renderPage(index, targetWidthPx)
        }

        if (bitmapState == null) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Rendering page $pageNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        } else {
            Image(
                bitmap = bitmapState!!.asImageBitmap(),
                contentDescription = "PDF page $pageNumber",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    activeMode: EditorMode,
    selectedTool: String,
    onToolSelected: (String) -> Unit
) {
    val tools = when (activeMode) {
        EditorMode.Document -> listOf("B", "I", "U", "Align", "Insert", "Review")
        EditorMode.Spreadsheet -> listOf("fx", "Format", "Sort", "Chart", "Data", "Review")
        EditorMode.Presentation -> listOf("Text", "Image", "Shape", "Table", "Design", "Present")
        EditorMode.Pdf -> listOf("Annotate", "Highlight", "Draw", "Text", "Share", "More")
        EditorMode.Text -> listOf("Find", "Replace", "Indent", "Wrap", "Count", "More")
    }
    NexoraCard(contentPadding = 6.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            tools.forEach { label ->
                NexoraToolbarButton(
                    label = label,
                    selected = selectedTool == label,
                    onClick = { onToolSelected(label) }
                )
            }
        }
    }
}

@Composable
private fun FloatingEditorToolbar(
    activeMode: EditorMode,
    selectedTool: String,
    onToolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        EditorToolbar(
            activeMode = activeMode,
            selectedTool = selectedTool,
            onToolSelected = onToolSelected
        )
    }
}

private enum class ToolbarPosition(val label: String) {
    Top("Top toolbar"),
    Bottom("Bottom toolbar"),
    Floating("Floating")
}

@Composable
private fun CompactToolRow(
    tools: List<String>,
    selectedTool: String,
    onToolSelected: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        tools.forEach { label ->
            NexoraToolbarButton(
                label = label,
                selected = selectedTool == label,
                onClick = { onToolSelected(label) }
            )
        }
    }
}

private data class DocxUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val document: DocxDocument? = null,
    val editText: String = "",
    val isEditing: Boolean = false
)

private data class PptxUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val document: PptxDocument? = null,
    val activeSlideIndex: Int = 0
)

private data class TextUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val content: String = ""
)

private fun PptxUiState.updateText(slideIndex: Int, elementId: Int, updated: String): PptxUiState {
    val document = document ?: return this
    val updatedSlides = document.slides.mapIndexed { index, slide ->
        if (index != slideIndex) return@mapIndexed slide
        val updatedElements = slide.elements.map { element ->
            if (element is PptxTextElement && element.id == elementId) {
                element.copy(text = updated)
            } else {
                element
            }
        }
        slide.copy(elements = updatedElements)
    }
    return copy(document = document.copy(slides = updatedSlides))
}

private fun columnName(index: Int): String {
    var value = index
    val result = StringBuilder()
    do {
        result.append(('A'.code + (value % 26)).toChar())
        value = value / 26 - 1
    } while (value >= 0)
    return result.reverse().toString()
}

private enum class EditorMode(
    val label: String,
    val fileName: String
) {
    Document("Document", "Untitled Document"),
    Spreadsheet("Sheet", "Untitled Spreadsheet"),
    Presentation("Slides", "Untitled Presentation"),
    Pdf("PDF", "Untitled PDF"),
    Text("Text", "Untitled Text")
}

private fun WorkspaceFile.toEditorTab(): EditorTab = EditorTab(
    fileId = id,
    title = name,
    dirty = false,
    type = type,
    sourcePath = path
)

private fun copyDocument(
    contentResolver: android.content.ContentResolver,
    sourceUri: Uri,
    targetUri: Uri
) {
    contentResolver.openInputStream(sourceUri)?.use { input ->
        contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
            input.copyTo(output)
        }
    }
}

private fun DocumentType?.toEditorMode(): EditorMode = when (this) {
    DocumentType.SHEET -> EditorMode.Spreadsheet
    DocumentType.SLIDE -> EditorMode.Presentation
    DocumentType.PDF -> EditorMode.Pdf
    DocumentType.TEXT -> EditorMode.Text
    DocumentType.DOC, null -> EditorMode.Document
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
