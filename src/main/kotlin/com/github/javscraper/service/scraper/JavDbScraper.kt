package com.github.javscraper.service.scraper

import com.github.javscraper.Constants.Regex.DATE
import com.github.javscraper.Constants.Regex.NUMBER_DOUBLE
import com.github.javscraper.Constants.Regex.NUMBER_INT
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.extension.*
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.MovieIndex
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class JavDbScraper(
    private val baseUrl: String,
    private val webClient: WebClient,
    private val retry: Retry
) : Scraper {

    override val name: String = "JavDB"

    override fun canSearch(javId: JavId) = true

    override fun search(javId: JavId): Mono<List<MovieIndex>> =
        webClient.get()
            .uri(baseUrl) { it.path("/search").queryParam("q", javId.number).queryParam("f", "all").build() }
            .retrieve()
            .asDocument(baseUrl)
            .retryWhen(retry)
            .map { document ->
                document.selectXpath("//a[contains(@class,'box')]")
                    .map { movieBox ->
                        MovieIndex(
                            provider = name,
                            detailPageUrl = movieBox.attr("abs:href").toUriOrNull() ?: throw IllegalArgumentException("fail to parse detailPageUrl"),
                            number = movieBox.selectXpath(".//strong").text().trim().lowercase(),
                            title = movieBox.attr("title"),
                            thumbUrl = movieBox.selectXpath(".//img").attr("abs:src").toUriOrNull(),
                            releaseDate = movieBox.selectXpath(".//div[contains(@class,'meta')]").text().toLocalDateOrNull()
                        )
                    }
                    .filter { it.number.isNotBlank() }
            }

    override fun getVideo(movieIndex: MovieIndex): Mono<MovieEntity> =
        webClient.get(movieIndex.detailPageUrl)
            .asDocument(baseUrl)
            .retryWhen(retry)
            .map { document ->
                MovieEntity(
                    provider = name,
                    detailPageUrl = movieIndex.detailPageUrl,
                    number = document.selectXpath("//strong[contains(text(),'番號')]/following-sibling::span[1]").text().trim().lowercase().ifBlank { throw IllegalArgumentException("Fail to parse number: index=$movieIndex") },
                    title = document.selectXpath("//*[contains(@class,'title')]/strong").text().remove("^\\s*?\\Q${movieIndex.number}\\E\\s*".toRegex(RegexOption.IGNORE_CASE)).trim(),
                    coverUrl = document.selectXpath("//img[contains(@class,'video-cover')]").attr("abs:src").toUriOrNull(),
                    releaseDate = document.selectXpath("//strong[contains(text(),'日期')]/following-sibling::span[1]").text().let { DATE.find(it)?.value }?.toLocalDateOrNull(),
                    director = document.selectXpath("//strong[contains(text(),'導演')]/following-sibling::span[1]").text().trim().ifBlank { null },
                    actors = document.selectXpath("//strong[contains(text(),'演員')]/following-sibling::span[1]//a").asSequence().filter { it.selectXpath("./following-sibling::strong[1]").text().trim() == "♀" }.map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    length = document.selectXpath("//strong[contains(text(),'時長')]/following-sibling::span[1]").text().let { NUMBER_INT.find(it) }?.value?.toLongOrNull().let { Duration.ofMinutes(it ?: 0L) },
                    studio = document.selectXpath("//strong[contains(text(),'片商')]/following-sibling::span[1]").text().trim(),
                    label = document.selectXpath("//strong[contains(text(),'片商')]/following-sibling::span[1]").text().trim(),
                    series = document.selectXpath("//strong[contains(text(),'系列')]/following-sibling::span[1]").text().trim(),
                    genres = document.selectXpath("//strong[contains(text(),'類別')]/following-sibling::span[1]//a").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    samples = document.selectXpath("//div[contains(@class,'preview-images')]//img").mapNotNullTo(mutableSetOf()) { it.attr("abs:src").toUriOrNull() },
                    communityRating = document.selectXpath("//strong[contains(text(),'評分')]/following-sibling::span[1]").text().let { NUMBER_DOUBLE.find(it) }?.value?.toDoubleOrNull()?.let { it * 2 }
                )
            }
}