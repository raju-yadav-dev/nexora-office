package com.nexora.core.data.document

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.nexora.core.common.logging.NexoraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MuPdfRenderSession(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : PdfPageRenderer {

    private val logTag = "MuPdfRenderSession"
    private var document: Document? = null
    private var tempFile: File? = null
    private val cache = LruCache<Int, Bitmap>(12)

    override suspend fun open(): PdfPageRenderer = withContext(Dispatchers.IO) {
        if (document == null) {
            NexoraLogger.d(logTag, "Opening MuPDF document: $uri")
            val input = contentResolver.openInputStream(uri) ?: error("Unable to open PDF stream")
            val data = input.use { it.readBytes() }
            val file = File.createTempFile("nexora_pdf_", ".pdf", context.cacheDir).apply {
                writeBytes(data)
            }
            tempFile = file
            document = Document.openDocument(file.absolutePath)
        }
        this@MuPdfRenderSession
    }

    override fun pageCount(): Int = document?.countPages() ?: 0

    override suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val cached = cache.get(index)
            if (cached != null) return@withContext cached
            val doc = document ?: return@withContext null
            if (index < 0 || index >= doc.countPages()) return@withContext null

            val page = doc.loadPage(index)
            try {
                val bounds = page.bounds
                val width = (bounds.x1 - bounds.x0).coerceAtLeast(1f)
                val scale = targetWidthPx / width
                val matrix = Matrix(scale, scale)
                val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, true)
                try {
                    val bitmap = Bitmap.createBitmap(
                        pixmap.width,
                        pixmap.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.setPixels(
                        pixmap.getPixels(),
                        0,
                        pixmap.width,
                        0,
                        0,
                        pixmap.width,
                        pixmap.height
                    )
                    cache.put(index, bitmap)
                    bitmap
                } finally {
                    pixmap.destroy()
                }
            } finally {
                page.destroy()
            }
        }

    override fun close() {
        runCatching { document?.destroy() }
        document = null
        cache.evictAll()
        tempFile?.delete()
        tempFile = null
        NexoraLogger.d(logTag, "Closed MuPDF document")
    }
}
