package com.dss.emulator.bluetooth

import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class DataQueueManager private constructor() {
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val isRunning = AtomicBoolean(false)
    private val listeners = mutableListOf<(ByteArray) -> Unit>()
    @Volatile
    private var executor: ExecutorService? = null

    companion object {
        @Volatile
        private var instance: DataQueueManager? = null

        fun getInstance(): DataQueueManager {
            return instance ?: synchronized(this) {
                instance ?: DataQueueManager().also { instance = it }
            }
        }
    }

    fun addData(data: ByteArray) {
        Log.d("DataQueueManager", "Adding data to queue: ${data.size} bytes: " + isRunning.get())
        if (!isRunning.get()) {
            start()
        }
        if (!queue.offer(data)) {
            Log.e("DataQueueManager", "Failed to add data to queue - queue might be full")
        }
    }

    fun addListener(listener: (ByteArray) -> Unit) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: (ByteArray) -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    @Synchronized
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            executor = Executors.newSingleThreadExecutor { r ->
                Thread(r, "DataQueueProcessor").apply {
                    isDaemon = true
                    priority = Thread.NORM_PRIORITY
                }
            }
            executor?.execute(::processQueue)
        }
    }

    private fun processQueue() {
        while (isRunning.get()) {
            try {
                val data = queue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                val currentListeners = synchronized(listeners) {
                    listeners.toList()
                }
                currentListeners.forEach { listener ->
                    try {
                        listener(data)
                    } catch (e: Exception) {
                        Log.e("DataQueueManager", "Error in listener callback: ${e.message}", e)
                    }
                }
            } catch (e: InterruptedException) {
                Log.d("DataQueueManager", "Queue processing interrupted")
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                Log.e("DataQueueManager", "Error processing queue: ${e.message}", e)
            }
        }
    }

    @Synchronized
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            executor?.shutdown()
            try {
                if (executor?.awaitTermination(1, TimeUnit.SECONDS) != true) {
                    executor?.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor?.shutdownNow()
                Thread.currentThread().interrupt()
            } finally {
                executor = null
            }

            synchronized(listeners) {
                listeners.clear()
            }
            queue.clear()
        }
    }

    fun isActive(): Boolean = isRunning.get()
}
