package com.github.javscraper.extension

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URL

fun WebClient.get(baseUrl: String, path: String): WebClient.ResponseSpec = get(DefaultUriBuilderFactory(baseUrl).builder().path(path).build())

fun WebClient.get(url: URL): WebClient.ResponseSpec = get(url.toURI())

fun WebClient.get(uri: URI): WebClient.ResponseSpec = get().uri(uri).retrieve()

fun WebClient.ResponseSpec.asDocument(baseUrl: String = ""): Mono<Document> = bodyToMono(String::class.java).map { Jsoup.parse(it, baseUrl) }