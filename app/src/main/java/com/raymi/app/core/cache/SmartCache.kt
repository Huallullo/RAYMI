package com.raymi.app.core.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
    val ttlMs: Long = 5 * 60 * 1000 // 5 minutos por defecto
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > ttlMs
}

class SmartCache<T> {
    private val _state = MutableStateFlow<CacheEntry<T>?>(null)
    val state: StateFlow<CacheEntry<T>?> = _state.asStateFlow()
    private val mutex = Mutex() // OPTIMIZACIÓN: Thread-safe para escrituras concurrentes

    fun get(): T? {
        val entry = _state.value
        return if (entry != null && !entry.isExpired) entry.data else null
    }

    suspend fun set(data: T, ttlMs: Long = 5 * 60 * 1000) {
        mutex.withLock {
            _state.value = CacheEntry(data, ttlMs = ttlMs)
        }
    }

    suspend fun invalidate() {
        mutex.withLock {
            _state.value = null
        }
    }

    fun isValid(): Boolean = _state.value != null && _state.value?.isExpired == false
}
