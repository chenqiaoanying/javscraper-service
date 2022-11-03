package com.github.javscraper.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javscraper.extension.logger
import com.github.javscraper.extension.toUriOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.abs

@Service
class GfriendAvatarService(
    private val webClient: WebClient,
    private val fileCacheService: FileCacheService,
    private val objectMapper: ObjectMapper,
    private val retryPolicy: Retry
) {
    private val baseUrl = "https://cdn.jsdelivr.net/gh/xinxin8816/gfriends"
    private val logger = logger()
    private var lastUpdateTimestamp: Long = 0
    private lateinit var fileNodeTree: Map<String, Map<String, List<FileNode>>>
    private val initMutex = Mutex()
    private val ttl = Duration.ofDays(1)
    private val cacheKey = "gfriends.json"
    private var syncing = false

    fun findUriByName(name: String): Mono<URI> = mono(Dispatchers.Default) {
        if (!this@GfriendAvatarService::fileNodeTree.isInitialized) {
            initMutex.withLock {
                if (!this@GfriendAvatarService::fileNodeTree.isInitialized) {
                    val cacheFile = fileCacheService.getCacheFile(cacheKey)
                    val fileTree = if (cacheFile != null) {
                        objectMapper.readValue(cacheFile, FileTree::class.java)
                            .also { lastUpdateTimestamp = cacheFile.lastModified() }
                    } else {
                        val content = webClient.get()
                            .uri(baseUrl) { it.path("/Filetree.json").build() }
                            .retrieve()
                            .bodyToMono(ByteArray::class.java)
                            .retryWhen(retryPolicy)
                            .doOnNext { fileCacheService.saveCache(cacheKey, it) }
                            .doOnError { logger.error("Fail to get file tree from remote", it) }
                            .onErrorComplete()
                            .awaitFirstOrNull()
                            ?: throw IllegalStateException("Fail to initial file tree")

                        objectMapper.readValue(content, FileTree::class.java)
                            .also { lastUpdateTimestamp = System.currentTimeMillis() }
                    }
                    fileNodeTree = fileTree.convert()
                }
            }
        }
        if (Duration.ofMillis(abs(System.currentTimeMillis() - lastUpdateTimestamp)) > ttl) {
            if (!syncing) {
                syncing = true
                logger.info("syncing file tree from remote")
                webClient.get()
                    .uri(baseUrl) { it.path("/Filetree.json").build() }
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .retryWhen(retryPolicy)
                    .doOnNext { content ->
                        fileCacheService.saveCache(cacheKey, content)
                        fileNodeTree = objectMapper.readValue(content, FileTree::class.java).convert()
                        lastUpdateTimestamp = System.currentTimeMillis()
                        logger.info("success to sync file tree from remote")
                    }
                    .doOnError { logger.error("Fail to get file tree from remote", it) }
                    .doFinally { syncing = false }
                    .subscribe()
            }
        }

        fileNodeTree[name]?.entries?.asSequence()
            ?.flatMap { (company, files) -> files.asSequence().map { file -> company to file } }
            ?.maxByOrNull { (_, file) -> file.timestamp }
            ?.let { (company, file) ->
                "$baseUrl/Content/$company/${file.path}".toUriOrNull()
            }
    }

    fun FileTree.convert() =
        content.asSequence()
            .flatMap { (company, actors) ->
                actors.asSequence()
                    .map { (actor, path) -> arrayOf(company, actor, path) }
            }
            .let { rows ->
                rows.fold(TreeMap<String, MutableMap<String, MutableList<FileNode>>>()) { map, (company, actor, path) ->
                    map.computeIfAbsent(actor.substringBeforeLast('.')) { TreeMap() }
                        .computeIfAbsent(company) { ArrayList(2) }
                        .add(FileNode(path, Timestamp.from(Instant.ofEpochSecond(path.substringAfterLast('=').toLong()))))
                    map
                }
            }

    data class FileTree(
        @JsonProperty("Content")
        val content: Map<String, Map<String, String>>
    )

    data class FileNode(val path: String, val timestamp: Timestamp)
}