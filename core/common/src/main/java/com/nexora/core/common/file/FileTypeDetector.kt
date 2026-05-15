package com.nexora.core.common.file

import android.webkit.MimeTypeMap
import com.nexora.core.model.DocumentType

object FileTypeDetector {
    fun fromNameOrMime(name: String?, mime: String?): DocumentType {
        val normalizedMime = mime?.lowercase()?.trim().orEmpty()
        val extension = name?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()?.trim().orEmpty()
        return when {
            normalizedMime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || extension == "docx" ->
                DocumentType.DOC
            normalizedMime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || extension == "xlsx" ->
                DocumentType.SHEET
            normalizedMime == "application/vnd.openxmlformats-officedocument.presentationml.presentation" || extension == "pptx" ->
                DocumentType.SLIDE
            normalizedMime == "application/pdf" || extension == "pdf" ->
                DocumentType.PDF
            normalizedMime.startsWith("text/") || extension in setOf("txt", "md", "csv", "json", "xml") ->
                DocumentType.TEXT
            else -> DocumentType.TEXT
        }
    }

    fun inferMimeType(name: String?): String? {
        val extension = name?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()?.trim().orEmpty()
        if (extension.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
