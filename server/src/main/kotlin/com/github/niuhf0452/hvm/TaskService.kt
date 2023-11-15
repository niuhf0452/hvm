package com.github.niuhf0452.hvm

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Serializable
data class TaskStatus(
    val id: Long,
    val name: String,
    val output: String
)

class TaskService {
    private val taskIdGen = AtomicLong(0)
    private val tasks = ConcurrentLinkedQueue<Task>()

    fun listTasks(): List<TaskStatus> {
        return tasks.map { task ->
            TaskStatus(task.id, task.name, task.output.get())
        }
    }

    fun submitTask(name: String, kill: () -> Unit): Task {
        val task = Task(name, kill)
        tasks.add(task)
        return task
    }

    fun kill(id: Long) {
        tasks.find { it.id == id }?.kill?.invoke()
    }

    fun remove(id: Long) {
        tasks.removeIf { it.id == id }
    }

    fun clear() {
        tasks.clear()
    }

    inner class Task(val name: String, val kill: () -> Unit) {
        val id = taskIdGen.incrementAndGet()
        val output = AtomicReference("")

        fun appendOutput(output: String) {
            this.output.updateAndGet { it + output }
        }
    }
}