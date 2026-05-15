package com.nexora.core.data.document

import android.graphics.Bitmap

interface PdfPageRenderer : AutoCloseable {
    suspend fun open(): PdfPageRenderer
    fun pageCount(): Int
    suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap?
}
