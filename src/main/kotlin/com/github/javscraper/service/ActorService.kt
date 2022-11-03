package com.github.javscraper.service

import com.github.javscraper.data.ActorRepository
import com.github.javscraper.data.entity.ActorEntity
import com.github.javscraper.extension.calculateLevenshteinDistance
import com.github.javscraper.extension.calculateLevenshteinDistanceIgnoreCase
import com.github.javscraper.service.model.ActorIndex
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.extra.math.min
import java.net.URI

@Service
class ActorService(
    private val avatarService: GfriendAvatarService,
    private val xslistRepository: XslistRepository,
    private val actorRepository: ActorRepository
) {
    fun autoSearch(name: String, number: String? = null): Mono<ActorEntity> {
        val possibleNameList = allPossibleName(name).toList()
        return possibleNameList.toFlux()
            .flatMap { doSearch(it) }
            .switchIfEmpty(if (number.isNullOrBlank()) Flux.empty() else doSearch(number))
            .distinct { it.name }
            .flatMap { getDetail(it) }
            .collectSortedList(Comparator.comparingInt { calculateNameDistance(it, possibleNameList) })
            .mapNotNull { actors ->
                if (number.isNullOrBlank()) {
                    actors.firstOrNull()
                } else {
                    actors.firstOrNull { detail -> detail.movieNumbers.contains(number.lowercase()) } ?: actors.firstOrNull()
                }
            }
    }

    fun search(name: String): Flux<ActorIndex> =
        allPossibleName(name).toFlux().flatMap { doSearch(it) }

    fun searchAvatarByName(names: Set<String>): Mono<Map<String, URI>> =
        names.toFlux()
            .flatMap { name ->
                avatarService.findUriByName(name)
                    .switchIfEmpty {
                        doSearch(name).min(Comparator.comparingInt { calculateLevenshteinDistance(it.name, name) })
                            .mapNotNull<URI> { it.avatarUrl }
                    }
                    .map { name to it }
            }
            .collectList()
            .map { it.toMap() }

    private fun allPossibleName(name: String) =
        name.splitToSequence(*"()（）、，".toCharArray())
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun doSearch(name: String): Flux<ActorIndex> =
        actorRepository.findByName(name).map { it.toIndex() }.toFlux()
            .switchIfEmpty(xslistRepository.search(name))

    fun getDetail(index: ActorIndex): Mono<ActorEntity> =
        index.actor.toMono()
            .switchIfEmpty { actorRepository.findByDetailPageUrl(index.detailPageUrl).toMono() }
            .switchIfEmpty { index.birthday?.let { actorRepository.findByNameAndBirthday(index.name, index.birthday) }.toMono() }
            .switchIfEmpty { xslistRepository.getDetail(index).doOnNext { actorRepository.save(it) } }
            .flatMap { actor ->
                avatarService.findUriByName(actor.name)
                    .map { actor.apply { avatarUrl = it } }
                    .defaultIfEmpty(actor)
            }

    private fun calculateNameDistance(actor: ActorEntity, allPossibleName: List<String>): Int =
        (actor.aliases + actor.name).minOf { alias -> allPossibleName.minOf { possibleName -> calculateLevenshteinDistanceIgnoreCase(alias, possibleName) } }
}
