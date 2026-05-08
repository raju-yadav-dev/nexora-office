package com.nexora.feature.filemanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile

@Composable
fun FileManagerScreen(
    onOpenFile: (WorkspaceFile) -> Unit
) {
    val files = listOf(
        WorkspaceFile(name = "Roadmap.docx", path = "/docs/Roadmap.docx", type = DocumentType.DOC),
        WorkspaceFile(name = "Q2 Budget.xlsx", path = "/sheets/Q2 Budget.xlsx", type = DocumentType.SHEET),
        WorkspaceFile(name = "Pitch Deck.pptx", path = "/slides/Pitch Deck.pptx", type = DocumentType.SLIDE),
        WorkspaceFile(name = "Design Notes.pdf", path = "/pdf/Design Notes.pdf", type = DocumentType.PDF)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("File Manager", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Local and cloud files unified in one workspace.",
            style = MaterialTheme.typography.bodyMedium
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(files) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(file) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium)
                        Text(file.path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
