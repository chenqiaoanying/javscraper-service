package com.github.javscraper.service.scraper

import com.github.javscraper.Constants.Regex.DATE
import com.github.javscraper.Constants.Regex.NUMBER_INT
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.extension.*
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.Matcher
import com.github.javscraper.service.model.MovieIndex
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.util.retry.Retry
import java.time.Duration

class JavBusScraper(
    private val baseUrl: String,
    private val webClient: WebClient,
    private val retry: Retry
) : Scraper {

    override val name: String = "JavBus"

    override fun canSearch(javId: JavId) = javId.matcher != Matcher.FC2

    override fun search(javId: JavId): Mono<List<MovieIndex>> =
        webClient.get(baseUrl, "/search/${javId.number}&type=1&parent=ce")
            .onStatus({ it.is4xxClientError }) { Mono.empty() }
            .asDocument(baseUrl)
            .retryWhen(retry)
            .flatMap { document ->
                // 判断是否有 无码的影片
                val uncensoredFilmAmount = document.selectXpath<TextNode>("//a[contains(@href,'/uncensored/search/')]//span[contains(@class,'film')]/following::text()").text().let { NUMBER_INT.find(it) }?.value?.toIntOrNull() ?: 0
                if (uncensoredFilmAmount == 0) {
                    parseIndex(document).toMono()
                } else {
                    webClient.get(baseUrl, "/uncensored/search/${javId.number}&type=1").asDocument(baseUrl)
                        .map { parseIndex(document) + parseIndex(it) }
                }
            }
            .map { it.toList() }

    private fun parseIndex(document: Document): Sequence<MovieIndex> =
        document.selectXpath("//a[@class='movie-box']").asSequence()
            .map { movieBox ->
                val imgNode = movieBox.selectXpath(".//div[@class='photo-frame']//img")
                MovieIndex(
                    provider = name,
                    detailPageUrl = movieBox.attr("abs:href").toUriOrNull() ?: throw IllegalArgumentException("fail to parse detailPageUrl"),
                    number = movieBox.selectXpath(".//date[1]").text().trim().lowercase(),
                    title = imgNode.attr("title"),
                    thumbUrl = imgNode.attr("abs:src").ifBlank { null }?.toUriOrNull(),
                    releaseDate = movieBox.selectXpath(".//date[2]").text().toLocalDateOrNull()
                )
            }
            .filter { it.number.isNotBlank() }

    override fun getVideo(movieIndex: MovieIndex): Mono<MovieEntity> =
        webClient.get(movieIndex.detailPageUrl)
            .asDocument(baseUrl)
            .retryWhen(retry)
            .map { document ->
                MovieEntity(
                    provider = name,
                    detailPageUrl = movieIndex.detailPageUrl,
                    number = document.selectXpath("//span[contains(text(),'識別碼')]/following::span[1]").text().trim().lowercase(),
                    title = document.selectXpath("//h3").text().trim().remove("^\\s*?\\Q${movieIndex.number}\\E\\s*".toRegex(RegexOption.IGNORE_CASE)).trim(),
                    coverUrl = document.selectXpath("//a[@class='bigImage']").attr("abs:href").toUriOrNull(),
                    releaseDate = document.selectXpath<TextNode>("//span[contains(text(),'發行日期')]/following::text()[1]").text().let { DATE.find(it)?.value }?.toLocalDateOrNull(),
                    director = document.selectXpath("//span[contains(text(),'導演')]/following-sibling::a[1]").text().trim().ifBlank { null },
                    actors = document.selectXpath("//div[@class='star-name']").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    length = document.selectXpath<TextNode>("//span[contains(text(),'長度')]/following::text()[1]").text().let { NUMBER_INT.find(it) }?.value?.toLongOrNull().let { Duration.ofMinutes(it ?: 0L) },
                    studio = document.selectXpath("//span[contains(text(),'製作商')]/following::a[1]").text().trim(),
                    label = document.selectXpath("//span[contains(text(),'發行商')]/following::a[1]").text().trim(),
                    series = document.selectXpath("//span[contains(text(),'系列')]/following::a[1]").text().trim(),
                    genres = document.selectXpath("//span[@class='genre']/label").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    samples = document.selectXpath("//a[@class='sample-box']//img").mapNotNullTo(mutableSetOf()) { it.attr("abs:src").toUriOrNull() }
                )
            }
}