import dita.dev.main
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthStatusTest {

    @Test
    fun testHealthCheck() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, "/health_check")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("OK", response.content)
        }
    }
}