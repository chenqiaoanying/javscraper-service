package com.github.javscraper.service

import com.github.javscraper.Constants
import com.github.javscraper.data.entity.ActorEntity
import com.github.javscraper.extension.*
import com.github.javscraper.service.model.ActorIndex
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class XslistRepository(
    @Value("\${http.base-url.xslist}")
    private val baseUrl: String,
    private val webClient: WebClient,
    private val retryPolicy: Retry,
) {
    private val providerName = "Xslist"
    private val dateFormatter1 = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    private val dateFormatter2 = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private val datePattern2 = """\d{2}/\d{2}/\d{4}""".toRegex()
    private val namePattern = """^\s*?(?<english>[a-z]+[a-z\s]*?)\s*?-\s*?(?<name>\S+[\s\S]*?)\s*?$""".toRegex(RegexOption.IGNORE_CASE)

    fun search(name: String): Flux<ActorIndex> =
        webClient.get().uri(baseUrl) { it.path("/search").queryParam("query", name).queryParam("lg", "zh").build() }
            .retrieve()
            .asDocument(baseUrl)
            .retryWhen(retryPolicy)
            .flatMapIterable { parseIndexesFromDocument(it) }

    fun getDetail(index: ActorIndex): Mono<ActorEntity> =
        webClient.get().uri(index.detailPageUrl).retrieve().asDocument(baseUrl)
            .retryWhen(retryPolicy)
            .map { parseDetailFromDocument(index, it) }

    private fun parseIndexesFromDocument(document: Document): List<ActorIndex> =
        document.selectXpath("//li")
            .map { resultNode ->
                val name = resultNode.selectXpath(".//a").text()
                val nameMathResult = namePattern.find(name)
                ActorIndex(
                    provider = providerName,
                    detailPageUrl = resultNode.selectXpath(".//a").attr("abs:href").toUriOrNull() ?: throw IllegalArgumentException("fail to parse detailPageUrl"),
                    name = nameMathResult?.groups?.get("name")?.value ?: name,
                    englishName = nameMathResult?.groups?.get("english")?.value,
                    birthday = document.selectXpath(".//p").text().let { datePattern2.find(it) }?.value?.toLocalDateOrNull(dateFormatter2),
                    avatarUrl = resultNode.selectXpath(".//img").attr("abs:src").toUriOrNull()
                )
            }

    private fun parseDetailFromDocument(index: ActorIndex, document: Document): ActorEntity {
        val dictionary = document.selectXpath("//*[@itemprop='nationality']/..")
            .ifEmpty { document.selectXpath("//p[contains(text(),'出生:')]") }
            .textNodes()
            .asSequence()
            .map { line -> line.text().splitToSequence(":", "：").filter { it.isNotBlank() }.map { it.trim() }.toList() }
            .filter { it.count() == 2 }
            .associate { it[0] to it[1] }

        return ActorEntity(
            provider = providerName,
            detailPageUrl = index.detailPageUrl,
            avatarUrl = index.avatarUrl,
            name = document.selectXpath("//span[@itemprop='name']").text().trim(),
            englishName = index.englishName,
            aliases = document.selectXpath("//span[@itemprop='additionalName']").asSequence().map { it.text() }.filter { it.isNotBlank() }.toSet(),
            galleries = document.selectXpath("//div[@id='gallery']/a").mapNotNull { it.attr("abs:href").toUriOrNull() }.filter { it.toString().substringAfterLast('/').contains('.') },
            birthday = dictionary["出生"]?.toLocalDateOrNull(dateFormatter1) ?: LocalDate.MIN,
            bloodType = dictionary["血型"],
            cupSize = dictionary["罩杯"]?.firstOrNull(),
            debutDate = dictionary["出道日期"]?.let { if (it.length == 8) it + "01日" else it }?.toLocalDateOrNull(dateFormatter1),
            height = document.selectXpath("//*[@itemprop='height']").text().let { Constants.Regex.NUMBER_INT.find(it) }?.value?.toInt(),
            measurements = dictionary["三围"],
            nationality = document.selectXpath("//*[@itemprop='nationality']").text(),
            biography = document.selectXpath<TextNode>("//p[contains(text(),'简介:')]/text()[last()]").text().trim(),
            movieNumbers = document.selectXpath("//table[@id='movices']//tbody//tr").asSequence()
                .map { rawNode -> rawNode.selectXpath("./td[1]").text().trim().lowercase() }
                .toSet()
        )
    }
}