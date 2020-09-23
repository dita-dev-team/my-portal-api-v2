import dita.dev.data.Exam
import dita.dev.utils.ExcelParser
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun `test fixStartTime`() {
        val excelParser = ExcelParser()
        var result = excelParser.fixStartTime("8:45am", "11:45am")
        assertEquals("8:45am", result)

        result = excelParser.fixStartTime("8:45", "11:45am")
        assertEquals(result, "8:45am")

        result = excelParser.fixStartTime("11:45", "1:45pm")
        assertEquals(result, "11:45am")

        result = excelParser.fixStartTime("11:45", "1:45am")
        assertEquals(result, "11:45pm")
    }

    @Test
    fun `valid date is returned`() {
        val stream = File("test/files/excel-new.xlsx").inputStream()
        val workbook = XSSFWorkbook(stream)
        val sheetName = workbook.getSheetName(0)
        val sheet = workbook.getSheet(sheetName)
        val excelParser = ExcelParser()
        excelParser.j = 3
        val date = excelParser.getDate(sheet, 3)
        assertNotNull(date)
        assertTrue(date.matches("[\\d]+/[\\d]+/[\\d]+".toRegex()))
        stream.close()
    }

    @Test
    fun `valid datetime is returned`() {
        val stream = File("test/files/excel-new.xlsx").inputStream()
        val workbook = XSSFWorkbook(stream)
        val sheetName = workbook.getSheetName(0)
        val sheet = workbook.getSheet(sheetName)
        val excelParser = ExcelParser()
        excelParser.i = 7
        excelParser.j = 2
        val dateTime = excelParser.getDateTimeDetails(sheet)
        assertNotNull(dateTime)
        assertTrue(dateTime.matches("[\\d]+/[\\d]+/[\\d]+\\s[\\d]+(?:\\\\.|:)[\\d]+[apm]+".toRegex()))
        stream.close()
    }

    @Test
    fun `valid details are returned`() {
        val stream = File("test/files/excel-new.xlsx").inputStream()
        val workbook = XSSFWorkbook(stream)
        val sheetName = workbook.getSheetName(0)
        val sheet = workbook.getSheet(sheetName)
        val excelParser = ExcelParser()
        excelParser.i = 7
        excelParser.j = 2
        val details = excelParser.getDetails(sheetName, sheet)
        assertTrue(details.isNotEmpty())
        assertTrue(details.containsKey("shift"))
        assertTrue(details.containsKey("room"))
        assertTrue(details.containsKey("dateTime"))
        assertEquals("LR14", details["room"])
        stream.close()
    }

    @Test
    fun `August 2017 is parsed successfully`() {
        val stream = File("test/files/excel-new1.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "ACS-354A",
                "ICO-018T",
                "PSY-414T",
                "COM-264B",
                "PSY-211P",
                "DEV-111X",
                "HRM-611X",
                "MME-614X",
            )
        )
        stream.close()
    }

    @Test
    fun `January 2018 is parsed successfully`() {
        val stream = File("test/files/excel-new3.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "ACS-404A",
                "ACS-454A",
                "ACS-451A",
                "ENG-214T",
                "PGM-614X",
                "PEA-141T",
                "MCD-619X",
            )
        )
        stream.close()
    }

    @Test
    fun `June 2018 is parsed successfully`() {
        val stream = File("test/files/excel-new4.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "MAT-313A",
                "MUS-496A",
                "BMS-402A",
                "ICO-094T",
                "DICT-224",
                "GRA-613X",
                "SOC-314X",
                "DICT-211T",
            )
        )
        stream.close()
    }

    @Test
    fun `August 2018 is parsed successfully`() {
        val stream = File("test/files/excel-new5.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "ACS-401A",
                "PSY-211A",
                "MUS-115A",
                "CHD-642X",
                "DICT-105T",
                "MAK-317T",
                "ICO-056U",
                "MUS-113A",
                "COM-445B",
                "ACS-261A",
            )
        )
        stream.close()
    }

    @Test
    fun `January 2019 is parsed successfully`() {
        val stream = File("test/files/excel-new6.xlsx").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "SWK-317A",
                "MIS-313A",
                "MUS-419A",
                "PSY-408T",
                "MAK-423T",
                "ICO-049T",
                "COM-422T",
                "GRA-613Z",
                "MCD-606X",
                "MCD-612X",
                "COM-408X",
                "COM-408A",
            )
        )
        stream.close()
    }

    @Test
    fun `May 2019 is parsed successfully`() {
        val stream = File("test/files/excel-new7.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "ENH-227A",
                "MUS-419A",
                "MIS-414A",
                "COM-321T",
                "DICT-109T",
                "DBM-021T",
                "COM-099T",
                "DGE-109U",
                "GRW-611X",
                "CHD-644X",
                "MME-609X",
                "GRA-610Z",
            )
        )
        stream.close()
    }

    @Test
    fun `August 2019 is parsed successfully`() {
        val stream = File("test/files/excel-new8.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "COM-624Y",
                "MUS-319B",
                "ACS-332A",
                "ICC-09U",
                "DICT-107T",
                "PSY-211T",
                "PSY-408T",
                "DHR-100T",
                "GRW-611X",
                "GRA-614X",
                "MME-610X",
                "MAK-423X",
            )
        )
        stream.close()
    }

    @Test
    fun `January 2020 is parsed successfully`() {
        val stream = File("test/files/excel-new9.xlsx").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "MIS-113B",
                "PSY-408A",
                "MUS-318A",
                "IMU-075T",
                "ICO-013T",
                "DICT-226T",
                "PSY-211X",
                "COM-408X",
                "CHD-642X",
            )
        )
        stream.close()
    }

    @Test
    fun `June 2020 is parsed successfully`() {
        val stream = File("test/files/excel-new10.xls").inputStream()
        val excelParser = ExcelParser()
        excelParser.extractData(stream)
        assertTrue(excelParser.units.isNotEmpty())
        assertTrue(
            hasNames(
                excelParser.units,
                "EEE-221A",
                "MAK-426A",
                "DICT-105A",
                "EDU-514T",
                "DBM-021T",
                "PSY-657X",
                "COM-302X",
                "PSY-644X",
                "CHD-644X",
                "PSY-211X",
            )
        )
        stream.close()
    }

    private fun hasNames(list: List<Exam>, vararg names: String): Boolean {
        val size = names.size
        val set = names.toSet()
        var counter = 0
        list.forEach lit@{
            if (set.contains(it.name)) {
                counter++
            }
            if (counter == size) {
                return@lit
            }
        }
        return counter >= size
    }
}