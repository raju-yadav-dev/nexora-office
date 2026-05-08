package com.nexora.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.core.model.EditorTab

@Composable
fun EditorScreen() {
    val engine = remember { EditorEngine() }
    val firstTab = remember { EditorTab(fileId = "default", title = "Untitled Doc", dirty = false) }
    LaunchedEffect(firstTab) {
        engine.openTab(firstTab)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Editor Workspace", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Rich Text") })
            AssistChip(onClick = {}, label = { Text("Spreadsheet") })
            AssistChip(onClick = {}, label = { Text("Presentation") })
            AssistChip(onClick = {}, label = { Text("AI Assist") })
        }

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Start writing...") }
        )
    }
}
