package com.nexora.core.common.file

import kotlin.math.roundToInt

object FileSizeFormatter {
    fun format(sizeBytes: Long?): String {
        if (sizeBytes == null || sizeBytes <= 0L) return "Ready"
        val kb = sizeBytes / 1024f
        val mb = kb / 1024f
        return if (mb >= 1f) {
            "${(mb * 10f).roundToInt() / 10f} MB"
        } else {
            "${kb.toInt().coerceAtLeast(1)} KB"
        }
    }
}
