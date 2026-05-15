package com.nexora.core.data.document

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.nexora.core.common.logging.NexoraLogger

class HybridPdfRenderSession(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val uri: Uri
) : PdfPageRenderer {

    private val logTag = "HybridPdfRenderSession"
    private var renderer: PdfPageRenderer? = null

    override suspend fun open(): PdfPageRenderer {
        if (renderer != null) return this
        renderer = runCatching {
            val mupdf = MuPdfRenderSession(context, contentResolver, uri)
            mupdf.open()
            NexoraLogger.i(logTag, "Using MuPDF renderer")
            mupdf
        }.getOrElse { error ->
            NexoraLogger.w(logTag, "MuPDF init failed, falling back to PdfRenderer", error)
            val fallback = PdfRenderSession(contentResolver, uri)
            fallback.open()
            fallback
        }
        return this
    }

    override fun pageCount(): Int = renderer?.pageCount() ?: 0

    override suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap? =
        renderer?.renderPage(index, targetWidthPx)

    override fun close() {
        renderer?.close()
        renderer = null
    }
}
