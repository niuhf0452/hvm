package com.github.niuhf0452.hvm

import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val hoconConfig = ConfigFactory.load()
    val appConfig = Hocon.decodeFromConfig<AppConfig>(hoconConfig.getConfig("hvm"))
    embeddedServer(CIO, port = appConfig.port, module = { configure(appConfig) })
        .start(wait = true)
}

@Serializable
data class ErrorResponse(val error: String)

fun Application.configure(
    appConfig: AppConfig,
    taskService: TaskService = TaskService(),
    mediaService: MediaService = MediaService(appConfig, taskService)
) {
    install(ContentNegotiation) {
        json()
    }
    install(PartialContent)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Unknown error"))
        }
    }
    routing {
        route(appConfig.rootPath) {
            get("/media/browse") {
                val path = call.request.queryParameters.getOrFail("path")
                val files = mediaService.listFolder(path)
                call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
                call.response.status(HttpStatusCode.OK)
                call.respond(files)
            }
            post("/media/move") {
                val from = call.request.queryParameters.getOrFail("from")
                val to = call.request.queryParameters.getOrFail("to")
                mediaService.move(from, to)
                call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
                call.response.status(HttpStatusCode.OK)
            }
            post("/media/cut") {
                val path = call.request.queryParameters.getOrFail("path")
                val from = call.request.queryParameters.getOrFail("from")
                val to = call.request.queryParameters.getOrFail("to")
                mediaService.cut(path, from, to)
                call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
                call.response.status(HttpStatusCode.OK)
            }
            get("/tasks") {
                val tasks = taskService.listTasks()
                call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
                call.response.status(HttpStatusCode.OK)
                call.respond(tasks)
            }
            appConfig.mediaFolders.forEach { mediaFolder ->
                staticFiles(remotePath = "/media/file/${mediaFolder.name}", dir = mediaFolder.resolvedPath) {
                    enableAutoHeadResponse()
                }
            }
        }
    }
}
