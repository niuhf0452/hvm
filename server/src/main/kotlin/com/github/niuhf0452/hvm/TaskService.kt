package com.github.niuhf0452.hvm

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Serializable
data class TaskStatus(
        val id: Long,
        val name: String,
        val progress: Int, // unit 1/1000
        val output: String
)

class TaskService {
    private val taskIdGen = AtomicLong(0)
    private val tasks = ConcurrentLinkedQueue<Task>()

    fun listTasks(): List<TaskStatus> {
        return tasks.map { task ->
            var progress = (task.progress.get() * 1000 / task.size).toInt()
            if (progress < 0) progress = -1
            else if (progress > 1000) progress = 1000
            TaskStatus(task.id, task.name, progress, task.output.get())
        }
    }

    fun submitTask(name: String, size: Long, kill: () -> Unit): Task {
        val task = Task(name, size, kill)
        tasks.add(task)
        return task
    }

    fun kill(id: Long) {
        tasks.find { it.id == id }?.kill?.invoke()
    }

    inner class Task(val name: String, val size: Long, val kill: () -> Unit) {
        val id = taskIdGen.incrementAndGet()
        val progress = AtomicLong(0)
        val output = AtomicReference<String>("")

        fun done() {
            tasks.remove(this)
        }

        fun updateProgress(progress: Long) {
            this.progress.set(progress)
        }

        fun appendOutput(output: String) {
            this.output.updateAndGet { it + output }
        }
    }
}