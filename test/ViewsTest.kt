import dita.dev.data.*
import dita.dev.main
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.*
import io.mockk.*
import org.junit.Test
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.koin.test.mock.declare
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewsTest : AutoCloseKoinTest() {

    private val authRepo: AuthRepo by inject()
    private val examsRepo: ExamsRepo by inject()
    private val messagesRepo: MessagesRepo by inject()

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
            addHeader("X-My-Portal", "")
            addHeader("Origin", "http://localhost:8080")
        }) {
            assertEquals(io.ktor.http.HttpStatusCode.Found, response.status())
            assertEquals("/app/login", response.headers["Location"])

            verify(inverse = true) { authRepo.isTokenValid(any()) }
        }
        // User is redirected to login screen if id_token validation fails
        every { authRepo.isTokenValid(any()) } returns TokenValid.No

        with(handleRequest(HttpMethod.Post, "/app/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            addHeader("X-My-Portal", "")
            addHeader("Origin", "http://localhost:8080")
            setBody(listOf("id_token" to "test").formUrlEncode())
        }) {
            assertEquals(HttpStatusCode.Found, response.status())
            assertEquals("/app/login", response.headers["Location"])

            verify { authRepo.isTokenValid(any()) }
        }

        // User is redirected to forbidden screen if id_token validation is succesful but is not admin
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns false

        with(handleRequest(HttpMethod.Post, "/app/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            addHeader("X-My-Portal", "")
            addHeader("Origin", "http://localhost:8080")
            setBody(listOf("id_token" to "test").formUrlEncode())
        }) {
            assertEquals(HttpStatusCode.Found, response.status())
            assertEquals("/app/forbidden", response.headers["Location"])

            verify { authRepo.isTokenValid(any()) }
        }

        // User is redirected to homepage if id_token validation succeeds
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true

        with(handleRequest(HttpMethod.Post, "/app/login") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            addHeader("X-My-Portal", "")
            addHeader("Origin", "http://localhost:8080")
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
        coEvery { authRepo.isAdmin(any()) } returns true

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
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
        coEvery { authRepo.isAdmin(any()) } returns true

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            handleRequest(HttpMethod.Get, "/app/logout")

            with(handleRequest(HttpMethod.Get, "/app")) {
                assertEquals(HttpStatusCode.Found, response.status())
                assertEquals("/app/login", response.headers["Location"])
            }
        }
    }

    @Test
    fun `exam schedule count is displayed`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        declare {
            mockk<ExamsRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true
        coEvery { examsRepo.getExamScheduleCount() } returns 5

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Get, "/app/exams")) {
                assertTrue { response.content!!.contains("<strong>5</strong>") }
                assertTrue { response.content!!.contains("<b-button variant=\"danger\" @click=\"clearExamSchedule\">Clear</b-button>") }
            }
        }
    }

    @Test
    fun `clear button is not shown if schedule count is 0`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        declare {
            mockk<ExamsRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true
        coEvery { examsRepo.getExamScheduleCount() } returns 0

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Get, "/app/exams")) {
                assertFalse { response.content!!.contains("<b-button variant=\"danger\" @click=\"clearExamSchedule\">Clear</b-button>") }
            }
        }
    }

    @Test
    fun `exam is cleared successfully`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        declare {
            mockk<ExamsRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true
        coEvery { examsRepo.clearExamSchedule() } just Runs

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Post, "/app/exams/delete") {
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }

        coVerify { examsRepo.clearExamSchedule() }
    }

    @Test
    fun `exam schedule is uploaded successfully`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        declare {
            mockk<ExamsRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true
        coEvery { examsRepo.uploadExamSchedule(any()) } just Runs

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Post, "/app/exams/upload") {
                val boundary = "***bbb***"
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                addHeader(
                    HttpHeaders.ContentType,
                    ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
                )
                val stream = File("test/files/excel-new11.xlsx").inputStream()
                setBody(boundary, listOf(
                    PartData.FileItem({ stream.asInput() }, {}, headersOf(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.File
                            .withParameter(ContentDisposition.Parameters.Name, "file")
                            .withParameter(ContentDisposition.Parameters.FileName, "file.xlsx")
                            .toString()
                    )
                    )
                )
                )
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }

        coVerify { examsRepo.uploadExamSchedule(any()) }
    }

    @Test
    fun `exam schedule is not uploaded if file is not an excel file`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        declare {
            mockk<ExamsRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true
        coEvery { examsRepo.uploadExamSchedule(any()) } just Runs

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Post, "/app/exams/upload") {
                val boundary = "***bbb***"

                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                addHeader(
                    HttpHeaders.ContentType,
                    ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
                )
                val stream = File("test/files/excel-new11.xlsx").inputStream()
                setBody(boundary, listOf(
                    PartData.FileItem({ stream.asInput() }, {}, headersOf(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.File
                            .withParameter(ContentDisposition.Parameters.Name, "file")
                            .withParameter(ContentDisposition.Parameters.FileName, "file.txt")
                            .toString()
                    )
                    )
                )
                )
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }

        coVerify(inverse = true) { examsRepo.uploadExamSchedule(any()) }
    }

    @Test
    fun `sending notification successfully`() = withTestApplication(Application::main) {
        declare {
            mockk<AuthRepo>()
        }
        declare {
            mockk<MessagesRepo>()
        }

        // User is redirected to homepage if id_token validation succeeds
        val user = User("1", "test@gmail.com")
        every { authRepo.isTokenValid(any()) } returns TokenValid.Yes(user)
        coEvery { authRepo.isAdmin(any()) } returns true
        coEvery { messagesRepo.sendNotification(any(), any(), any(), any()) } returns true

        cookiesSession {
            handleRequest(HttpMethod.Post, "/app/login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(listOf("id_token" to "test").formUrlEncode())
            }

            with(handleRequest(HttpMethod.Post, "/app/notifications") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                addHeader("X-My-Portal", "")
                addHeader("Origin", "http://localhost:8080")
                setBody(
                    listOf(
                        "topic" to "debug",
                        "title" to "test",
                        "body" to "test message"
                    ).formUrlEncode()
                )
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }

        coVerify {
            messagesRepo.sendNotification(
                "test",
                "test message",
                "debug",
                user.email
            )
        }
    }
}