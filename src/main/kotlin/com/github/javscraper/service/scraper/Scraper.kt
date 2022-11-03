package com.github.javscraper.service.scraper

import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.MovieIndex
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

interface Scraper {
    val name: String

    fun canSearch(javId: JavId): Boolean

    fun search(javId: JavId): Mono<List<MovieIndex>>

    fun getVideo(movieIndex: MovieIndex): Mono<MovieEntity>
}

abstract class SearchableScraper : Scraper {
    override fun search(javId: JavId): Mono<List<MovieIndex>> = mono {
        val possibleKeys = javId.allPossibleNumbers
        for (key in possibleKeys) {
            val videoIndexList = searchByKeyword(key).awaitFirst()
            if (videoIndexList.isNotEmpty()) {
                return@mono videoIndexList
            }
        }
        null
    }

    protected abstract fun searchByKeyword(keyword: String): Mono<List<MovieIndex>>
}

abstract class DirectReachScraper : Scraper {
    override fun search(javId: JavId): Mono<List<MovieIndex>> =
        directReach(javId)
            .map { listOf(it.toIndex()) }

    override fun getVideo(movieIndex: MovieIndex): Mono<MovieEntity> {
        return if (movieIndex.movie != null) {
            movieIndex.movie.toMono()
        } else {
            directReach(JavId.from(movieIndex.number) ?: throw IllegalArgumentException("Fail to parse number: ${movieIndex.number}"))
        }
    }

    abstract fun directReach(javId: JavId): Mono<MovieEntity>
}