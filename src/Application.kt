package dita.dev

import dita.dev.data.RemoteConfigRepo
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.core.logger.Level
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.main(testing: Boolean = false) {
    install(DefaultHeaders)
    install(CallLogging)
    install(Koin) {
        slf4jLogger(Level.ERROR)
        modules(appModules)
    }
    install(Authentication) {
        basic("hooks") {
            realm = "my-portal"
            validate { credentials ->
                val user = System.getProperty("basic_user", "test")
                val password = System.getProperty("basic_password", "test")
                if (credentials.password == password && credentials.name == user) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    val remoteConfigRepo by inject<RemoteConfigRepo>()

    routing {
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


    }
}

