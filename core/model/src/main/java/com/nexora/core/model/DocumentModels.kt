package com.nexora.core.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class DocumentType {
    DOC,
    SHEET,
    SLIDE,
    PDF
}

@Serializable
data class WorkspaceFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val type: DocumentType,
    val lastModified: String = Instant.now().toString(),
    val isPinned: Boolean = false
)

@Serializable
data class EditorTab(
    val fileId: String,
    val title: String,
    val dirty: Boolean
)
