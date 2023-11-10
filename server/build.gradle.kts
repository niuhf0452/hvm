import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.graalvm.buildtools.native") version "0.9.28"
}

group = "com.github.niuhf0452"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.github.niuhf0452.hvm.MainKt")
}

object Ver {
    const val kotlin = "1.9.20"
    const val logback = "1.4.11"
    const val serialization = "1.6.0"
    const val ktor = "2.3.5"

    const val junit = "5.9.3"
}

dependencies {
    implementation(kotlin("stdlib-jdk8", Ver.kotlin))
    implementation("ch.qos.logback:logback-classic:${Ver.logback}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:${Ver.serialization}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${Ver.ktor}")
    implementation("io.ktor:ktor-server-core-jvm:${Ver.ktor}")
    implementation("io.ktor:ktor-server-cio-jvm:${Ver.ktor}")
    implementation("io.ktor:ktor-server-content-negotiation:${Ver.ktor}")
    implementation("io.ktor:ktor-server-partial-content:${Ver.ktor}")
    implementation("io.ktor:ktor-server-status-pages:${Ver.ktor}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Ver.junit}")
    testImplementation(kotlin("test", Ver.kotlin))
}

kotlin {
    jvmToolchain(17)
}

graalvmNative {
    toolchainDetection.set(true)
    binaries {
        named("main") {
            imageName.set("hvm")
            mainClass.set("com.github.niuhf0452.hvm.MainKt")
            buildArgs.add("-O4")
            useFatJar.set(true)
        }
        all {
            resources.autodetect()
            buildArgs.addAll("--verbose", "--initialize-at-build-time=ch.qos.logback,org.slf4j")
        }
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    mergeServiceFiles()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(PASSED, SKIPPED, FAILED)
    }
}
