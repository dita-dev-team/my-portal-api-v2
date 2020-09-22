import dita.dev.utils.ExcelParser
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*
import kotlin.test.assertNull

class ExcelParserTest {

    @Test
    fun `null is returned for invalid format`() {
        val excelParser = ExcelParser()
        val dateTime = excelParser.stringToDate("ksdfdsf")
        assertNull(dateTime)
    }

    @Test
    fun `date object is returned`() {
        val excelParser = ExcelParser()
        var dateTime = excelParser.stringToDate("1/10/19 1:30pm")
        assertThat(dateTime, instanceOf(Date::class.java))

        dateTime = excelParser.stringToDate("1/10/2019 1:30pm")
        assertThat(dateTime, instanceOf(Date::class.java))

        dateTime = excelParser.stringToDate("1/10/19 1.30pm")
        assertThat(dateTime, instanceOf(Date::class.java))

        dateTime = excelParser.stringToDate("1/10/2019 1.30pm")
        assertThat(dateTime, instanceOf(Date::class.java))
    }
}