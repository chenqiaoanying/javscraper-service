package com.github.javscraper.data

import com.github.javscraper.data.entity.ActorEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.net.URI
import java.time.LocalDate

@Repository
interface ActorRepository : JpaRepository<ActorEntity, Int> {
    fun findByName(name: String): List<ActorEntity>
    fun findByNameAndBirthday(name: String, birthday: LocalDate): ActorEntity?
    fun findByDetailPageUrl(detailPageUrl: URI): ActorEntity?
}