import dita.dev.utils.ExcelParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*
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
    fun `test sanitize returns units`() {
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
}