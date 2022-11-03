package com.github.javscraper.data

import com.github.javscraper.data.entity.FetchResult
import com.github.javscraper.data.entity.FetchResultId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FetchResultRepository : JpaRepository<FetchResult, FetchResultId> {
    fun findByNumber(number: String): Collection<FetchResult>
    fun findByNumberIn(number: Collection<String>): Collection<FetchResult>
}