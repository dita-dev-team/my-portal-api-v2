package dita.dev.utils

import dita.dev.data.Exam
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ExcelParser {
    var shift = ""
    var i = 0
    var j = 0
    val units = mutableListOf<Exam>()

    fun extractData(`is`: InputStream) {
        val bis = BufferedInputStream(`is`)
        bis.mark(0)
        val workbook = try {
            XSSFWorkbook(bis)
        } catch (e: OLE2NotOfficeXmlFileException) {
            bis.reset()
            if (bis.markSupported()) {
                bis.reset()
            }
            HSSFWorkbook(bis)
        }
        for (s in 0 until workbook.numberOfSheets) {
            val sheetName = workbook.getSheetName(s)
            val worksheet = workbook.getSheet(sheetName)

            for (r in 0..worksheet.lastRowNum) {
                val row: Row = worksheet.getRow(r) ?: continue
                i = r
                for (c in 0..row.lastCellNum) {
                    val cell: Cell? = row.getCell(c)
                    if (cell == null || cell.cellType == CellType.BLANK) {
                        continue
                    }

                    val value = cell.stringCellValue.trim()

                    if (value.contains("semester", true)) {
                        continue
                    }

                    j = c

                    val pattern =
                        "(?:[a-zA-Z]{3,4}[\\d]{3}|[a-zA-Z]{3,4}[\\s]+[\\d]{3}|[a-zA-Z]{3,4}-[\\d]{3})".toRegex(
                            RegexOption.IGNORE_CASE
                        )
                    val badPattern = "[a-z]{3,4}\\s*[0-9]{3}[a-z]{2}&[a-z]+".toRegex(RegexOption.IGNORE_CASE)
                    if (value.contains(badPattern)) {
                        continue
                    }

                    if (value.contains(pattern) && j > 0) {
                        val details = getDetails(sheetName, worksheet)
                        val names = sanitize(value)

                        names.forEach {
                            val exam = Exam(
                                name = formatCourseTitle(it),
                                room = details["room"] as String,
                                date = (details["dateTime"] as? Date)?.time ?: 0,
                                shift = details["shift"] as String
                            )
                            units.add(exam)
                        }
                        j++
                    }
                }
            }
        }
    }

    fun getDetails(sheetName: String, sheet: Sheet): Map<String, Any?> {
        this.shift = getShift(sheetName)
        val dateTimeDetails = getDateTimeDetails(sheet)
        val dateTimeDetailsDate = stringToDate(dateTimeDetails ?: "")
        val row: Row? = sheet.getRow(i)
        val room = if (row == null) {
            "NO ROOM"
        } else {
            val cell: Cell? = row.getCell(0)
            if (cell == null || cell.cellType == CellType.BLANK) {
                "NO ROOM"
            } else {
                cell.stringCellValue
            }
        }
        return mapOf<String, Any?>(
            "dateTime" to dateTimeDetailsDate,
            "shift" to shift,
            "room" to room
        )
    }

    fun getDateTimeDetails(sheet: Sheet): String? {
        val rowNo = i
        val colNo = j
        val pattern = "(?:(.*)-([\\d]+.[\\d]+[apm]+))".toRegex(RegexOption.IGNORE_CASE)
        for (i in rowNo downTo 0) {
            val row: Row = sheet.getRow(i) ?: continue
            val cell: Cell? = row.getCell(colNo)
            if (cell == null || cell.cellType == CellType.BLANK) {
                continue
            }
            val value = cell.stringCellValue
            if (value.isEmpty()) {
                continue
            }

            val match = pattern.findAll(value).toList()
            if (match.isNotEmpty()) {
                val date = getDate(sheet, i + 1)
                if (date != null) {
                    var start = match[0].groupValues[1]
                    val end = match[0].groupValues[2]
                    start = fixStartTime(start, end)
                    return "$date ${start.toLowerCase()}"
                }
            }
        }

        return null
    }

    fun fixStartTime(start: String, end: String): String {
        if (start.contains("[a-z]{2}".toRegex(RegexOption.IGNORE_CASE))) {
            return start
        }

        val suffix = end.takeLast(2)
        val noPattern = "(^[0-9]+)".toRegex()

        val startMatch = noPattern.findAll(start).toList()
        val endMatch = noPattern.findAll(end).toList()
        val st = startMatch[0].value.toInt()
        val en = endMatch[0].value.toInt()
        return if (st < en) {
            "$start$suffix"
        } else {
            if (suffix.contains("[a]".toRegex(RegexOption.IGNORE_CASE))) {
                "${start}pm"
            } else {
                "${start}am"
            }
        }
    }

    fun getDate(sheet: Sheet, rowNo: Int): String? {
        val row = sheet.getRow(rowNo - 2)
        val col = j
        val pattern = "[\\w]+day[\\s]+([\\d]+/[\\d]+/[\\d]+)".toRegex(RegexOption.IGNORE_CASE)
        for (j in col downTo 0) {
            val cell: Cell? = row.getCell(j)
            if (cell == null || cell.cellType == CellType.BLANK) {
                continue
            }
            val cellValue = cell.stringCellValue.trim()

            val match = pattern.matchEntire(cellValue)

            if (match !== null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    fun split(text: String): List<String> {
        return when {
            text.contains("-") -> {
                text.split("-")
            }
            text.contains(" ") -> {
                text.split(" ")
            }
            else -> {
                val initLen = if (text.contains("^[a-z]{3}\\d".toRegex(RegexOption.IGNORE_CASE))) 3 else 4
                listOf(text.substring(0, initLen), text.substring(initLen))
            }
        }
    }

    fun getShift(text: String): String {
        return when {
            text.contains("athi", true) -> {
                "athi"
            }
            text.contains("evening", true) -> {
                "evening"
            }
            else -> {
                "day"
            }
        }
    }

    fun formatCourseTitle(text: String): String {
        return if (text.contains("-")) {
            text
        } else {
            val initLen = if (text.contains("^[a-z]{3}\\d".toRegex(RegexOption.IGNORE_CASE))) 3 else 4
            text.substring(0, initLen) + "-" + text.substring(initLen)
        }
    }

    fun sanitize(text: String): List<String> {
        val coursesArray = mutableListOf<String>()
        var courseCode = text.replace("\\s".toRegex(RegexOption.MULTILINE), "").replace("&".toRegex(), "/")
        // Correct type AAA111B(X)
        val match = "(.*)([A-Z]\\((.)\\))".toRegex().matchEntire(courseCode)
        match?.let {
            courseCode = it.groupValues[1] + it.groupValues[3]
        }

        val similarDoubleClasses =
            "[a-z]{3,4}\\d{3}[a-z]/[a-z]{3,4}\\d{3}[a-z]*.*".toRegex(RegexOption.IGNORE_CASE) // YYY111A/YYY222A
        val lackingClassPrefix =
            "[a-z]{3,4}[\\d]{3}/[a-z]{3,4}[\\d]{3}[a-z]{1}".toRegex(RegexOption.IGNORE_CASE) // YYY111/YYY222A
        val conjoinedClasses =
            "[a-z]{3,4}[\\d]{3}[a-z]{1}/[a-z]{1}(?:[/]*|.{1})".toRegex(RegexOption.IGNORE_CASE) // YYY111A/B
        val fourJoinedClasses =
            "[A-Z]{3,4}[\\d]{3}(?:/[\\d]{3})*|[A-Z]{3,4}[\\d]{3}(?:/[\\d]{3}[a-z]*)*".toRegex(RegexOption.IGNORE_CASE) // YYY111/222/333/444
        val doubleJoined = "[A-Z]{3,4}/[A-Z]{3,4}[\\d]{3}[a-z]{1}".toRegex(RegexOption.IGNORE_CASE) // AAA/BBB111

        coursesArray.add(courseCode)

        if (courseCode.contains("/")) {
            var courseCodes = emptyList<String>()
            when {
                similarDoubleClasses.findAll(courseCode).any() -> {
                    courseCodes = courseCode.split("/").map { code ->
                        if (!code.last().isDigit()) code else addSection(code)
                    }
                }
                lackingClassPrefix.findAll(courseCode).any() -> {
                    courseCodes = courseCode.split("/")
                    courseCodes = courseCodes.mapIndexed { index, s ->
                        if (index == 0) {
                            s + courseCode.last()
                        } else {
                            s
                        }
                    }
                }
                conjoinedClasses.findAll(courseCode).any() -> {
                    val prefix = courseCode.substring(0, 6)
                    val sections = courseCode.substring(6).split("/")
                    courseCodes = sections.map {
                        prefix + it
                    }
                }
                doubleJoined.findAll(courseCode).any() -> {
                    courseCodes = courseCode.split("/")
                    courseCodes = courseCodes.mapIndexed { index, s ->
                        if (index == 0) {
                            s + courseCodes[1].substring(3)
                        } else {
                            s
                        }
                    }
                }
                fourJoinedClasses.findAll(courseCode).any() -> {
                    val prefix = courseCode.substring(0, 3)
                    val codes = courseCode.substring(3).split("/")
                    val last = courseCode.last()
                    courseCodes = codes.map { code ->
                        val section: Char = if (!last.isDigit()) {
                            last.toUpperCase()
                        } else {
                            when (shift) {
                                "athi" -> 'A'
                                "day" -> 'T'
                                else -> 'X'
                            }
                        }
                        prefix + code.substring(0, 3) + section
                    }
                }
                else -> {
                    println("Unable to sanitize $text")
                }
            }
            val temp = mutableListOf<String>()
            courseCodes.forEach { code ->
                if (code.length > 7) {
                    val chunks = code.chunked(7)
                    temp.addAll(chunks)
                } else {
                    temp.add(code)
                }
            }
            return temp.filter { it.length > 6 }
        } else {
            return coursesArray.filter { it.length > 6 }
        }
    }

    fun addSection(code: String): String {
        val section = when (shift) {
            "athi" -> "A"
            "day" -> "T"
            else -> "X"
        }
        return code + section
    }

    fun stringToDate(text: String): Date? {
        var dateTime: Date? = null
        when {
            text.contains("[\\d]+/[\\d]+/[\\d]{2}[\\s]+[\\d]+:[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yy h:mma")
                dateTime = formatter.parse(text)
            }
            text.contains("[\\d]+/[\\d]+/[\\d]{4}[\\s]+[\\d]+:[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yyyy h:mma")
                dateTime = formatter.parse(text)
            }
            text.contains("[\\d]+/[\\d]+/[\\d]{2}[\\s]+[\\d]+\\.[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yy h.mma")
                dateTime = formatter.parse(text)
            }
            text.contains("[\\d]+/[\\d]+/[\\d]{4}[\\s]+[\\d]+\\.[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yyyy h.mma")
                dateTime = formatter.parse(text)
            }
        }
        return dateTime
    }

    fun chunkString(string: String, length: Int): List<String> {
        val pattern = ".{1, $length}".toRegex()
        return string.split(pattern)
    }
}