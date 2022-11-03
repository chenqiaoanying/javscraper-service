package com.github.javscraper.data.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.javscraper.service.model.ActorIndex
import org.hibernate.annotations.NaturalId
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.net.URI
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "actor", indexes = [Index(columnList = "updateTimestamp")])
class ActorEntity(
    @JsonIgnore @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Int = 0,
    @Column(length = 10) val provider: String,
    @NaturalId @Column(length = 15) val name: String,
    @NaturalId @Column val birthday: LocalDate,
    @Column(length = 25) val englishName: String?,
    @Column val detailPageUrl: URI,
    @Column var avatarUrl: URI?,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "actor_alias", joinColumns = [JoinColumn(name = "actor_id", referencedColumnName = "id")])
    @Column(name = "alias", length = 25) val aliases: Set<String>,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "actor_gallery", joinColumns = [JoinColumn(name = "actor_id", referencedColumnName = "id")])
    @Column(name = "url") val galleries: List<URI>,
    @Column(length = 4) val bloodType: String?,
    @Column(length = 1) val cupSize: Char?,
    @Column val debutDate: LocalDate?,
    @Column val height: Int?,
    @Column(length = 20) val measurements: String?,
    @Column(length = 15) val nationality: String?,
    @Column @Lob val biography: String?,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "actor_movie",
        joinColumns = [JoinColumn(name = "actor_name", referencedColumnName = "name"), JoinColumn(name = "actor_birthday", referencedColumnName = "birthday")],
        indexes = [Index(columnList = "actor_name"), Index(columnList = "movie_number")]
    )
    @JsonIgnore
    @Column(name = "movie_number") val movieNumbers: Set<String>,
    @UpdateTimestamp @Column val updateTimestamp: Timestamp = Timestamp.from(Instant.now())
) : Serializable {
    override fun hashCode(): Int = Objects.hash(name, birthday)

    override fun equals(other: Any?): Boolean = other is ActorEntity && name == other.name && birthday == other.birthday

    fun toIndex(): ActorIndex =
        ActorIndex(
            provider = provider,
            detailPageUrl = detailPageUrl,
            name = name,
            englishName = englishName,
            birthday = birthday,
            avatarUrl = avatarUrl,
            actor = this,
        )
}