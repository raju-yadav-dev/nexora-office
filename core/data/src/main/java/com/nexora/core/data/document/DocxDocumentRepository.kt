package com.nexora.core.data.document

import android.content.ContentResolver
import android.net.Uri
import com.nexora.core.common.logging.NexoraLogger
import com.nexora.core.model.DocxDocument
import com.nexora.core.model.DocumentBlock
import com.nexora.core.model.HeadingBlock
import com.nexora.core.model.ImageBlock
import com.nexora.core.model.ListStyle
import com.nexora.core.model.ParagraphAlignment
import com.nexora.core.model.ParagraphBlock
import com.nexora.core.model.TableBlock
import com.nexora.core.model.TextRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.ParagraphAlignment as XwpfParagraphAlignment

class DocxDocumentRepository {
    private val logTag = "DocxDocumentRepository"

    suspend fun loadDocx(contentResolver: ContentResolver, uri: Uri): DocxDocument =
        withContext(Dispatchers.IO) {
            NexoraLogger.d(logTag, "Loading DOCX: $uri")
            contentResolver.openInputStream(uri)?.use { input ->
                val document = XWPFDocument(input)
                val blocks = mutableListOf<DocumentBlock>()
                document.bodyElements.forEach { bodyElement ->
                    when (bodyElement) {
                        is XWPFParagraph -> {
                            val headingLevel = bodyElement.headingLevel()
                            val runs = bodyElement.runs
                                .mapNotNull { run ->
                                    val text = run.textValue()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                    TextRun(
                                        text = text,
                                        bold = run.isBold,
                                        italic = run.isItalic,
                                        underline = run.underline != UnderlinePatterns.NONE
                                    )
                                }
                                .ifEmpty { listOf(TextRun(bodyElement.text.orEmpty())) }
                            val listStyle = if (bodyElement.numID != null) ListStyle.BULLET else ListStyle.NONE
                            if (headingLevel != null) {
                                blocks.add(HeadingBlock(level = headingLevel, runs = runs))
                            } else {
                                blocks.add(
                                    ParagraphBlock(
                                        runs = runs,
                                            alignment = bodyElement.alignment.toParagraphAlignment(),
                                        listStyle = listStyle
                                    )
                                )
                            }
                            bodyElement.runs.orEmpty().forEach { run ->
                                run.embeddedPictures.orEmpty().forEach { picture ->
                                    blocks.add(
                                        ImageBlock(
                                            description = picture.description ?: picture.pictureData?.fileName ?: "Image"
                                        )
                                    )
                                }
                            }
                        }
                        else -> {
                            val table = bodyElement as? org.apache.poi.xwpf.usermodel.XWPFTable
                            if (table != null) {
                                val rows = table.rows.map { row ->
                                    row.tableCells.map { cell -> cell.text.orEmpty().trim() }
                                }
                                blocks.add(TableBlock(rows = rows))
                            }
                        }
                    }
                }
                DocxDocument(blocks = blocks)
            } ?: DocxDocument()
        }

    suspend fun saveDocx(contentResolver: ContentResolver, uri: Uri, document: DocxDocument) {
        withContext(Dispatchers.IO) {
            NexoraLogger.d(logTag, "Saving DOCX: $uri")
            val outputStream = contentResolver.openOutputStream(uri, "wt") ?: return@withContext
            outputStream.use { output ->
                val docx = XWPFDocument()
                document.blocks.forEach { block ->
                    when (block) {
                        is HeadingBlock -> {
                            val paragraph = docx.createParagraph()
                            paragraph.style = "Heading${block.level.coerceIn(1, 6)}"
                            block.runs.forEach { run ->
                                paragraph.createRun().apply {
                                    setText(run.text)
                                    isBold = run.bold
                                    isItalic = run.italic
                                    underline = if (run.underline) UnderlinePatterns.SINGLE else UnderlinePatterns.NONE
                                }
                            }
                        }
                        is ParagraphBlock -> {
                            val paragraph = docx.createParagraph()
                            paragraph.alignment = block.alignment.toXwpfAlignment()
                            block.runs.forEach { run ->
                                paragraph.createRun().apply {
                                    setText(run.text)
                                    isBold = run.bold
                                    isItalic = run.italic
                                    underline = if (run.underline) UnderlinePatterns.SINGLE else UnderlinePatterns.NONE
                                }
                            }
                        }
                        is TableBlock -> {
                            val table = docx.createTable()
                            block.rows.forEachIndexed { rowIndex, row ->
                                val tableRow = if (rowIndex == 0) table.getRow(0) else table.createRow()
                                row.forEachIndexed { cellIndex, cellText ->
                                    val cell = if (cellIndex == 0) tableRow.getCell(0) else tableRow.addNewTableCell()
                                    cell.text = cellText
                                }
                            }
                        }
                        is ImageBlock -> {
                            val paragraph = docx.createParagraph()
                            paragraph.createRun().setText("[Image: ${block.description}]")
                        }
                        else -> Unit
                    }
                }
                docx.write(output)
            }
        }
    }
}

private fun XWPFParagraph.headingLevel(): Int? {
    val style = this.style ?: return null
    val heading = style.lowercase().removePrefix("heading").trim()
    return heading.toIntOrNull()?.takeIf { it in 1..6 }
}

private fun org.apache.poi.xwpf.usermodel.XWPFRun.textValue(): String? =
    getText(0)

private fun XwpfParagraphAlignment.toParagraphAlignment(): ParagraphAlignment = when (this) {
    XwpfParagraphAlignment.CENTER -> ParagraphAlignment.CENTER
    XwpfParagraphAlignment.RIGHT -> ParagraphAlignment.END
    XwpfParagraphAlignment.BOTH -> ParagraphAlignment.JUSTIFY
    else -> ParagraphAlignment.START
}

private fun ParagraphAlignment.toXwpfAlignment(): XwpfParagraphAlignment = when (this) {
    ParagraphAlignment.CENTER -> XwpfParagraphAlignment.CENTER
    ParagraphAlignment.END -> XwpfParagraphAlignment.RIGHT
    ParagraphAlignment.JUSTIFY -> XwpfParagraphAlignment.BOTH
    ParagraphAlignment.START -> XwpfParagraphAlignment.LEFT
}
