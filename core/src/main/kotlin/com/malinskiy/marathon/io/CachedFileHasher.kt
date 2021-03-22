package com.malinskiy.marathon.io

import org.apache.commons.collections4.map.LRUMap
import java.io.File
import java.util.*

class CachedFileHasher(
    private val delegate: FileHasher,
    cacheCapacity: Int = DEFAULT_CACHE_CAPACITY
) : FileHasher {

    private val cache = Collections.synchronizedMap(LRUMap<File, String>(cacheCapacity))

    override suspend fun getHash(file: File): String =
        cache.getOrPut(file) { delegate.getHash(file) }

    private companion object {
        private const val DEFAULT_CACHE_CAPACITY = 512
    }
}
