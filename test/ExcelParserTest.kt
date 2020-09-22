import dita.dev.utils.ExcelParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExcelParserTest {

    @Test
    fun `test stringToDate `() {
        val excelParser = ExcelParser()
        var dateTime = excelParser.stringToDate("ksdfdsf")
        assertNull(dateTime)

        dateTime = excelParser.stringToDate("1/10/19 1:30pm")
        assertThat(dateTime, instanceOf(Date::class.java))

        dateTime = excelParser.stringToDate("1/10/2019 1:30pm")
        assertThat(dateTime, instanceOf(Date::class.java))

        dateTime = excelParser.stringToDate("1/10/19 1.30pm")
        assertThat(dateTime, instanceOf(Date::class.java))

        dateTime = excelParser.stringToDate("1/10/2019 1.30pm")
        assertThat(dateTime, instanceOf(Date::class.java))
    }

    @Test
    fun `test sanitize`() {
        val excelParser = ExcelParser()
        var result = excelParser.sanitize("ACS101A")
        assertThat(result, `is`(listOf("ACS101A")))

        result = excelParser.sanitize("ACS101A/B")
        assertThat(result, `is`(listOf("ACS101A", "ACS101B")))

        result = excelParser.sanitize("ACS101A/ACS101B")
        assertThat(result, `is`(listOf("ACS101A", "ACS101B")))

        excelParser.shift = "athi"
        result = excelParser.sanitize("ACS101A/ACS201A/ACS102")
        assertThat(result, `is`(listOf("ACS101A", "ACS201A", "ACS102A")))

        excelParser.shift = ""
        result = excelParser.sanitize("ACS101/ACS101B")
        assertThat(result, `is`(listOf("ACS101B", "ACS101B")))

        excelParser.shift = "athi"
        result = excelParser.sanitize("ACS111/219/319/419")
        assertThat(result, `is`(listOf("ACS111A", "ACS219A", "ACS319A", "ACS419A")))

        excelParser.shift = ""
        result = excelParser.sanitize("ACS111/219/319/419A")
        assertThat(result, `is`(listOf("ACS111A", "ACS219A", "ACS319A", "ACS419A")))

        result = excelParser.sanitize("ACS261/MIS224B")
        assertThat(result, `is`(listOf("ACS261B", "MIS224B")))
    }

    @Test
    fun `test formatCourseTitle`() {
        val excelParser = ExcelParser()
        var result = excelParser.formatCourseTitle("ACS-113")
        assertEquals(result, "ACS-113")

        result = excelParser.formatCourseTitle("ACS113")
        assertEquals(result, "ACS-113")

        result = excelParser.formatCourseTitle("DICT114")
        assertEquals(result, "DICT-114")

    }

    @Test
    fun `test getShift`() {
        val excelParser = ExcelParser()
        var result = excelParser.getShift("Athi-River Day")
        assertEquals(result, "athi")

        result = excelParser.getShift("Nairobi Evening")
        assertEquals(result, "evening")

        result = excelParser.getShift("Nairobi Day")
        assertEquals(result, "day")
    }

    @Test
    fun `test split`() {
        val excelParser = ExcelParser()
        var result = excelParser.split("ACS-113")
        assertThat(result, `is`(listOf("ACS", "113")))

        result = excelParser.split("ACS 113")
        assertThat(result, `is`(listOf("ACS", "113")))

        result = excelParser.split("ACS113")
        assertThat(result, `is`(listOf("ACS", "113")))

        result = excelParser.split("DICT114")
        assertThat(result, `is`(listOf("DICT", "114")))
    }
}