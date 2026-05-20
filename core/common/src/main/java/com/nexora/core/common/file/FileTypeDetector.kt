package com.nexora.core.common.file

import android.webkit.MimeTypeMap
import com.nexora.core.model.DocumentType

object FileTypeDetector {
    fun fromNameOrMime(name: String?, mime: String?): DocumentType {
        val normalizedMime = mime?.lowercase()?.trim().orEmpty()
        val extension = name?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()?.trim().orEmpty()
        return when {
            normalizedMime in setOf(
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ) || extension in setOf("doc", "docx") ->
                DocumentType.DOC
            normalizedMime in setOf(
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ) || extension in setOf("xls", "xlsx") ->
                DocumentType.SHEET
            normalizedMime in setOf(
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ) || extension in setOf("ppt", "pptx") ->
                DocumentType.SLIDE
            normalizedMime == "application/pdf" || extension == "pdf" ->
                DocumentType.PDF
            normalizedMime.startsWith("image/") || extension in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "heif") ->
                DocumentType.IMAGE
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
