package com.github.javscraper.service.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.net.URI
import java.time.LocalDate

data class Actor(
    val provider: String,
    val name: String,
    val birthday: LocalDate,
    val detailPageUrl: URI,
    val avatarUrl: URI?,
    val aliases: Set<String>,
    val galleries: List<URI>,
    val bloodType: String?,
    val cupSize: Char?,
    val debutDate: LocalDate?,
    val height: Int?,
    val measurements: String?,
    val nationality: String?,
    val biography: String?,
    @JsonIgnore val movieList: List<Movie>
) : Serializable {
    data class Movie(val number: String, val title: String, val releaseDate: LocalDate?)
}