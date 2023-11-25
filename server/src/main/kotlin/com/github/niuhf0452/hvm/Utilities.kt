package com.github.niuhf0452.hvm

object Utilities {
    fun normalizePath(path: String): String {
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

    private val pattern = Regex("""(\d{2}):(\d{2}):(\d{2})""")

    fun parseTime(time: String): Long {
        val m = pattern.matchEntire(time) ?: throw IllegalArgumentException("time format error")
        return m.groupValues[1].toLong() * 3600 + m.groupValues[2].toLong() * 60 + m.groupValues[3].toLong()
    }
}