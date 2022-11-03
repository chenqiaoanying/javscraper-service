package com.github.javscraper.data.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.javscraper.service.model.MovieIndex
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.net.URI
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "movie", indexes = [Index(columnList = "updateTimestamp")])
class MovieEntity(
    @JsonIgnore @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int = 0,
    @Column(length = 15) val number: String,
    @Column(length = 10) val provider: String,
    @Column val detailPageUrl: URI,
    @Lob @Column val title: String,
    @Column val coverUrl: URI?,
    @Column val releaseDate: LocalDate?,
    @Lob @Column val description: String? = null,
    @Column(length = 15) val director: String? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_actor", joinColumns = [JoinColumn(name = "movie_id", referencedColumnName = "id")])
    @Column(name = "actor_name", length = 15) val actors: Set<String> = emptySet(),
    @Column val length: Duration? = null,
    @Column(length = 30) val studio: String, // 制作组
    @Column(length = 30) val label: String, // 发行商
    @Column(length = 30) val series: String? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_genre", joinColumns = [JoinColumn(name = "movie_id", referencedColumnName = "id")])
    @Column(name = "genre", length = 20) val genres: Set<String> = emptySet(),
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_sample", joinColumns = [JoinColumn(name = "movie_id", referencedColumnName = "id")])
    @Column(name = "url") val samples: Set<URI>,
    @Column val communityRating: Double? = null,
    @UpdateTimestamp @Column val updateTimestamp: Timestamp = Timestamp.from(Instant.now())
) : Serializable {
    override fun hashCode(): Int = Objects.hash(number, provider)
    override fun equals(other: Any?): Boolean = other is MovieEntity && number == other.number && provider == other.provider

    fun toIndex() =
        MovieIndex(
            provider = provider,
            detailPageUrl = detailPageUrl,
            number = number,
            title = title,
            thumbUrl = coverUrl,
            releaseDate = releaseDate,
            movie = this
        )

    fun merge(movie: MovieEntity): MovieEntity =
        MovieEntity(
            number = number,
            provider = provider,
            detailPageUrl = detailPageUrl,
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