package dita.dev.data

import org.jsoup.Jsoup
import org.jsoup.helper.HttpConnection
import org.jsoup.nodes.Document
import org.jsoup.nodes.FormElement
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

interface CalendarProvider {

    fun getCalendar(): Calendar
}

class PortalCalendarProvider : CalendarProvider {

    private val baseUrl = "http://portal.daystar.ac.ke/Common/CourseSchedule.aspx"
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36"
    private var cookies: MutableMap<String, String>? = null

    override fun getCalendar(): Calendar {
        val request = Jsoup.connect(baseUrl)
            .followRedirects(true)
            .timeout(600 * 1000)
            .userAgent(userAgent)
        val response = request.execute()
        cookies = response.cookies()
        val document = response.parse()
        val select = document.selectFirst("select#_ctl0_PlaceHolderMain__ctl0_cbTerm")
        val options = select.select("option")
        var text = ""
        val optionTexts = options.map { option -> option.text().trim() }
        val termValues = options.map { option -> option.attr("value") }
        text = getLatestSemText(optionTexts)
        val period = getSemesterPeriod(document, termValues)
        var name: String? = null
        if (period != null) {
            name = getNameFromPeriod(period)
        }

        if (name == null) {
            name = getName(text)
        }
        return Calendar(name, period)
    }

    private fun getLatestSemText(optionTexts: List<String>): String {
        val formatter = SimpleDateFormat("yyyy MMMM")
        val dates = optionTexts.map { formatter.parse(it) }.sortedDescending()
        return formatter.format(dates.first())
    }

    private fun getName(text: String): String? {
        val year = Year.now().value
        return when {
            text.contains("january", true) -> {
                "jan${year}"
            }
            text.contains("june", true) -> {
                "jun${year}"
            }
            text.contains("august", true) -> {
                "aug${year}"
            }
            else -> null
        }

    }

    private fun getNameFromPeriod(period: DateRange): String? {
        val year = Year.now().value
        return when {
            period.isJanSemester() -> {
                "jan${year}"
            }
            period.isBlockSemester() -> {
                "jun${year}"
            }
            period.isAugSemester() -> {
                "aug${year}"
            }
            else -> null
        }

    }

    private fun getSemesterPeriod(doc: Document): DateRange? {
        return try {
            val form = doc.selectFirst("#aspnetForm") as FormElement
            val data = form.formData()
            data.removeAt(0)
            data.add(0, HttpConnection.KeyVal.create("__EVENTTARGET", "_ctl0\$PlaceHolderMain\$_ctl0\$btnSearch"))
            val request = Jsoup.connect(baseUrl)
                .followRedirects(true)
                .timeout(600 * 1000)
                .userAgent(userAgent)
                .cookies(cookies)
            val response = request.data(data).post()
            val element = response.selectFirst("span[id$=DateRange_CourseList]")
            val dateRangeString = element.text().trim()
            val reg = "(\\d{1,2}/\\d{1,2}/\\d{4})".toRegex()
            val matches = reg.findAll(dateRangeString)
            val start = matches.first()
            val end = matches.last()
            val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
            val startDate = LocalDate.from(formatter.parse(start.value))
            val endDate = LocalDate.from(formatter.parse(end.value))
            DateRange(startDate.toNormalDate(), endDate.toNormalDate())
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    private fun getSemesterPeriod(doc: Document, terms: List<String>): DateRange? {
        val today = LocalDate.now()
        var period: DateRange? = null
        for (term in terms) {
            try {
                val form = doc.selectFirst("#aspnetForm") as FormElement
                val data = form.formData()
                data.removeAt(0)
                data.add(0, HttpConnection.KeyVal.create("__EVENTTARGET", "_ctl0\$PlaceHolderMain\$_ctl0\$btnSearch"))
                data.add(0, HttpConnection.KeyVal.create("_ctl0:PlaceHolderMain:_ctl0:cbTerm", term))
                val request = Jsoup.connect(baseUrl)
                    .followRedirects(true)
                    .timeout(600 * 1000)
                    .userAgent(userAgent)
                    .cookies(cookies)
                val response = request.data(data).post()
                val element = response.selectFirst("span[id$=DateRange_CourseList]")
                val dateRangeString = element.text().trim()
                val reg = "(\\d{1,2}/\\d{1,2}/\\d{4})".toRegex()
                val matches = reg.findAll(dateRangeString)
                val start = matches.first()
                val end = matches.last()
                val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
                val startDate = LocalDate.from(formatter.parse(start.value))
                val endDate = LocalDate.from(formatter.parse(end.value))
                if (endDate.isBefore(today)) {
                    continue
                }
                period = DateRange(startDate.toNormalDate(), endDate.toNormalDate())
            } catch (e: Exception) {
                println(e)
            }
        }
        return period
    }

}

fun DateRange.isJanSemester(): Boolean {
    val currentYear = Year.now().value
    val date = LocalDate.of(currentYear, Month.MARCH, 1).toNormalDate()
    return !(date.before(start) || date.after(end))
}

fun DateRange.isBlockSemester(): Boolean {
    val currentYear = Year.now().value
    val date = LocalDate.of(currentYear, Month.JULY, 1).toNormalDate()
    return !(date.before(start) || date.after(end))
}

fun DateRange.isAugSemester(): Boolean {
    val date = LocalDate.now().toNormalDate()
    return !(date.before(start) || date.after(end))
}

fun LocalDate.toNormalDate(): Date {
    val zoneId = ZoneId.systemDefault()
    return Date.from(atStartOfDay(zoneId).toInstant())
}