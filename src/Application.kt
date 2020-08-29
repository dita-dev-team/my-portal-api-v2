package dita.dev

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.core.logger.Level
import org.koin.ktor.ext.Koin
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

    routing {
        get("health_check") {
            call.respondText("OK")
        }
    }
}

