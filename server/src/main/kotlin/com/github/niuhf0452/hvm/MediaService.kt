package com.github.niuhf0452.hvm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class MediaFile(val name: String, val path: String, val size: Long, val isDirectory: Boolean)

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


class MediaService(private val appConfig: AppConfig, private val taskService: TaskService) {
    suspend fun listFolder(path: String): List<MediaFile> {
        if (path == "/") {
            return appConfig.mediaFolders.map { MediaFile(it.name, "/${it.name}", 0, true) }
        }
        val npath = normalizePath(path)
        val dir = parsePath(npath)
        return withContext(Dispatchers.IO) {
            (dir.listFiles()?.toList() ?: emptyList())
                .map { file ->
                    MediaFile(file.name, "/${npath}/${file.name}", file.length(), file.isDirectory)
                }
                .sortedWith(MediaFileComparator)
        }
    }

    suspend fun move(from: String, to: String) {
        val fromFile = parsePath(normalizePath(from))
        val toFile = parsePath(normalizePath(to))
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

    suspend fun cut(path: String, from: String, to: String) {
        val file = parsePath(normalizePath(path))
        val fromTime = parseTime(from)
        val toTime = parseTime(to)
        val duration = toTime - fromTime
        withContext(Dispatchers.IO) {
            if (!file.exists()) {
                throw IllegalArgumentException("source file not found")
            }
            val clip = computeClipFile(file)
            launch {
                val args = listOf(
                    "docker",
                    "run", "--rm",
                    "-v", "${file.parentFile}:/data",
                    "--entrypoint", "ffmpeg",
                    "linuxserver/ffmpeg",
                    "-i", "/data/${file.name}",
                    "-ss", from,
                    "-t", duration.toString(),
                    "-vcodec", "copy",
                    "-acodec", "copy",
                    "/data/${clip.name}"
                )
                val proc = ProcessBuilder(args)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
                val task = taskService.submitTask("Cut file: $from", kill = proc::destroyForcibly)
                proc.inputStream.bufferedReader().use { reader ->
                    task.appendOutput(args.joinToString(" ", postfix = "\n"))
                    reader.lines().forEach { line ->
                        task.appendOutput(line)
                    }
                }
            }
        }
    }

    private fun normalizePath(path: String): String {
        var npath = path
        while (npath.startsWith("/")) {
            npath = npath.substring(1)
        }
        while (npath.endsWith("/")) {
            npath = npath.substring(0, npath.length - 1)
        }
        if (npath.isEmpty()) {
            throw IllegalArgumentException("path is empty")
        }
        return npath
    }

    private fun parsePath(nPath: String): java.io.File {
        val pathList = nPath.split("/")
        val name = pathList.first()
        val lib = appConfig.mediaFolders.find { it.name == name }
            ?: throw IllegalArgumentException("path $name not found")
        val file = lib.resolvedPath.resolve(pathList.drop(1).joinToString("/"))
        if (!file.canonicalPath.startsWith(lib.resolvedPath.canonicalPath)) {
            throw IllegalArgumentException("path $nPath not found")
        }
        return file
    }

    private val pattern = Regex("""(\d{2}):(\d{2}):(\d{2})""")

    private fun parseTime(time: String): Long {
        val m = pattern.matchEntire(time) ?: throw IllegalArgumentException("time format error")
        return m.groupValues[1].toLong() * 3600 + m.groupValues[2].toLong() * 60 + m.groupValues[3].toLong()
    }

    private fun computeClipFile(file: java.io.File): java.io.File {
        for (i in 1..100) {
            val clip = file.parentFile.resolve("${file.nameWithoutExtension}-1.${file.extension}")
            if (!clip.exists()) {
                return clip
            }
        }
        throw IllegalStateException("cannot find a file name for the clip")
    }
}