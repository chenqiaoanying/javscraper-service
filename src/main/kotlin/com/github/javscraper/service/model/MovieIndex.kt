package com.github.javscraper.service.model

import com.github.javscraper.data.entity.MovieEntity
import java.net.URI
import java.time.LocalDate

data class MovieIndex(
    val provider: String,
    val detailPageUrl: URI,
    val number: String,
    val title: String,
    val thumbUrl: URI?,
    val releaseDate: LocalDate? = null,
    val movie: MovieEntity? = null,
)