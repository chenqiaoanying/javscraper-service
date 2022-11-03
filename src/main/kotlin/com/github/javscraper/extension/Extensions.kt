package com.github.javscraper.extension

import com.github.javscraper.service.model.ResponseMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.util.function.Tuple2

fun Any.logger(): Logger = LoggerFactory.getLogger(this::class.java)

operator fun <T1, T2> Tuple2<T1, T2>.component1() = t1
operator fun <T1, T2> Tuple2<T1, T2>.component2() = t2

fun <T> Mono<T>.toResponseMessage(): Mono<ResponseMessage<T>> =
    this.map { ResponseMessage.success(it) }
        .onErrorResume { ResponseMessage.error<T>(it).toMono() }
        .defaultIfEmpty(ResponseMessage.empty())