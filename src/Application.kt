package dita.dev

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import dita.dev.data.*
import dita.dev.utils.ExcelParser
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.pebble.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import org.koin.core.logger.Level
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger
import org.mpierce.ktor.csrf.CsrfProtection
import org.mpierce.ktor.csrf.HeaderPresent
import org.mpierce.ktor.csrf.OriginMatchesKnownHost
import org.mpierce.ktor.csrf.csrfProtection
import java.io.File
import java.security.SecureRandom

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.main(testing: Boolean = false) {
    install(DefaultHeaders)
    install(AutoHeadResponse)
    install(CallLogging)
    install(Koin) {
        slf4jLogger(Level.ERROR)
        modules(appModules)
    }
    install(Authentication) {
        basic("hooks") {
            realm = "my-portal"
            validate { credentials ->
                val user = environment.getConfigValue("auth.basic_user", "test")
                val password = environment.getConfigValue("auth.basic_password", "test")
                if (credentials.password == password && credentials.name == user) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
        })
    }
    val scheme = environment.getConfigValue("app.scheme", "http")
    val host = environment.getConfigValue("app.domain", "localhost")
    val port = if (host == "localhost") 8080 else null
    install(CsrfProtection) {
        validate(OriginMatchesKnownHost(scheme, host, port))
        validate(HeaderPresent("X-My-Portal"))
    }

    val secretEncryptKey = hex(environment.getConfigValue("session.encrypt_key", "00112233445566778899aabbccddeeff"))
    val secretAuthKey = hex(environment.getConfigValue("session.auth_key", "6819b57a326945c1968f45236581"))
    install(Sessions) {
        cookie<UserSession>("SESSION") {
            transform(
                SessionTransportTransformerEncrypt(
                    secretEncryptKey,
                    secretAuthKey,
                    ivGenerator = {
                        SecureRandom().generateSeed(16)
                    }
                )
            )
        }
    }

    val remoteConfigRepo by inject<RemoteConfigRepo>()
    val authRepo by inject<AuthRepo>()
    val examsRepo by inject<ExamsRepo>()
    val messagesRepo by inject<MessagesRepo>()

    routing {
        static("static") {
            resources("static")
        }

        get("/") {
            call.respondRedirect("/app")
        }

        get("/health_check") {
            call.respondText("OK")
        }

        authenticate("hooks") {
            post("update_calendar") {
                remoteConfigRepo.updateCurrentCalendar()
                call.respondText("OK")
            }

            post("check_exam_period") {
                remoteConfigRepo.checkExamPeriod()
                call.respondText("OK")
            }
        }

        route("app") {
            intercept(ApplicationCallPipeline.Setup) {
                val path = call.request.path()
                if (path.contains("app")) {
                    if (call.isLoggedIn()) {
                        if (path == "/app/login") {
                            call.respondRedirect("/app")
                            return@intercept finish()
                        }
                    } else {
                        if (path != "/app/login" && path != "/app/forbidden") {
                            call.respondRedirect("/app/login")
                            return@intercept finish()
                        }
                    }
                }
            }

            get {
                call.respond(getLoggedInPebbleContent("index.peb"))
            }

            route("login") {
                get {
                    call.respond(PebbleContent("login.peb", FirebaseConfig.generateModel(emptyMap())))
                }

                csrfProtection {
                    post {
                        val payload = call.receiveParameters()
                        if (payload["id_token"] == null) {
                            call.respondRedirect("/app/login", permanent = false)
                        } else {
                            val isValid = authRepo.isTokenValid(payload["id_token"]!!)
                            when (isValid) {
                                is TokenValid.No -> {
                                    call.respondRedirect("/app/login", permanent = false)
                                }
                                is TokenValid.Yes -> {
                                    if (authRepo.isAdmin(isValid.data.uid)) {
                                        // Session should expire in 24 hours
                                        val expiresAt = System.currentTimeMillis() + (1000 * 60 * 60 * 24)
                                        call.sessions.set(
                                            UserSession(
                                                isValid.data.uid,
                                                isValid.data.email,
                                                expiresAt
                                            )
                                        )
                                        call.respondRedirect("/app", permanent = false)
                                    } else {
                                        call.respondRedirect("/app/forbidden", permanent = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            route("logout") {
                get {
                    if (call.isLoggedIn()) {
                        call.sessions.clear<UserSession>()
                    }
                    call.respondRedirect("/app")
                }
            }

            route("forbidden") {
                get {
                    call.respond(PebbleContent("forbidden.peb", FirebaseConfig.generateModel(emptyMap())))
                }
            }

            route("exams") {
                get {
                    val totalExamSchedules = examsRepo.getExamScheduleCount()
                    val model = mapOf(
                        "examsScheduleCount" to totalExamSchedules
                    )
                    call.respond(getLoggedInPebbleContent("exams.peb", model))
                }

                route("upload") {
                    csrfProtection {
                        post {
                            val parser = ExcelParser()
                            val multipart = call.receiveMultipart()
                            multipart.forEachPart { part ->
                                when (part) {
                                    is PartData.FileItem -> {
                                        val ext = File(part.originalFileName).extension
                                        if (ext == "xls" || ext == "xlsx") {
                                            part.streamProvider().use { input ->
                                                parser.extractData(input)
                                            }
                                        }
                                    }
                                    else -> {
                                    }
                                }

                                part.dispose()
                            }
                            if (parser.units.isNotEmpty()) {
                                examsRepo.uploadExamSchedule(parser.units)
                            }
                            call.respondText("Ok")
                        }
                    }

                }

                route("delete") {
                    csrfProtection {
                        post {
                            examsRepo.clearExamSchedule()
                            call.respondText("Ok")
                        }
                    }
                }
            }

            route("notifications") {
                get {
                    call.respond(getLoggedInPebbleContent("notifications.peb", emptyMap()))
                }

                csrfProtection {
                    post {
                        val payload = call.receiveParameters()
                        val topic = payload["topic"]!!
                        val title = payload["title"]!!
                        val body = payload["body"]!!
                        val session = call.getUserSession()
                        messagesRepo.sendNotification(
                            title,
                            body,
                            topic,
                            session.email
                        )
                        call.respondText("Ok")
                    }
                }
            }

            route("buildings") {
                get {
                    call.respond(getLoggedInPebbleContent("buildings.peb", emptyMap()))
                }
            }
        }
    }
}

fun ApplicationCall.isLoggedIn(): Boolean {
    val session = sessions.get<UserSession>()
    return session != null && System.currentTimeMillis() < session.expiration
}

fun ApplicationCall.getUserSession(): UserSession {
    val session = sessions.get<UserSession>() ?: error("No session found")
    if (System.currentTimeMillis() > session.expiration) {
        error("Session expired")
    }
    return session
}

fun ApplicationEnvironment.getConfigValue(key: String, default: String): String =
    this.config.propertyOrNull(key)?.getString() ?: default


fun getLoggedInPebbleContent(template: String, model: Map<String, Any> = emptyMap()): PebbleContent {
    val finalMap = mutableMapOf<String, Any>()
    finalMap.putAll(FirebaseConfig.generateModel(mapOf("isLoggedIn" to true)))
    finalMap.putAll(model)
    return PebbleContent(template, finalMap)
}