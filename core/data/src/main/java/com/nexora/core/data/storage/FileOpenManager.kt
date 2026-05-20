package com.nexora.core.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.nexora.core.common.file.FileSizeFormatter
import com.nexora.core.common.file.FileTypeDetector
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import java.time.Instant
import java.io.File

class FileOpenManager(
    private val context: Context,
    private val metadataResolver: DocumentMetadataResolver
) {
    fun buildWorkspaceFile(uri: Uri): WorkspaceFile {
        validateOpenableDocument(uri)
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
        "application/msword",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-powerpoint",
        "application/pdf",
        "image/*",
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
        DocumentType.IMAGE -> "Image"
        DocumentType.TEXT -> "Text"
    }

    private fun validateOpenableDocument(uri: Uri) {
        if (uri.scheme == "file") {
            val file = File(uri.path.orEmpty())
            when {
                file.isDirectory -> throw FolderSelectedForOpenException()
                !file.exists() -> throw DocumentUnavailableException()
                !file.canRead() -> throw DocumentUnreadableException()
                file.length() == 0L -> throw EmptyDocumentException()
            }
            return
        }

        if (DocumentsContract.isTreeUri(uri)) {
            throw FolderSelectedForOpenException()
        }

        val document = DocumentFile.fromSingleUri(context, uri)
        when {
            document?.isDirectory == true -> throw FolderSelectedForOpenException()
            document != null && !document.exists() -> throw DocumentUnavailableException()
            document != null && !document.canRead() -> throw DocumentUnreadableException()
        }

        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                if (descriptor.length == 0L) throw EmptyDocumentException()
            } ?: throw DocumentUnreadableException()
        }.getOrElse { error ->
            when (error) {
                is OpenDocumentException -> throw error
                else -> throw DocumentUnreadableException()
            }
        }
    }
}

sealed class OpenDocumentException(message: String) : IllegalArgumentException(message)

class FolderSelectedForOpenException : OpenDocumentException("Folders cannot be opened as documents.")

class DocumentUnavailableException : OpenDocumentException("This document is no longer available.")

class DocumentUnreadableException : OpenDocumentException("This document cannot be read.")

class EmptyDocumentException : OpenDocumentException("This document appears to be empty.")
