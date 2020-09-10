import dita.dev.data.RemoteConfigRepo
import dita.dev.main
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import okhttp3.Credentials
import org.junit.Test
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.koin.test.mock.declare
import kotlin.test.assertEquals

class HooksTest : AutoCloseKoinTest() {

    private val remoteConfigRepo: RemoteConfigRepo by inject()


    @Test
    fun testUpdateCalendar() = withTestApplication(Application::main) {
        declare {
            mockk<RemoteConfigRepo>()
        }
        coEvery { remoteConfigRepo.updateCurrentCalendar() } just Runs
        with(handleRequest(HttpMethod.Post, "/update_calendar") {
            addHeader("Authorization", Credentials.basic("test", "test"))
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("OK", response.content)
            coVerify { remoteConfigRepo.updateCurrentCalendar() }
        }
    }

    @Test
    fun testCheckExamPeriod() = withTestApplication(Application::main) {
        declare {
            mockk<RemoteConfigRepo>()
        }
        coEvery { remoteConfigRepo.checkExamPeriod() } just Runs
        with(handleRequest(HttpMethod.Post, "/check_exam_period") {
            addHeader("Authorization", Credentials.basic("test", "test"))
        }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("OK", response.content)
            coVerify { remoteConfigRepo.checkExamPeriod() }
        }
    }

}