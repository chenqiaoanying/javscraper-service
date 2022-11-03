package com.github.javscraper.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier
import kotlin.test.Test

@SpringBootTest
@ActiveProfiles("ut")
class XslistRepositoryTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var xslistRepository: XslistRepository

    @Test
    fun testGetActor() {
        StepVerifier.create(xslistRepository.search("石川澪").single().flatMap { xslistRepository.getDetail(it) })
            .assertNext {
                val expectJson = """
                    {
                        "provider": "Xslist",
                        "detailPageUrl": "https://xslist.org/zh/model/139994.html",
                        "avatarUrl": "https://xslist.org/kojav/model2/139000/139994.jpg",
                        "name": "石川澪",
                        "aliases": [],
                        "galleries": [
                            "https://xslist.org/kojav/model2/139000/139994.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1645769464.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1639540402.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1635484385.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1633398613.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1666402613.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1666400418.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1666400216.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1666399613.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1664510270.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1659593969.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1659583907.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1657083541.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1656990642.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1656985245.jpg",
                            "https://m1.xslist.org/gallery/139000/139994/1654656384.jpg"
                        ],
                        "birthday": "2002-03-29",
                        "bloodType": "n/a",
                        "cupSize": "C",
                        "debutDate": "2021-09-01",
                        "height": 158,
                        "measurements": "B82 / W58 / H86",
                        "nationality": "日本",
                        "biography": "暂无关于石川澪(Mio Ishikawa/20岁)的介绍。"
                    }
                """.trimIndent()
                JSONAssert.assertEquals(expectJson, objectMapper.writeValueAsString(it), JSONCompareMode.LENIENT)
            }
            .verifyComplete()
    }
}