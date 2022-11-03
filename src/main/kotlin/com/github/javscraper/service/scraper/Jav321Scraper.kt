package com.github.javscraper.service.scraper

import com.github.javscraper.Constants.Regex.NUMBER_INT
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.extension.*
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.Matcher
import org.jsoup.nodes.TextNode
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class Jav321Scraper(
    private val baseUrl: String,
    private val webClient: WebClient,
    private val retry: Retry
) : DirectReachScraper() {
    override val name: String = "Jav321"

    override fun canSearch(javId: JavId) = javId.matcher != Matcher.FC2

    override fun directReach(javId: JavId): Mono<MovieEntity> =
        webClient.post()
            .uri(baseUrl) { it.path("/search").build() }
            .body(BodyInserters.fromFormData("sn", javId.number))
            .retrieve()
            .asDocument(baseUrl)
            .retryWhen(retry)
            .filter { it.selectXpath("//h3").isNotEmpty() }
            .map { document ->
                val samples = document.selectXpath("//a[contains(@href,'snapshot')]/img").mapNotNullTo(mutableSetOf()) { it.attr("abs:src").toUriOrNull() }
                MovieEntity(
                    provider = name,
                    detailPageUrl = document.selectXpath("//li/a[contains(text(),'简体中文')]").attr("abs:href").toUriOrNull() ?: throw IllegalArgumentException("fail to parse detailPageUrl"),
                    number = document.selectXpath<TextNode>("//b[contains(text(),'品番')]/following::text()[1]").text().removePrefix(": ").lowercase().ifBlank { throw IllegalArgumentException("fail to parse serialNumber") },
                    title = document.selectXpath<TextNode>("//h3[1]/text()[1]").text(),
                    coverUrl = samples.first(),
                    releaseDate = document.selectXpath<TextNode>("//b[contains(text(),'配信開始日')]/following::text()[1]").text().removePrefix(": ").trim().toLocalDateOrNull(),
                    actors = document.selectXpath("//b[contains(text(),'出演者')]/following-sibling::a[contains(@href, 'star')]").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    length = document.selectXpath<TextNode>("//b[contains(text(),'収録時間')]/following::text()[1]").text().removePrefix(": ")
                        .let { NUMBER_INT.find(it) }?.value?.toLongOrNull().let { Duration.ofMinutes(it ?: 0) },
                    studio = document.selectXpath("//b[contains(text(),'メーカー')]/following-sibling::a[1]").text().removePrefix(": ").trim(),
                    label = document.selectXpath("//b[contains(text(),'メーカー')]/following-sibling::a[1]").text().removePrefix(": ").trim(),
                    samples = samples,
                    description = document.selectXpath<TextNode>("//h3[1]/../..//div[@class='row'][last()]/div/text()[1]").text(),
                    communityRating = document.selectXpath("//b[contains(text(),'平均評価')]/following-sibling::img").attr("data-original")
                        .let { NUMBER_INT.find(it) }?.value?.toDoubleOrNull()?.let { it / 50 * 10 }
                )
            }
}