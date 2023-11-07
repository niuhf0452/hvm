package com.github.niuhf0452.hvm

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class AppConfig(val port: Int, val rootPath: String, val mediaFolders: List<MediaFolder>)

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
