package com.github.javscraper.service.scraper

import com.github.javscraper.Constants.Regex.DATE
import com.github.javscraper.Constants.Regex.NUMBER_DOUBLE
import com.github.javscraper.Constants.Regex.NUMBER_INT
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.extension.*
import com.github.javscraper.service.model.JavId
import com.github.javscraper.service.model.Matcher
import com.github.javscraper.service.model.MovieIndex
import org.jsoup.nodes.TextNode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class MgsTageScraper(
    private val baseUrl: String,
    private val webClient: WebClient,
    private val retry: Retry
) : Scraper {
    override val name: String = "MgsTage"

    override fun canSearch(javId: JavId) = javId.matcher != Matcher.FC2

    override fun search(javId: JavId): Mono<List<MovieIndex>> =
        webClient.get()
            .uri(baseUrl) { it.path("/search/search.php").queryParam("search_word", javId.number).queryParam("disp_type", "detail").build() }
            .cookie("adc", "1")
            .retrieve()
            .asDocument(baseUrl)
            .retryWhen(retry)
            .map { document ->
                document.selectXpath("//div[@class='search_list']//li")
                    .map { movieBox ->
                        val detailPageUrl = movieBox.selectXpath("./a").attr("abs:href").toUriOrNull() ?: throw IllegalArgumentException("fail to parse detailPageUrl")
                        MovieIndex(
                            provider = name,
                            detailPageUrl = detailPageUrl,
                            number = detailPageUrl.path.split("/").last { it.isNotBlank() }.lowercase(),
                            title = movieBox.selectXpath("//p[contains(@class, 'title')]").text().trim(),
                            thumbUrl = movieBox.selectXpath("//h5//img").attr("abs:src").toUriOrNull()
                        )
                    }
                    .filter { it.number.isNotBlank() }
            }

    override fun getVideo(movieIndex: MovieIndex): Mono<MovieEntity> =
        webClient.get()
            .uri(movieIndex.detailPageUrl)
            .cookie("adc", "1")
            .retrieve()
            .asDocument(baseUrl)
            .retryWhen(retry)
            .map { document ->
                MovieEntity(
                    provider = name,
                    detailPageUrl = movieIndex.detailPageUrl,
                    number = document.selectXpath("//th[(contains(text(), '品番'))]/following-sibling::td").text().trim().lowercase(),
                    title = document.selectXpath("//h1[contains(@class, 'tag')]").text().trim(),
                    coverUrl = document.selectXpath("//div[contains(@class, 'detail_photo')]/h2/img").attr("abs:src").toUriOrNull(),
                    releaseDate = document.selectXpath("//th[(contains(text(), '商品発売日'))]/following-sibling::td").text().let { DATE.find(it)?.value }?.toLocalDateOrNull("yyyy/MM/dd"),
                    description = document.selectXpath("//dl[contains(@id, 'introduction')]//p[contains(@class, 'introduction')]").text(),
                    actors = document.selectXpath("//th[(contains(text(), '出演'))]/following-sibling::td//a").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    length = document.selectXpath("//th[(contains(text(), '収録時間'))]/following-sibling::td").text().let { NUMBER_INT.find(it) }?.value?.toLongOrNull().let { Duration.ofMinutes(it ?: 0L) },
                    studio = document.selectXpath("//th[(contains(text(), 'メーカー'))]/following-sibling::td//a").text().trim(),
                    label = document.selectXpath("//th[(contains(text(), 'メーカー'))]/following-sibling::td//a").text().trim(),
                    series = document.selectXpath("//th[(contains(text(), 'シリーズ'))]/following-sibling::td//a").text().trim(),
                    genres = document.selectXpath("//th[(contains(text(), 'ジャンル'))]/following-sibling::td//a").asSequence().map { it.text().trim() }.filter { it.isNotBlank() }.toSet(),
                    samples = document.selectXpath("//dl[contains(@id, 'sample-photo')]//ul//img").mapNotNullTo(mutableSetOf()) { it.attr("abs:src").toUriOrNull() },
                    communityRating = document.selectXpath<TextNode>("//th[(contains(text(), '評価'))]//following-sibling::td/span/following-sibling::text()").text().let { NUMBER_DOUBLE.find(it) }?.value?.toDoubleOrNull()?.let { it * 2 }
                )
            }
}