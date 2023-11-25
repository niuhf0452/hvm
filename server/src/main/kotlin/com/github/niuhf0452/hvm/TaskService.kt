package com.github.niuhf0452.hvm

import com.github.niuhf0452.hvm.impl.TaskServiceImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface Task {
    val id: Long
    val name: String

    suspend fun run(updater: TaskStateUpdater)

    fun cancel()
}

interface TaskStateUpdater {
    fun progress(percent: Int)
    fun error(message: String)
    fun finish()
    fun cancel()
}

@Serializable
enum class TaskState {
    @SerialName("pending")
    Pending,

    @SerialName("running")
    Running,

    @SerialName("finished")
    Finished,

    @SerialName("error")
    Error,

    @SerialName("canceled")
    Canceled;

    fun isCompleted(): Boolean {
        return this == Finished || this == Error || this == Canceled
    }
}

@Serializable
data class TaskInfo(
    val id: Long,
    val name: String,
    val percent: Int,
    val error: String?,
    val state: TaskState,
)

interface TaskService : AutoCloseable {
    fun submit(task: Task)
    fun remove(id: Long)
    fun clear()
    fun subscribe(): Flow<List<TaskInfo>>

    companion object {
        fun create(config: TasksConfig): TaskService {
            return TaskServiceImpl(config)
        }
    }
}
