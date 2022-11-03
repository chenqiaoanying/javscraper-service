package com.github.javscraper.service

import com.github.javscraper.data.FetchResultRepository
import com.github.javscraper.data.MovieRepository
import com.github.javscraper.data.entity.FetchResult
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.data.entity.Status
import com.github.javscraper.extension.calculateLevenshteinDistanceIgnoreCase
import com.github.javscraper.extension.logger
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.MovieIndex
import com.github.javscraper.service.scraper.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.extra.math.min
import kotlin.reflect.KClass

@Service
class MovieService(
    scrapers: List<Scraper>,
    private val movieRepository: MovieRepository,
    private val fetchResultRepository: FetchResultRepository
) {
    private val logger = logger()
    private val scrapers: Map<String, Scraper>
    private val scraperComparator: Comparator<KClass<out Scraper>> =
        Comparator.comparingInt {
            when (it) {
                FC2Scraper::class, MgsTageScraper::class -> 0 //官网
                Jav321Scraper::class, JavBusScraper::class, JavDbScraper::class -> 2 // 第三方
                else -> Int.MAX_VALUE
            }
        }

    init {
        this.scrapers = scrapers.associateBy { it.name }
    }

    fun search(keyword: String): Flux<MovieIndex> = doSearch(keyword)
        .sort(Comparator.comparingInt { calculateLevenshteinDistanceIgnoreCase(it.number, keyword) })

    fun autoSearch(keyword: String): Mono<MovieEntity> =
        doSearch(keyword)
            .groupBy { it.number.lowercase() }
            .min(Comparator.comparingInt { calculateLevenshteinDistanceIgnoreCase(it.key(), keyword) })
            .flatMapMany { it }
            .flatMap { getMovie(it).onErrorComplete() }
            .collectSortListByScraper()
            .mapNotNull { it.merge() }

    fun getMovie(index: MovieIndex): Mono<MovieEntity> {
        return index.movie.toMono()
            .switchIfEmpty { movieRepository.findByNumberAndProvider(index.number.lowercase(), index.provider).toMono() }
            .switchIfEmpty {
                scrapers[index.provider]!!.getVideo(index)
                    .doOnNext { fetchResultRepository.save(FetchResult.loaded(it)) }
                    .doOnError { logger.error("Fail to get detail from index: index=$index", it) }
            }
    }

    fun getMovieAndThenMerge(movieIndex: List<MovieIndex>): Mono<MovieEntity> {
        val numberCount = movieIndex.mapTo(mutableSetOf()) { it.number.lowercase() }.size
        return if (numberCount > 1) {
            IllegalArgumentException("merge can only apply on the index with the same number").toMono()
        } else if (numberCount == 0) {
            IllegalArgumentException("no index is provided").toMono()
        } else {
            movieIndex.toFlux()
                .flatMap { getMovie(it) }
                .collectSortListByScraper()
                .mapNotNull { it.merge() }
        }
    }

    private fun doSearch(keyword: String): Flux<MovieIndex> {
        val javId = JavId.from(keyword) ?: throw IllegalArgumentException("Fail to parse keyword $keyword")
        val availableScrapers = scrapers.values.filter { it.canSearch(javId) }
        return Mono.fromCallable { fetchResultRepository.findByNumber(javId.number) }
            .map { it.ifEmpty { fetchResultRepository.findByNumberIn(javId.allPossibleNumbers) } }
            .flatMapMany { fetchResults ->
                val resultByProvider = fetchResults.associateBy { it.provider }
                availableScrapers.toFlux()
                    .flatMap { scraper ->
                        val result = resultByProvider[scraper.name]
                        if (result == null || result.status == Status.Unloaded) {
                            scraper.search(javId)
                                .doOnSuccess { indexList ->
                                    if (indexList == null || indexList.isEmpty()) {
                                        fetchResultRepository.save(FetchResult.nonexistence(javId.number, scraper.name))
                                    } else {
                                        fetchResultRepository.saveAll(indexList.map { if (it.movie == null) FetchResult.unloaded(it.number, it.provider) else FetchResult.loaded(it.movie) })
                                    }
                                }
                                .doOnError {
                                    fetchResultRepository.save(FetchResult.error(javId.number, scraper.name, it.message))
                                    logger.error("Fail to search: scraper=${scraper::class.simpleName}, javId=$javId", it)
                                }
                                .onErrorComplete()
                                .flatMapIterable { it }
                        } else {
                            result.result?.toIndex().toMono()
                        }
                    }
            }
    }

    private fun Flux<MovieEntity>.collectSortListByScraper() = this.collectSortedList(Comparator.comparing({ scrapers[it.provider]!!::class }, scraperComparator))

    private fun List<MovieEntity>.merge(): MovieEntity? {
        var result: MovieEntity? = null
        for (video in this) {
            result = result?.merge(video) ?: video
        }
        return result
    }
}