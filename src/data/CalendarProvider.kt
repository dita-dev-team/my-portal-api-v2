package dita.dev.data

import com.jaunt.Document
import com.jaunt.HttpRequest
import com.jaunt.NotFound
import com.jaunt.UserAgent
import com.jaunt.util.MultiMap
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

interface CalendarProvider {

    fun getCalendar(): Calendar
}

class PortalCalendarProvider(private val ua: UserAgent) : CalendarProvider {

    private val baseUrl = "http://portal.daystar.ac.ke/Common/CourseSchedule.aspx"
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36"

    override fun getCalendar(): Calendar {
        val request = HttpRequest.makeGET(baseUrl, MultiMap(), mutableMapOf("User-Agent" to userAgent))
        val response = ua.send(request)
        val select = response.findFirst("<select id=_ctl0_PlaceHolderMain__ctl0_cbTerm>")
        val option = select.findFirst("<option selected=selected>")
        val text = option.textContent.trim()
        val period = getSemesterPeriod(response)
        val name: String? = if (period != null) {
            getNameFromPeriod(period)
        } else {
            getName(text)
        }
        return Calendar(name, period)
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
        val form = doc.getForm("<form name=aspnetForm>")
        form.set("__EVENTTARGET", "_ctl0\$PlaceHolderMain\$_ctl0\$btnSearch")
        val response = ua.send(form.request)
        return try {
            val element = response.findFirst("<span id=.*DateRange_CourseList$>")
            val dateRangeString = element.textContent.trim()
            val reg = "(\\d{1,2}/\\d{1,2}/\\d{4})".toRegex()
            val matches = reg.findAll(dateRangeString)
            val start = matches.first()
            val end = matches.last()
            val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
            val startDate = LocalDate.from(formatter.parse(start.value))
            val endDate = LocalDate.from(formatter.parse(end.value))
            DateRange(startDate.toNormalDate(), endDate.toNormalDate())
        } catch (e: NotFound) {
            null
        }
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