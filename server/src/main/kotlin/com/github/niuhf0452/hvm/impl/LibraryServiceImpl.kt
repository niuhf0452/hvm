package com.github.niuhf0452.hvm.impl

import com.github.niuhf0452.hvm.LibraryConfig
import com.github.niuhf0452.hvm.LibraryService
import com.github.niuhf0452.hvm.MediaFile
import com.github.niuhf0452.hvm.Utilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaFileComparator : Comparator<MediaFile> {
    override fun compare(a: MediaFile, b: MediaFile): Int {
        if (a.isDirectory && !b.isDirectory) {
            return -1
        }
        if (!a.isDirectory && b.isDirectory) {
            return 1
        }
        return a.name.compareTo(b.name)
    }
}

class LibraryServiceImpl(
    private val config: LibraryConfig
) : LibraryService {
    override suspend fun listFolder(path: String): List<MediaFile> {
        if (path == "/") {
            return config.folders.map { MediaFile(it.name, "/${it.name}", 0, true) }
        }
        val npath = Utilities.normalizePath(path)
        val dir = config.parsePath(npath)
        return withContext(Dispatchers.IO) {
            (dir.listFiles()?.toList() ?: emptyList())
                .map { file ->
                    MediaFile(file.name, "/${npath}/${file.name}", file.length(), file.isDirectory)
                }
                .sortedWith(MediaFileComparator)
        }
    }

    override suspend fun move(from: String, to: String) {
        val fromFile = config.parsePath(Utilities.normalizePath(from))
        val toFile = config.parsePath(Utilities.normalizePath(to))
        withContext(Dispatchers.IO) {
            if (!fromFile.exists()) {
                throw IllegalArgumentException("source file not found")
            }
            if (toFile.exists()) {
                throw IllegalArgumentException("destination file already exists")
            }
            fromFile.renameTo(toFile)
        }
    }

    override suspend fun delete(path: String) {
        val file = config.parsePath(Utilities.normalizePath(path))
        withContext(Dispatchers.IO) {
            if (!file.exists()) {
                throw IllegalArgumentException("source file not found")
            }
            file.delete()
        }
    }
}