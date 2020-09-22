package dita.dev.utils

import java.text.SimpleDateFormat
import java.util.*

class ExcelParser {
    var shift = ""
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
            "[A-Z]{3,4}[\\d]{3}(?:/[\\d]{3}[a-z]*)*".toRegex(RegexOption.IGNORE_CASE) // YYY111/222/333/444
        val doubleJoined = "[A-Z]{3,4}/[A-Z]{3,4}[\\d]{3}[a-z]{1}".toRegex(RegexOption.IGNORE_CASE) // AAA/BBB111

        coursesArray.add(courseCode)

        if (courseCode.contains("/")) {
            var courseCodes = emptyList<String>()
            when {
                courseCode.matches(similarDoubleClasses) -> {
                    courseCodes = courseCode.split("/").map { code ->
                        if (!code.last().isDigit()) code else addSection(code)
                    }
                }
                courseCode.matches(lackingClassPrefix) -> {
                    courseCodes = courseCode.split("/")
                    courseCodes = courseCodes.mapIndexed { index, s ->
                        if (index == 0) {
                            s + courseCode.last()
                        } else {
                            s
                        }
                    }
                }
                courseCode.matches(conjoinedClasses) -> {
                    val prefix = courseCode.substring(0, 6)
                    val sections = courseCode.substring(6).split("/")
                    courseCodes = sections.map {
                        prefix + it
                    }
                }
                courseCode.matches(doubleJoined) -> {
                    courseCodes = courseCode.split("/")
                    courseCodes = courseCodes.mapIndexed { index, s ->
                        if (index == 0) {
                            s + courseCodes[1].substring(3)
                        } else {
                            s
                        }
                    }
                }
                courseCode.matches(fourJoinedClasses) -> {
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
                    val chunks = chunkString(code, 7)
                    temp.addAll(chunks)
                } else {
                    temp.add(code)
                }
            }
            return temp
        } else {
            return coursesArray
        }
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