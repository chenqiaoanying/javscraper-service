package com.github.javscraper.configuration

import com.github.javscraper.configuration.properties.HttpProperties
import com.github.javscraper.service.scraper.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import reactor.util.retry.Retry

@Configuration
class ScraperConfiguration(
    private val httpProperties: HttpProperties,
    private val webClient: WebClient,
    private val retryPolicy: Retry
) {
    @Bean
    fun javBusScraper(): Scraper =
        JavBusScraper(httpProperties.baseUrl.javbus, webClient, retryPolicy)

    @Bean
    fun jav321Scraper(): Scraper =
        Jav321Scraper(httpProperties.baseUrl.jav321, webClient, retryPolicy)

    @Bean
    fun fc2Scraper(): Scraper =
        FC2Scraper(httpProperties.baseUrl.fc2, webClient, retryPolicy)

    @Bean
    fun javdbScraper(): Scraper =
        JavDbScraper(httpProperties.baseUrl.javdb, webClient, retryPolicy)

    @Bean
    fun mgstageScraper(): Scraper =
        MgsTageScraper(httpProperties.baseUrl.mgstage, webClient, retryPolicy)
}