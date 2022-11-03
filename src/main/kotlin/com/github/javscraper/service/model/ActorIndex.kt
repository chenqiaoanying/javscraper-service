package com.github.javscraper.service.model

import com.github.javscraper.data.entity.ActorEntity
import java.net.URI
import java.time.LocalDate

data class ActorIndex(
    val provider: String,
    val detailPageUrl: URI,
    val name: String,
    val englishName: String?,
    val birthday: LocalDate?,
    val avatarUrl: URI?,
    val actor: ActorEntity? = null
)