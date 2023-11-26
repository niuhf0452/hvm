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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val hoconConfig = ConfigFactory.load()
        .withFallback(ConfigFactory.parseResources("defaults.conf"))
        .resolve()
    val appConfig = Hocon.decodeFromConfig<AppConfig>(hoconConfig.getConfig("hvm"))
    val idGen = IdGen.create(System::currentTimeMillis, appConfig.idGen.workerId)
    val taskService = TaskService.create(appConfig.tasks)
    val libraryService = LibraryService.create(appConfig.library)
    val server = embeddedServer(CIO, port = appConfig.port) {
        configure(appConfig, idGen, taskService, libraryService)
    }
    server.addShutdownHook {
        taskService.close()
    }
    server.start(wait = true)
}

@Serializable
data class ErrorResponse(val error: String)

fun Application.configure(
    appConfig: AppConfig,
    idGen: IdGen,
    taskService: TaskService,
    libraryService: LibraryService
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
            addStaticRoutes(appConfig.library)
            addLibraryRoutes(libraryService)
            addTasksRoutes(taskService)
            addMediaRoutes(appConfig.library, idGen, taskService)
        }
    }
}

fun Route.addStaticRoutes(config: LibraryConfig) {
    config.folders.forEach { mediaFolder ->
        staticFiles(remotePath = "/api/media/file/${mediaFolder.name}", dir = mediaFolder.resolvedPath) {
            enableAutoHeadResponse()
        }
    }
    staticResources(remotePath = "/", basePackage = "web")
}

fun Route.addLibraryRoutes(libraryService: LibraryService) {
    get("/api/media/browse") {
        val path = call.request.queryParameters.getOrFail("path")
        val files = libraryService.listFolder(path)
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.OK)
        call.respond(files)
    }
    post("/api/media/move") {
        val from = call.request.queryParameters.getOrFail("from")
        val to = call.request.queryParameters.getOrFail("to")
        libraryService.move(from, to)
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.OK)
    }
    post("/api/media/delete") {
        val file = call.request.queryParameters.getOrFail("file")
        libraryService.delete(file)
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.OK)
    }
}

fun Route.addTasksRoutes(taskService: TaskService) {
    get("/api/tasks") {
        val flow = taskService.subscribe()
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.OK)
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            flow.flowOn(Dispatchers.IO)
                .collect { data ->
                    write("data: ${Json.encodeToString(data)}\n\n")
                    flush()
                }
        }
    }
    delete("/api/tasks/task/{id}") {
        val id = call.parameters.getOrFail("id").toLong()
        taskService.remove(id)
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.NoContent)
    }
    post("/api/tasks/clear") {
        taskService.clear()
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.NoContent)
    }
}

fun Route.addMediaRoutes(config: LibraryConfig, idGen: IdGen, taskService: TaskService) {
    val tasks = Tasks(config, idGen)
    post("/api/media/cut") {
        val path = call.request.queryParameters.getOrFail("path")
        val from = call.request.queryParameters.getOrFail("from")
        val to = call.request.queryParameters.getOrFail("to")
        taskService.submit(tasks.cut(path, from, to))
        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Public))
        call.response.status(HttpStatusCode.OK)
    }
}
