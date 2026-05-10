package com.nexora.core.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class DocumentType {
    DOC,
    SHEET,
    SLIDE,
    PDF,
    TEXT
}

@Serializable
data class DocxDocument(
    val blocks: List<DocumentBlock> = emptyList()
)

@Serializable
sealed interface DocumentBlock

@Serializable
data class TextRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

@Serializable
enum class ParagraphAlignment {
    START,
    CENTER,
    END,
    JUSTIFY
}

@Serializable
enum class ListStyle {
    NONE,
    BULLET,
    NUMBERED
}

@Serializable
data class ParagraphBlock(
    val runs: List<TextRun> = emptyList(),
    val alignment: ParagraphAlignment = ParagraphAlignment.START,
    val listStyle: ListStyle = ListStyle.NONE
) : DocumentBlock

@Serializable
data class HeadingBlock(
    val level: Int,
    val runs: List<TextRun> = emptyList()
) : DocumentBlock

@Serializable
data class TableBlock(
    val rows: List<List<String>> = emptyList()
) : DocumentBlock

@Serializable
data class ImageBlock(
    val description: String = "Image",
    val widthPx: Int? = null,
    val heightPx: Int? = null
) : DocumentBlock

@Serializable
data class CellRef(
    val row: Int,
    val column: Int
)

@Serializable
data class CellData(
    val raw: String,
    val display: String
)

@Serializable
data class SpreadsheetSheet(
    val name: String,
    val cells: Map<CellRef, CellData> = emptyMap(),
    val maxRow: Int = 0,
    val maxColumn: Int = 0
)

@Serializable
data class SpreadsheetDocument(
    val sheets: List<SpreadsheetSheet> = emptyList(),
    val activeSheetIndex: Int = 0
)

@Serializable
sealed interface PptxElement

@Serializable
data class PptxTextElement(
    val id: Int,
    val text: String
) : PptxElement

@Serializable
data class PptxImageElement(
    val id: Int,
    val description: String
) : PptxElement

@Serializable
data class PptxSlide(
    val index: Int,
    val elements: List<PptxElement> = emptyList()
)

@Serializable
data class PptxDocument(
    val slides: List<PptxSlide> = emptyList(),
    val activeSlideIndex: Int = 0
)

@Serializable
data class WorkspaceFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val type: DocumentType,
    val lastModified: String = Instant.now().toString(),
    val sizeLabel: String = "Ready",
    val isPinned: Boolean = false
)

@Serializable
data class EditorTab(
    val fileId: String,
    val title: String,
    val dirty: Boolean,
    val type: DocumentType = DocumentType.DOC,
    val sourcePath: String = ""
)
