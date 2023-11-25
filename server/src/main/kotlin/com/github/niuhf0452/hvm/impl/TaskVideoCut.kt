package com.github.niuhf0452.hvm.impl

import com.github.niuhf0452.hvm.Task
import com.github.niuhf0452.hvm.TaskStateUpdater
import kotlinx.coroutines.coroutineScope
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.builder.FFmpegBuilder
import java.io.File
import java.util.concurrent.TimeUnit

class TaskVideoCut(
    override val id: Long,
    override val name: String,
    val file: File,
    val fromTimeInSec: Long,
    val toTimeInSec: Long
) : Task {
    private var thread: Thread? = null

    override suspend fun run(updater: TaskStateUpdater) {
        val duration = toTimeInSec - fromTimeInSec
        coroutineScope {
            if (!file.exists()) {
                throw IllegalArgumentException("source file not found")
            }
            val clip = computeClipFile(file)
            val ffmpeg = FFmpeg("ffmpeg")
            val executor = FFmpegExecutor(ffmpeg)
            val builder = FFmpegBuilder()
            builder.addInput(file.toString())
                .addOutput("${file.parentFile}/${clip.name}")
                .setVideoCodec("copy")
                .setAudioCodec("copy")
                .setStartOffset(fromTimeInSec, TimeUnit.SECONDS)
                .setDuration(duration, TimeUnit.SECONDS)
            val durationNs = TimeUnit.SECONDS.toNanos(duration).toDouble()
            val job = executor.createJob(builder) { progress ->
                val percentage = progress.out_time_ns / durationNs
                updater.progress((percentage * 100).toInt())
            }
            thread = Thread.currentThread()
            try {
                job.run()
            } catch (e: InterruptedException) {
                updater.error("cancelled")
            } finally {
                thread = null
            }
        }
    }

    override fun cancel() {
        thread?.interrupt()
    }

    private fun computeClipFile(file: File): File {
        for (i in 1..100) {
            val clip = file.parentFile.resolve("${file.nameWithoutExtension}-$i.${file.extension}")
            if (!clip.exists()) {
                return clip
            }
        }
        throw IllegalStateException("cannot find a file name for the clip")
    }
}