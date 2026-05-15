package com.nexora.core.data.storage

import android.content.Context
import android.net.Uri
import com.nexora.core.common.file.FileSizeFormatter
import com.nexora.core.common.file.FileTypeDetector
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import java.time.Instant

class FileOpenManager(
    private val context: Context,
    private val metadataResolver: DocumentMetadataResolver
) {
    fun buildWorkspaceFile(uri: Uri): WorkspaceFile {
        val metadata = metadataResolver.resolve(uri)
        val name = metadata.name
        val type = FileTypeDetector.fromNameOrMime(name, metadata.mimeType)
        val sizeLabel = FileSizeFormatter.format(metadata.sizeBytes)
        val lastModified = metadata.lastModified?.let { Instant.ofEpochMilli(it).toString() } ?: Instant.now().toString()

        return WorkspaceFile(
            name = name,
            path = uri.toString(),
            type = type,
            lastModified = lastModified,
            sizeLabel = sizeLabel,
            isPinned = false
        )
    }

    fun allowedMimeTypes(): Array<String> = arrayOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/pdf",
        "text/plain",
        "text/markdown",
        "text/csv",
        "application/json",
        "application/xml"
    )

    fun typeLabel(type: DocumentType): String = when (type) {
        DocumentType.DOC -> "Doc"
        DocumentType.SHEET -> "Sheet"
        DocumentType.SLIDE -> "Slides"
        DocumentType.PDF -> "PDF"
        DocumentType.TEXT -> "Text"
    }
}
