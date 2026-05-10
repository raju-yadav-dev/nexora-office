package com.nexora.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.core.designsystem.component.NexoraCard
import com.nexora.core.designsystem.component.NexoraGradientBackground
import com.nexora.core.designsystem.component.NexoraIconBadge
import com.nexora.core.designsystem.component.NexoraLogoMark
import com.nexora.core.designsystem.component.NexoraPill
import com.nexora.core.designsystem.component.NexoraToolbarButton
import com.nexora.core.designsystem.theme.NexoraError
import com.nexora.core.designsystem.theme.NexoraPrimary
import com.nexora.core.designsystem.theme.NexoraPrimaryVariant
import com.nexora.core.designsystem.theme.NexoraSecondary
import com.nexora.core.model.EditorTab

@Composable
fun EditorScreen() {
    val engine = remember { EditorEngine() }
    val initialTabs = remember {
        listOf(
            EditorTab(fileId = "proposal", title = "Proposal.docx", dirty = false),
            EditorTab(fileId = "report", title = "Report.xlsx", dirty = true),
            EditorTab(fileId = "guide", title = "Guide.pdf", dirty = false)
        )
    }
    val state by engine.state.collectAsState()
    var activeMode by remember { mutableStateOf(EditorMode.Document) }

    LaunchedEffect(engine) {
        initialTabs.forEach { engine.openTab(it) }
    }

    NexoraGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorHeader(activeMode = activeMode)
            OpenTabStrip(tabs = state.tabs, activeTabId = state.activeTabId)
            ModeStrip(activeMode = activeMode, onModeSelected = { activeMode = it })

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeMode) {
                    EditorMode.Document -> DocumentEditor()
                    EditorMode.Spreadsheet -> SpreadsheetEditor()
                    EditorMode.Presentation -> PresentationEditor()
                    EditorMode.Pdf -> PdfReader()
                    EditorMode.Workspace -> WorkspacePanel()
                }
            }

            EditorToolbar(activeMode = activeMode)
        }
    }
}

@Composable
private fun EditorHeader(activeMode: EditorMode) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Done",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activeMode.fileName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Autosaved - cloud synced",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NexoraToolbarButton(label = "Save")
        Spacer(Modifier.width(8.dp))
        NexoraToolbarButton(label = "...")
    }
}

@Composable
private fun OpenTabStrip(
    tabs: List<EditorTab>,
    activeTabId: String?
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            NexoraPill(
                label = if (tab.dirty) "${tab.title} *" else tab.title,
                selected = tab.fileId == activeTabId
            )
        }
    }
}

@Composable
private fun ModeStrip(
    activeMode: EditorMode,
    onModeSelected: (EditorMode) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditorMode.entries.forEach { mode ->
            NexoraPill(
                label = mode.label,
                selected = mode == activeMode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun DocumentEditor() {
    var body by remember {
        mutableStateOf(
            "1. Executive Summary\n\nNexora Office is a modern office suite for Android that brings documents, spreadsheets, presentations and PDF workflows into one focused workspace.\n\n2. Our Vision\n\nTo help teams create, edit and manage work quickly with a premium mobile-first experience."
        )
    }

    NexoraCard(contentPadding = 0.dp, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8FA))
                .padding(18.dp)
        ) {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF111827)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = NexoraPrimary
                )
            )
        }
    }
}

@Composable
private fun SpreadsheetEditor() {
    val rows = listOf(
        listOf("Month", "Sales", "Profit"),
        listOf("Jan", "12000", "2400"),
        listOf("Feb", "15000", "3000"),
        listOf("Mar", "18000", "3600"),
        listOf("Apr", "16000", "3200")
    )

    NexoraCard(contentPadding = 0.dp, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(12.dp)
        ) {
            Text(
                text = "Sales Report",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF111827),
                modifier = Modifier.padding(8.dp)
            )
            rows.forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .border(1.dp, Color(0xFFD4DAE6))
                                .background(if (rowIndex == 0) Color(0xFF1D4ED8) else Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = cell,
                                color = if (rowIndex == 0) Color.White else Color(0xFF111827),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresentationEditor() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(66.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (index == 1) NexoraPrimary.copy(alpha = 0.34f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF8B2F1C), Color(0xFF2B1410), Color(0xFFC08457))
                    )
                )
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = "BUSINESS PLAN",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "2024",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.86f)
                )
                Text(
                    text = "Modern - Clean - Professional",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun PdfReader() {
    NexoraCard(contentPadding = 14.dp, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF7F7F8))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NexoraLogoMark(size = 38.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Nexora Office",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "User Guide",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF111827)
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Get started with Nexora Office and boost your productivity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF374151)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(132.dp, 74.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NexoraPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Workspace", color = NexoraPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun WorkspacePanel() {
    val files = listOf(
        "Project Proposal.docx" to Color(0xFF2563EB),
        "Sales Report.xlsx" to NexoraSecondary,
        "User Guide.pdf" to NexoraError,
        "Business Plan.pptx" to Color(0xFFF97316)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        NexoraCard(color = NexoraPrimary.copy(alpha = 0.16f)) {
            Text(
                text = "Multi Tab Workspace",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pin, switch and continue documents easily",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        files.forEach { (name, color) ->
            NexoraCard(contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NexoraIconBadge(label = name.substringAfterLast(".").take(3).uppercase(), color = color, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("12.4 MB - active workspace", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EditorToolbar(activeMode: EditorMode) {
    val tools = when (activeMode) {
        EditorMode.Document -> listOf("B", "I", "U", "Align", "More")
        EditorMode.Spreadsheet -> listOf("fx", "Cells", "Sort", "Chart", "More")
        EditorMode.Presentation -> listOf("Text", "Image", "Shape", "Table", "More")
        EditorMode.Pdf -> listOf("Annotate", "Highlight", "Draw", "Text", "More")
        EditorMode.Workspace -> listOf("Pin", "Recent", "Cloud", "Open", "More")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        tools.forEachIndexed { index, label ->
            NexoraToolbarButton(label = label, selected = index == 0)
        }
    }
}

private enum class EditorMode(
    val label: String,
    val fileName: String
) {
    Document("Document", "Project Proposal.docx"),
    Spreadsheet("Sheet", "Sales Report.xlsx"),
    Presentation("Slides", "Business Plan.pptx"),
    Pdf("PDF", "User Guide.pdf"),
    Workspace("Workspace", "Workspace")
}
