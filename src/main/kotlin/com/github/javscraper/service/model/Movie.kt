package com.github.javscraper.service.model

import java.net.URI
import java.time.Duration
import java.time.LocalDate

data class Movie(
    val provider: String,
    val detailPageUrl: URI,
    val number: String,
    val title: String,
    val coverUrl: URI?,
    val releaseDate: LocalDate?,
    val description: String? = null,
    val director: String? = null,
    val actors: Set<String> = emptySet(),
    val length: Duration? = null,
    val studio: String, // 制作组
    val label: String, // 发行商
    val series: String? = null,
    val genres: Set<String> = emptySet(),
    val samples: Set<URI>,
    val communityRating: Double? = null
) {
    fun toIndex() =
        MovieIndex(
            provider = provider,
            detailPageUrl = detailPageUrl,
            number = number,
            title = title,
            thumbUrl = coverUrl,
            releaseDate = releaseDate,
        )

    fun merge(movie: Movie): Movie =
        copy(
            title = title.ifBlank { movie.title },
            coverUrl = coverUrl ?: movie.coverUrl,
            releaseDate = releaseDate ?: movie.releaseDate,
            description = description?.ifBlank { null } ?: movie.description?.ifBlank { null },
            director = director?.ifBlank { null } ?: movie.director?.ifBlank { null },
            actors = actors + movie.actors,
            length = length ?: movie.length,
            studio = studio.ifBlank { movie.studio },
            label = studio.ifBlank { movie.label },
            series = if (series.isNullOrBlank()) movie.series else series,
            genres = genres + movie.genres,
            samples = samples.ifEmpty { movie.samples },
            communityRating = communityRating ?: movie.communityRating
        )
}