package com.github.javscraper.service.scraper

import com.github.javscraper.Constants.Regex.DATE
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.extension.asDocument
import com.github.javscraper.extension.get
import com.github.javscraper.extension.toLocalDateOrNull
import com.github.javscraper.extension.toUriOrNull
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.Matcher
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.net.URI
import java.time.format.DateTimeFormatter

class FC2Scraper(
    private val baseUrl: String,
    private val webClient: WebClient,
    private val retry: Retry
) : DirectReachScraper() {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    override val name: String = "FC2"

    override fun canSearch(javId: JavId) = javId.matcher == Matcher.FC2

    override fun directReach(javId: JavId): Mono<MovieEntity> =
        webClient.get(baseUrl, "/article/${javId.id}/")
            .asDocument(baseUrl)
            .retryWhen(retry)
            .filter { it.selectXpath("//div[contains(@class,'items_notfound')]").isEmpty() }
            .map { document ->
                MovieEntity(
                    provider = name,
                    detailPageUrl = URI("$baseUrl/article/${javId.id}"),
                    number = javId.number.lowercase(),
                    title = document.selectXpath("//meta[@name='twitter:title']").attr("content").trim(),
                    coverUrl = document.selectXpath("//meta[@property='og:image']").attr("abs:content").trim()
                        .ifBlank { document.selectXpath("//h3[1]/../..//*[@id='video-player']").attr("abs:poster") }
                        .ifBlank { document.selectXpath("//h3[1]/../..//img[@class='img-responsive']").attr("abs:src") }
                        .toUriOrNull(),
                    releaseDate = document.selectXpath("//div[@class='items_article_Releasedate']").text().let { DATE.find(it) }?.value?.toLocalDateOrNull(dateFormatter),
                    series = "FC2",
                    studio = document.selectXpath("//section[@class='items_comment_sellerBox']//h4").text().trim(),
                    label = document.selectXpath("//section[@class='items_comment_sellerBox']//h4").text().trim(),
                    samples = document.selectXpath("//a[contains(@data-image-slideshow,'sample-images')]").mapNotNullTo(mutableSetOf()) { it.attr("abs:href").toUriOrNull() },
                    genres = document.selectXpath("//a[contains(@class, 'tag')]").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet()
                )
            }
}