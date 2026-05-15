package com.nexora.core.data.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.nexora.core.common.logging.NexoraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class PdfRenderSession(
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : PdfPageRenderer {
    private val logTag = "PdfRenderSession"
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val cache = LruCache<Int, Bitmap>(12)

    override suspend fun open(): PdfRenderSession = withContext(Dispatchers.IO) {
        if (renderer == null) {
            NexoraLogger.d(logTag, "Opening PDF renderer for $uri")
            fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            val descriptor = fileDescriptor ?: error("Unable to open PDF file")
            renderer = PdfRenderer(descriptor)
        }
        this@PdfRenderSession
    }

    override fun pageCount(): Int = renderer?.pageCount ?: 0

    override suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val cached = cache.get(index)
        if (cached != null) return@withContext cached
        val pdfRenderer = renderer ?: return@withContext null
        if (index < 0 || index >= pdfRenderer.pageCount) return@withContext null

        pdfRenderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width.toFloat()
            val width = (page.width * scale).roundToInt().coerceAtLeast(1)
            val height = (page.height * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val matrix = Matrix().apply { postScale(scale, scale) }
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            cache.put(index, bitmap)
            bitmap
        }
    }

    override fun close() {
        renderer?.close()
        fileDescriptor?.close()
        renderer = null
        fileDescriptor = null
        cache.evictAll()
        NexoraLogger.d(logTag, "Closed PDF renderer for $uri")
    }
}
