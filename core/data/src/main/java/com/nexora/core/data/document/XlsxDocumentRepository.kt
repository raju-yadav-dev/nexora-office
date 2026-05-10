package com.nexora.core.data.document

import android.content.ContentResolver
import android.net.Uri
import com.nexora.core.model.CellData
import com.nexora.core.model.CellRef
import com.nexora.core.model.SpreadsheetDocument
import com.nexora.core.model.SpreadsheetSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.Closeable

class XlsxDocumentRepository {
    suspend fun loadXlsx(contentResolver: ContentResolver, uri: Uri): SpreadsheetDocument =
        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { input ->
                val workbook = XSSFWorkbook(input)
                val formatter = DataFormatter()
                val evaluator = workbook.creationHelper.createFormulaEvaluator()
                val sheets = workbook.map { sheet ->
                    val cells = mutableMapOf<CellRef, CellData>()
                    var maxRow = 0
                    var maxColumn = 0
                    sheet.rowIterator().forEach { row ->
                        maxRow = maxOf(maxRow, row.rowNum)
                        row.cellIterator().forEach { cell ->
                            maxColumn = maxOf(maxColumn, cell.columnIndex)
                            val display = formatter.formatCellValue(cell, evaluator)
                            val raw = when (cell.cellType) {
                                CellType.FORMULA -> "=${cell.cellFormula}"
                                CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                                    display
                                } else {
                                    cell.numericCellValue.toString()
                                }
                                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                CellType.STRING -> cell.stringCellValue
                                CellType.BLANK -> ""
                                else -> display
                            }
                            if (raw.isNotBlank()) {
                                cells[CellRef(row = row.rowNum, column = cell.columnIndex)] =
                                    CellData(raw = raw, display = display)
                            }
                        }
                    }
                    SpreadsheetSheet(
                        name = sheet.sheetName,
                        cells = cells,
                        maxRow = maxRow,
                        maxColumn = maxColumn
                    )
                }
                SpreadsheetDocument(sheets = sheets)
            } ?: SpreadsheetDocument()
        }

    suspend fun loadSession(contentResolver: ContentResolver, uri: Uri): XlsxSession =
        withContext(Dispatchers.IO) {
            val workbook = contentResolver.openInputStream(uri)?.use { input ->
                XSSFWorkbook(input)
            } ?: XSSFWorkbook()
            XlsxSession(workbook)
        }

    suspend fun createEmptySession(): XlsxSession =
        withContext(Dispatchers.IO) {
            val workbook = XSSFWorkbook()
            workbook.createSheet("Sheet1")
            XlsxSession(workbook)
        }

    suspend fun saveSession(contentResolver: ContentResolver, uri: Uri, session: XlsxSession) {
        withContext(Dispatchers.IO) {
            val output = contentResolver.openOutputStream(uri, "wt") ?: return@withContext
            output.use { stream ->
                session.workbook.write(stream)
            }
        }
    }

    suspend fun saveXlsx(contentResolver: ContentResolver, uri: Uri, document: SpreadsheetDocument) {
        withContext(Dispatchers.IO) {
            val output = contentResolver.openOutputStream(uri, "wt") ?: return@withContext
            output.use { stream ->
                val workbook = XSSFWorkbook()
                document.sheets.forEach { sheetData ->
                    val sheet = workbook.createSheet(sheetData.name.ifBlank { "Sheet" })
                    sheetData.cells.forEach { (ref, data) ->
                        val row = sheet.getRow(ref.row) ?: sheet.createRow(ref.row)
                        val cell = row.getCell(ref.column) ?: row.createCell(ref.column)
                        val raw = data.raw
                        when {
                            raw.startsWith("=") -> cell.cellFormula = raw.removePrefix("=")
                            raw.equals("true", ignoreCase = true) || raw.equals("false", ignoreCase = true) ->
                                cell.setCellValue(raw.toBoolean())
                            raw.toDoubleOrNull() != null -> cell.setCellValue(raw.toDouble())
                            else -> cell.setCellValue(raw)
                        }
                    }
                }
                workbook.write(stream)
                workbook.close()
            }
        }
    }
}

class XlsxSession(internal val workbook: XSSFWorkbook) : Closeable {
    private val formatter = DataFormatter()
    private val evaluator: FormulaEvaluator = workbook.creationHelper.createFormulaEvaluator()
    private val sheetSizeCache = mutableMapOf<Int, Pair<Int, Int>>()

    fun sheetNames(): List<String> = workbook.sheetIterator().asSequence().map { it.sheetName }.toList()

    fun sheetCount(): Int = workbook.numberOfSheets

    fun sheetAt(index: Int) = workbook.getSheetAt(index)

    fun maxRowColumn(sheetIndex: Int): Pair<Int, Int> {
        sheetSizeCache[sheetIndex]?.let { return it }
        val sheet = sheetAt(sheetIndex)
        var maxRow = 0
        var maxColumn = 0
        sheet.rowIterator().forEach { row ->
            maxRow = maxOf(maxRow, row.rowNum)
            row.cellIterator().forEach { cell ->
                maxColumn = maxOf(maxColumn, cell.columnIndex)
            }
        }
        return (maxRow to maxColumn).also { sheetSizeCache[sheetIndex] = it }
    }

    fun getCellData(sheetIndex: Int, row: Int, column: Int): CellData? {
        val sheet = sheetAt(sheetIndex)
        val rowRef = sheet.getRow(row) ?: return null
        val cell = rowRef.getCell(column) ?: return null
        val display = formatter.formatCellValue(cell, evaluator)
        val raw = when (cell.cellType) {
            CellType.FORMULA -> "=${cell.cellFormula}"
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) display else cell.numericCellValue.toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.STRING -> cell.stringCellValue
            CellType.BLANK -> ""
            else -> display
        }
        return CellData(raw = raw, display = display)
    }

    fun setCellValue(sheetIndex: Int, row: Int, column: Int, raw: String) {
        val sheet = sheetAt(sheetIndex)
        val rowRef = sheet.getRow(row) ?: sheet.createRow(row)
        val cell = rowRef.getCell(column) ?: rowRef.createCell(column)
        if (raw.isBlank()) {
            cell.setBlank()
            return
        }
        when {
            raw.startsWith("=") -> cell.cellFormula = raw.removePrefix("=")
            raw.equals("true", ignoreCase = true) || raw.equals("false", ignoreCase = true) ->
                cell.setCellValue(raw.toBoolean())
            raw.toDoubleOrNull() != null -> cell.setCellValue(raw.toDouble())
            else -> cell.setCellValue(raw)
        }
        sheetSizeCache.remove(sheetIndex)
    }

    fun recalculateAll() {
        // FIX #9: Cross-sheet formulas can change the effective dimensions of any sheet.
        // Invalidate the entire size cache so maxRowColumn() re-scans after recalc.
        sheetSizeCache.clear()
        evaluator.clearAllCachedResultValues()
        evaluator.evaluateAll()
    }

    override fun close() {
        workbook.close()
    }
}
