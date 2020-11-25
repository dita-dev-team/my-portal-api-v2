import dita.dev.data.AuthRepo
import dita.dev.data.TokenValid
import dita.dev.data.User
import dita.dev.main
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.koin.test.mock.declare
import kotlin.test.assertEquals

class ViewsTest : AutoCloseKoinTest() {

    private val authRepo: AuthRepo by inject()

    @Test
    fun `unauthenticated user is redirected to the login page`() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, "/app")) {
            assertEquals(HttpStatusCode.Found, response.status())
            assertEquals("/app/login", response.headers["Location"])
        }
    }

    @Test
    fun `login flow`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        // User is redirected to login screen if id_token is missig in payload
        with(handleRequest(HttpMethod.Post, "/app/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        }) {
            assertEquals(io.ktor.http.HttpStatusCode.Found, response.status())
            assertEquals("/app/login", response.headers["Location"])

            verify(inverse = true) { authRepo.isTokenValid(any()) }
        }
        // User is redirected to login screen if id_token validation fails
        every { authRepo.isTokenValid(any()) } returns TokenValid.No

        with(handleRequest(HttpMethod.Post, "/app/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("id_token" to "test").formUrlEncode())
        }) {
            assertEquals(HttpStatusCode.Found, response.status())
            assertEquals("/app/login", response.headers["Location"])

            verify { authRepo.isTokenValid(any()) }
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)

        with(handleRequest(HttpMethod.Post, "/app/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("id_token" to "test").formUrlEncode())
        }) {
            assertEquals(HttpStatusCode.Found, response.status())
            assertEquals("/app", response.headers["Location"])
            verify { authRepo.isTokenValid(any()) }
        }
    }

    @Test
    fun `root url redirects to homepage`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Get, "/")) {
                assertEquals(HttpStatusCode.Found, response.status())
                assertEquals("/app", response.headers["Location"])
            }
        }
    }

    @Test
    fun `logout flow`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            handleRequest(HttpMethod.Get, "/app/logout")

            with(handleRequest(HttpMethod.Get, "/app")) {
                assertEquals(HttpStatusCode.Found, response.status())
                assertEquals("/app/login", response.headers["Location"])
            }
        }
    }
}