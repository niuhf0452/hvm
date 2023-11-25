package com.github.niuhf0452.hvm.impl

import com.github.niuhf0452.hvm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors

@OptIn(DelicateCoroutinesApi::class)
class TaskServiceImpl(
    private val config: TasksConfig
) : TaskService {
    private val queue = Channel<Command>(Channel.UNLIMITED)
    private val jobs = ConcurrentSkipListMap<Long, Job>()
    private val dispatcher = Executors.newFixedThreadPool(config.concurrency).asCoroutineDispatcher()
    private val loopJob = GlobalScope.launch { commandLoop() }
    private val flow = MutableStateFlow<List<TaskInfo>>(emptyList())

    override fun close() {
        runBlocking {
            queue.close()
            loopJob.cancelAndJoin()
        }
        dispatcher.close()
    }

    override fun submit(task: Task) {
        sendCommand(Submit(task))
    }

    override fun subscribe(): Flow<List<TaskInfo>> {
        return flow
    }

    override fun remove(id: Long) {
        sendCommand(Remove(id))
    }

    override fun clear() {
        sendCommand(Clear)
    }

    private fun sendCommand(command: Command) {
        val result = queue.trySend(command)
        if (result.isClosed) {
            throw IllegalStateException("Task service is closed")
        }
    }

    private suspend fun commandLoop() {
        while (true) {
            val result = queue.receiveCatching()
            if (result.isClosed || result.isFailure) {
                break
            }
            when (val command = result.getOrThrow()) {
                is Submit -> {
                    val info = TaskInfo(
                        id = command.task.id,
                        name = command.task.name,
                        percent = 0,
                        error = null,
                        state = TaskState.Pending
                    )
                    flow.emit(flow.value + info)
                    GlobalScope.launch(dispatcher) {
                        val deferred = CompletableDeferred<TaskState>()
                        sendCommand(UpdateState(command.task.id, TaskState.Running, deferred = deferred))
                        if (deferred.await() != TaskState.Running) {
                            return@launch
                        }
                        val updater = object : TaskStateUpdater {
                            override fun progress(percent: Int) {
                                sendCommand(UpdatePercent(command.task.id, percent))
                            }

                            override fun error(message: String) {
                                sendCommand(UpdateState(command.task.id, TaskState.Error, message))
                            }

                            override fun finish() {
                                sendCommand(UpdateState(command.task.id, TaskState.Finished))
                            }

                            override fun cancel() {
                                sendCommand(UpdateState(command.task.id, TaskState.Canceled))
                            }
                        }
                        command.task.run(updater)
                        updater.finish()
                    }
                }

                is Remove -> {
                    val job = jobs.remove(command.id)
                    try {
                        job?.cancelAndJoin()
                    } catch (e: Exception) {
                        // Ignore
                    }
                    flow.emit(flow.value.filter { it.id != command.id })
                }

                is Clear -> {
                    flow.emit(flow.value.filter { !it.state.isCompleted() })
                }

                is UpdatePercent -> {
                    flow.emit(flow.value.map { info ->
                        if (info.id == command.id) info.copy(percent = command.percent) else info
                    })
                }

                is UpdateState -> {
                    flow.emit(flow.value.map { info ->
                        if (info.id == command.id) {
                            val updated = when (info.state to command.state) {
                                TaskState.Pending to TaskState.Running -> info.copy(state = TaskState.Running)
                                TaskState.Pending to TaskState.Canceled -> info.copy(state = TaskState.Canceled)
                                TaskState.Running to TaskState.Finished -> info.copy(
                                    state = TaskState.Finished,
                                    percent = 100
                                )

                                TaskState.Running to TaskState.Error -> info.copy(
                                    state = TaskState.Error,
                                    error = command.error
                                )

                                TaskState.Running to TaskState.Canceled -> info.copy(state = TaskState.Canceled)
                                else -> info
                            }
                            if (command.deferred != null) {
                                command.deferred.complete(updated.state)
                            }
                            cleanUp(updated)
                            updated
                        } else {
                            info
                        }
                    })
                }
            }
        }
    }

    private suspend fun cleanUp(info: TaskInfo) {
        if (info.state.isCompleted()) {
            jobs.remove(info.id)
            // do not block current coroutine
            GlobalScope.launch {
                delay(config.cleanUpDelay)
                sendCommand(Remove(info.id))
            }
        }
    }

    sealed interface Command

    data class Submit(val task: Task) : Command
    data class Remove(val id: Long) : Command
    data object Clear : Command
    data class UpdatePercent(val id: Long, val percent: Int) : Command
    data class UpdateState(
        val id: Long,
        val state: TaskState,
        val error: String? = null,
        val deferred: CompletableDeferred<TaskState>? = null
    ) : Command
}