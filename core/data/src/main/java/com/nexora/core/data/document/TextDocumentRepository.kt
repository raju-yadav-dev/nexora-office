package com.nexora.core.data.document

import android.content.ContentResolver
import android.net.Uri
import com.nexora.core.common.logging.NexoraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class TextDocumentRepository {
    private val logTag = "TextDocumentRepository"

    suspend fun loadText(contentResolver: ContentResolver, uri: Uri): String =
        withContext(Dispatchers.IO) {
            NexoraLogger.d(logTag, "Loading text: $uri")
            contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        }

    suspend fun saveText(contentResolver: ContentResolver, uri: Uri, content: String) {
        withContext(Dispatchers.IO) {
            NexoraLogger.d(logTag, "Saving text: $uri")
            val output = contentResolver.openOutputStream(uri, "wt") ?: return@withContext
            output.use { stream ->
                OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            }
        }
    }
}
