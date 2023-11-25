package com.github.niuhf0452.hvm

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
data class AppConfig(
    val port: Int,
    val rootPath: String,
    val library: LibraryConfig,
    val tasks: TasksConfig,
    val idGen: IdGenConfig
)

@Serializable
data class LibraryConfig(val folders: List<MediaFolder>) {
    fun parsePath(nPath: String): java.io.File {
        val pathList = nPath.split("/")
        val name = pathList.first()
        val lib = folders.find { it.name == name }
            ?: throw IllegalArgumentException("path $name not found")
        val file = lib.resolvedPath.resolve(pathList.drop(1).joinToString("/"))
        if (!file.canonicalPath.startsWith(lib.resolvedPath.canonicalPath)) {
            throw IllegalArgumentException("path $nPath not found")
        }
        return file
    }
}

@Serializable
data class MediaFolder(val name: String, val path: String) {
    @Transient
    val resolvedPath: java.io.File = resolve()

    private fun resolve(): java.io.File {
        val p = if (path.startsWith("~")) {
            val home = System.getProperty("user.home") ?: System.getenv("HOME")
            home + path.substring(1)
        } else if (path.startsWith("/")) {
            path
        } else {
            "/$path"
        }
        return java.io.File(p)
    }
}

@Serializable
data class TasksConfig(val concurrency: Int, val cleanUpDelay: Duration)

@Serializable
data class IdGenConfig(val workerId: Int)
