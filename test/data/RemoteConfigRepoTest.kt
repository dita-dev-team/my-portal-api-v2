package data

import dita.dev.appModules
import dita.dev.data.CalendarProvider
import dita.dev.data.RemoteConfigRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.koin.test.mock.declare
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream


fun String.gzip(): ByteArray {
    val bos = ByteArrayOutputStream(length)
    val gzip = GZIPOutputStream(bos)
    gzip.write(this.toByteArray())
    gzip.close()
    val compressed = bos.toByteArray()
    bos.close()
    return compressed
}

@ExperimentalCoroutinesApi
class RemoteConfigRepoTest : AutoCloseKoinTest() {
    private val remoteConfigRepo: RemoteConfigRepo by inject()
    private val calendarProvider: CalendarProvider by inject()


    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yy")

    @Test
    fun `areExamsAvailable returns false`(): Unit = runBlocking {
        val today = LocalDate.now(ZoneId.of("Africa/Nairobi"))
        val startDate = today.minusDays(2)
        val endDate = today.minusDays(1)
        val response = """
            {
              "parameters": {
                "exam_period": {
                    "defaultValue": {
                        "value": "{\"start_date\":\"${formatter.format(startDate)}\",\"end_date\":\"${formatter.format(
            endDate
        )}\"}"
                    },
                    "description": "Start and Dates for exams"
                }
              }
            }
        """.trimIndent().gzip()
        startKoin {
            modules(appModules)
        }
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(Buffer().write(response)))
        server.start()
        declare(named("firebaseUrl")) {
            server.url("/").toString()
        }

        assertThat(remoteConfigRepo.areExamsOngoing(), `is`(false))
        server.shutdown()
    }

    @Test
    fun `areExamsAvailable returns true`(): Unit = runBlocking {
        val today = LocalDate.now(ZoneId.of("Africa/Nairobi"))
        val startDate = today.minusDays(2)
        val endDate = today.plusDays(2)
        val response = """
            {
              "parameters": {
                "exam_period": {
                    "defaultValue": {
                        "value": "{\"start_date\":\"${formatter.format(startDate)}\",\"end_date\":\"${formatter.format(
            endDate
        )}\"}"
                    },
                    "description": "Start and Dates for exams"
                }
              }
            }
        """.trimIndent().gzip()
        startKoin {
            modules(appModules)
        }
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(Buffer().write(response)))
        server.start()
        declare(named("firebaseUrl")) {
            server.url("/").toString()
        }

        assertThat(remoteConfigRepo.areExamsOngoing(), `is`(true))
        server.shutdown()
    }

    @Test
    fun `checkExamPeriod updates availability of exams`(): Unit = runBlocking {
        val today = LocalDate.now(ZoneId.of("Africa/Nairobi"))
        val startDate = today.minusDays(2)
        val endDate = today.plusDays(2)
        val response = """
            {
              "parameters": {
                "exam_period": {
                  "defaultValue": {
                    "value": "{\"start_date\":\"${formatter.format(startDate)}\",\"end_date\":\"${formatter.format(
            endDate
        )}\"}"
                  }
                },
                "exam_timetable_available": {
                  "defaultValue": {
                    "value": "false"
                  }
                }
              }
            }
        """.trimIndent().gzip()
        startKoin {
            modules(appModules)
        }
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(Buffer().write(response)).setHeader("etag", "etag"))
        server.enqueue(MockResponse().setBody(Buffer().write("{}".gzip())).setHeader("etag", "etag"))
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setBody(Buffer().write(response)))
        server.start()
        declare(named("firebaseUrl")) {
            server.url("/").toString()
        }

        remoteConfigRepo.checkExamPeriod()
        var request = server.takeRequest()
        assertThat(request.method, `is`("GET")) // First fetch
        request = server.takeRequest()
        assertThat(request.method, `is`("PUT")) // Validate remote config
        request = server.takeRequest()
        assertThat(request.method, `is`("PUT")) // Update remote config
        request = server.takeRequest()
        assertThat(request.method, `is`("GET")) //Fetch update remote config
        server.shutdown()
    }
}