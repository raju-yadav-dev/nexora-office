package com.nexora.core.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

data class DocumentMetadata(
    val name: String,
    val sizeBytes: Long?,
    val lastModified: Long?,
    val mimeType: String?
)

class DocumentMetadataResolver(private val context: Context) {
    private val contentResolver = context.contentResolver

    fun resolve(uri: Uri): DocumentMetadata {
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
