package com.nexora.core.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.nexora.core.common.file.FileTypeDetector
import java.io.File

data class DocumentMetadata(
    val name: String,
    val sizeBytes: Long?,
    val lastModified: Long?,
    val mimeType: String?
)

class DocumentMetadataResolver(private val context: Context) {
    private val contentResolver = context.contentResolver

    fun resolve(uri: Uri): DocumentMetadata {
        if (uri.scheme == "file") {
            val file = File(uri.path.orEmpty())
            return DocumentMetadata(
                name = file.name.ifBlank { uri.lastPathSegment ?: "Document" },
                sizeBytes = file.takeIf { it.isFile }?.length(),
                lastModified = file.takeIf { it.exists() }?.lastModified(),
                mimeType = FileTypeDetector.inferMimeType(file.name)
            )
        }

        val name = queryString(uri, OpenableColumns.DISPLAY_NAME)
        val size = queryLong(uri, OpenableColumns.SIZE)
        val mimeType = contentResolver.getType(uri)
        val lastModified = DocumentFile.fromSingleUri(context, uri)?.lastModified()

        return DocumentMetadata(
            name = name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Document",
            sizeBytes = size,
            lastModified = lastModified,
            mimeType = mimeType
        )
    }

    private fun queryString(uri: Uri, column: String): String? =
        contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

    private fun queryLong(uri: Uri, column: String): Long? =
        contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
}
