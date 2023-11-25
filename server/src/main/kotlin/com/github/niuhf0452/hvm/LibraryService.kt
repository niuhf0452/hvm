package com.github.niuhf0452.hvm

import com.github.niuhf0452.hvm.impl.LibraryServiceImpl
import kotlinx.serialization.Serializable

@Serializable
data class MediaFile(val name: String, val path: String, val size: Long, val isDirectory: Boolean)

interface LibraryService {
    suspend fun listFolder(path: String): List<MediaFile>
    suspend fun move(from: String, to: String)
    suspend fun delete(path: String)

    companion object {
        fun create(config: LibraryConfig): LibraryService {
            return LibraryServiceImpl(config)
        }
    }
}
