package com.github.javscraper.data.entity

import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import javax.persistence.*
import javax.persistence.CascadeType.*
import javax.persistence.EnumType.*

enum class Status {
    Loaded, Unloaded, Nonexistent, Error
}

class FetchResultId : Serializable {
    lateinit var number: String
    lateinit var provider: String
    override fun hashCode(): Int = Objects.hash(number, provider)
    override fun equals(other: Any?): Boolean = other is FetchResultId && number == other.number && provider == other.provider
}

@Entity
@IdClass(FetchResultId::class)
@Table(name = "fetch_result")
open class FetchResult protected constructor() : Serializable {
    @Id
    open lateinit var number: String
        protected set

    @Id
    open lateinit var provider: String
        protected set

    @Enumerated(STRING)
    @Column(length = 12)
    open lateinit var status: Status
        protected set

    @Lob
    open var message: String? = null
        protected set

    @UpdateTimestamp
    open var updateTimestamp: Timestamp = Timestamp.from(Instant.now())
        protected set

    @OneToOne(cascade = [REFRESH, PERSIST, DETACH, MERGE])
    @JoinColumn(name = "movie_id", referencedColumnName = "id")
    open var result: MovieEntity? = null
        protected set

    override fun hashCode(): Int = Objects.hash(number, provider)
    override fun equals(other: Any?): Boolean = other is FetchResultId && number == other.number && provider == other.provider

    companion object {
        fun loaded(result: MovieEntity) =
            FetchResult().apply {
                this.number = result.number
                this.provider = result.provider
                this.status = Status.Loaded
                this.result = result
            }

        fun unloaded(number: String, provider: String) =
            FetchResult().apply {
                this.number = number
                this.provider = provider
                this.status = Status.Unloaded
            }

        fun nonexistence(number: String, provider: String) =
            FetchResult().apply {
                this.number = number
                this.provider = provider
                this.status = Status.Nonexistent
            }

        fun error(number: String, provider: String, message: String?) =
            FetchResult().apply {
                this.number = number
                this.provider = provider
                this.status = Status.Error
                this.message = message
            }
    }
}