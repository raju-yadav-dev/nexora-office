package com.nexora.core.data.document

import android.content.ContentResolver
import android.net.Uri
import com.nexora.core.model.PptxDocument
import com.nexora.core.model.PptxImageElement
import com.nexora.core.model.PptxSlide
import com.nexora.core.model.PptxTextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFPictureShape
import org.apache.poi.xslf.usermodel.XSLFTextShape

class PptxDocumentRepository {
    suspend fun loadPptx(contentResolver: ContentResolver, uri: Uri): PptxDocument =
        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { input ->
                val show = XMLSlideShow(input)
                val slides = show.slides.mapIndexed { slideIndex, slide ->
                    var elementId = 0
                    val elements = slide.shapes.mapNotNull { shape ->
                        when (shape) {
                            is XSLFTextShape -> {
                                val text = shape.text ?: ""
                                PptxTextElement(id = elementId++, text = text)
                            }
                            is XSLFPictureShape -> {
                                val description = shape.pictureData?.fileName ?: "Image"
                                PptxImageElement(id = elementId++, description = description)
                            }
                            else -> null
                        }
                    }
                    PptxSlide(index = slideIndex, elements = elements)
                }
                PptxDocument(slides = slides)
            } ?: PptxDocument()
        }

    suspend fun savePptx(contentResolver: ContentResolver, uri: Uri, document: PptxDocument) {
        withContext(Dispatchers.IO) {
            val input = contentResolver.openInputStream(uri) ?: return@withContext
            val show = input.use { XMLSlideShow(it) }
            document.slides.forEachIndexed { slideIndex, slideData ->
                val slide = show.slides.getOrNull(slideIndex) ?: return@forEachIndexed
                val textElements = slideData.elements.filterIsInstance<PptxTextElement>()
                var textIndex = 0
                slide.shapes.forEach { shape ->
                    if (shape is XSLFTextShape && textIndex < textElements.size) {
                        val updated = textElements[textIndex].text
                        shape.clearText()
                        shape.setText(updated)
                        textIndex += 1
                    }
                }
            }
            val output = contentResolver.openOutputStream(uri, "wt") ?: return@withContext
            output.use { stream ->
                show.write(stream)
                show.close()
            }
        }
    }
}
