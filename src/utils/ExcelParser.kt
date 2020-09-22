package dita.dev.utils

import java.text.SimpleDateFormat
import java.util.*

class ExcelParser {
    private var shift = ""
    private var i = 0
    private var j = 0

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
            text.matches("[\\d]+/[\\d]+/[\\d]{2}[\\s]+[\\d]+:[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yy h:mma")
                dateTime = formatter.parse(text)
            }
            text.matches("[\\d]+/[\\d]+/[\\d]{4}[\\s]+[\\d]+:[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yyyy h:mma")
                dateTime = formatter.parse(text)
            }
            text.matches("[\\d]+/[\\d]+/[\\d]{2}[\\s]+[\\d]+\\.[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
                val formatter = SimpleDateFormat("dd/MM/yy h.mma")
                dateTime = formatter.parse(text)
            }
            text.matches("[\\d]+/[\\d]+/[\\d]{4}[\\s]+[\\d]+\\.[\\d]+[amp]+".toRegex(RegexOption.IGNORE_CASE)) -> {
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