package com.github.niuhf0452.hvm

import com.github.niuhf0452.hvm.Utilities.normalizePath
import com.github.niuhf0452.hvm.Utilities.parseTime
import com.github.niuhf0452.hvm.impl.TaskVideoCut

class Tasks(
    private val config: LibraryConfig,
    private val idGen: IdGen
) {
    fun cut(path: String, from: String, to: String): Task {
        val file = config.parsePath(normalizePath(path))
        val fromTime = parseTime(from)
        val toTime = parseTime(to)
        return TaskVideoCut(idGen.nextId(), "Cut ${file.name}", file, fromTime, toTime)
    }
}