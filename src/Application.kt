package dita.dev

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import dita.dev.data.AuthRepo
import dita.dev.data.RemoteConfigRepo
import dita.dev.data.TokenValid
import dita.dev.data.UserSession
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

//    val secretEncryptKey = hex(System.getProperty("session_encrypt_key"))
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

    val googleClientId = System.getProperty("client_id")

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
                        if (path != "/app/login") {
                            call.respondRedirect("/app/login")
                            return@intercept finish()
                        }
                    }
                }
            }

            get {
                call.respond(PebbleContent("index.peb", emptyMap()))
            }

            route("login") {
                get {
                    call.respond(PebbleContent("login.peb", mapOf("clientId" to googleClientId)))
                }

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
                            }
                        }
                    }
                }
            }

            route("logout") {
                get {
                    call.sessions.clear<UserSession>()
                    call.respondRedirect("/app")
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


