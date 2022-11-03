package com.github.javscraper.service.scraper

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javscraper.compareJson
import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.service.model.JavId
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import kotlin.reflect.KClass
import kotlin.test.BeforeTest

@SpringBootTest
@ActiveProfiles("ut")
@Execution(CONCURRENT)
class ScraperTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var scraperList: List<Scraper>

    private lateinit var scraperMap: Map<KClass<out Scraper>, Scraper>

    @BeforeTest
    fun beforeTest() {
        scraperMap = scraperList.associateBy { it::class }
    }

    private fun Scraper.getVideo(javId: JavId): Mono<MovieEntity> = search(javId).flatMap { getVideo(it.first()) }

    @Test
    fun testJavBus() {
        testScraper(JavBusScraper::class, "ofje-327")
        testScraper(JavBusScraper::class, "MIDV-119")
    }

    @Test
    fun testJav321() {
        testScraper(Jav321Scraper::class, "ofje-327")
    }

    @Test
    fun testFC2() {
        testScraper(FC2Scraper::class, "fc2-3119905")
    }

    @Test
    fun testJavdb() {
        testScraper(JavDbScraper::class, "midv-119") {
            it.communityRating shouldNotBe null
        }
    }

    @Test
    fun testMgstageScraper() {
        testScraper(MgsTageScraper::class, "abw-250")
    }

    private fun <T : Scraper> testScraper(clazz: KClass<T>, number: String, additionalTest: (MovieEntity) -> Unit = {}) {
        val scraper = scraperMap[clazz]!!
        StepVerifier.create(scraper.getVideo(JavId.from(number)!!))
            .assertNext {
                additionalTest(it)
                val expectJson = ClassLoader.getSystemResource("scraper/${scraper.name}-$number.json".lowercase()).readText()
                compareJson(expectJson, objectMapper.writeValueAsString(it), JSONCompareMode.LENIENT, "communityRating")
            }
            .verifyComplete()
    }
}