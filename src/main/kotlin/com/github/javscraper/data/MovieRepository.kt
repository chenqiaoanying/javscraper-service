package com.github.javscraper.data

import com.github.javscraper.data.entity.MovieEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MovieRepository : JpaRepository<MovieEntity, String> {
    fun findByNumber(number: String): Collection<MovieEntity>
    fun findByNumberAndProvider(number: String, provider: String): MovieEntity?
    fun findByNumberIn(number: Collection<String>): Collection<MovieEntity>

    @Query("select distinct movie.number from MovieEntity movie where :actor in movie.actors")
    fun findNumberByActorName(actor: String): Collection<String>
}