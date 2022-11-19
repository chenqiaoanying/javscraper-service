package com.github.javscraper.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.net.URLEncoder
import java.time.Duration

@Service
class FileCacheService(
    @Value("\${cache_dir:/var/cache/jav}/file") fileCacheLocation: String
) {
    private val directory = File(fileCacheLocation)

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun getCache(key: String, ttl: Duration? = null): ByteArray? {
        val file = getFileByKey(key)
        return if (file.exists() && (ttl == null || Duration.ofMillis(System.currentTimeMillis() - file.lastModified()) < ttl)) file.readBytes() else null
    }

    fun getCacheFile(key: String, ttl: Duration? = null): File? {
        val file = getFileByKey(key)
        return if (file.exists() && (ttl == null || Duration.ofMillis(System.currentTimeMillis() - file.lastModified()) < ttl)) file else null
    }

    fun saveCache(key: String, byteArray: ByteArray): File {
        return getFileByKey(key).also { it.writeBytes(byteArray) }
    }

    private fun getFileByKey(key: String): File {
        val fileName = URLEncoder.encode(key, Charsets.UTF_8)

        return File("${directory.canonicalPath}${File.separator}$fileName")
    }
}