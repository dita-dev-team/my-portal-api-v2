buildscript {
    repositories {
        mavenCentral()

    }
    dependencies {
        classpath("net.saliman:gradle-properties-plugin:1.4.6")
    }
}

allprojects {
    apply<net.saliman.gradle.plugin.properties.PropertiesPlugin>()
}

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val koin_version: String by project
val project_id: String by project
val okhttp_version: String by project

plugins {
    application
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "dita.dev"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks.withType<JavaExec> {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach {
            val line = it.trim()
            val (key, value) = line.split("=")
            environment(key, value)
        }
    } else {
        println(".env file not found")
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}

tasks.withType<Test> {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach {
            val line = it.trim()
            val (key, value) = line.split("=")
            environment(key, value)
        }
    } else {
        println(".env file not found")
    }
    testLogging {
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true

        debug {
            events = setOf(
                org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
                org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
            )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        info.events = debug.events
        info.exceptionFormat = debug.exceptionFormat

        addTestListener(object : TestListener {
            override fun beforeTest(p0: TestDescriptor?) = Unit
            override fun beforeSuite(p0: TestDescriptor?) = Unit
            override fun afterTest(desc: TestDescriptor, result: TestResult) = Unit
            override fun afterSuite(desc: TestDescriptor, result: TestResult) {
                printResults(desc, result)
            }
        })
    }
}

fun printResults(desc: TestDescriptor, result: TestResult) {
    if (desc.parent != null) {
        val output = result.run {
            "Results: $resultType (" +
                    "$testCount tests, " +
                    "$successfulTestCount successes, " +
                    "$failedTestCount failures, " +
                    "$skippedTestCount skipped" +
                    ")"
        }
        val testResultLine = "|  $output  |"
        val repeatLength = testResultLine.length
        val separationLine = "-".repeat(repeatLength)
        println(separationLine)
        println(testResultLine)
        println(separationLine)
    }
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-pebble:$ktor_version")
    implementation("com.squareup.okhttp3:okhttp:${okhttp_version}")
    implementation("com.squareup.okhttp3:logging-interceptor:${okhttp_version}")
    implementation("com.squareup.moshi:moshi-kotlin:1.9.3")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.koin:koin-ktor:$koin_version")
    implementation("org.koin:koin-logger-slf4j:$koin_version")
    implementation("com.google.firebase:firebase-admin:7.0.0")
    implementation("com.github.kittinunf.fuse:fuse:1.0.0-beta3")
    implementation("org.apache.poi:poi:4.1.2")
    implementation("org.apache.poi:poi-ooxml:4.1.2")
    implementation("org.jsoup:jsoup:1.13.1")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.koin:koin-test:$koin_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:${okhttp_version}")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
