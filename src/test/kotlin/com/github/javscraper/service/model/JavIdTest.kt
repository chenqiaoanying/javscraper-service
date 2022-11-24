package com.github.javscraper.service.model

import com.github.javscraper.extension.levenshteinIgnoreCase
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JavIdTest {
    @Test
    fun test() {
        JavId.from("hhd800.com@300MAAN-735") shouldBe JavId("300maan-735", "300maan-735", JavIdType.Suren, Matcher.Suren)
        JavId.from("rctd-460-ch.mp4") shouldBe JavId("rctd-460", "rctd-460", JavIdType.Censored, Matcher.Censored)
        JavId.from("FC2-3119905") shouldBe JavId("fc2-3119905", "3119905", JavIdType.Suren, Matcher.FC2)
    }

    @Test
    fun testLevenshteinDistance() {
        levenshteinIgnoreCase("", "") shouldBe 0
        levenshteinIgnoreCase("abc", "") shouldBe 3
        levenshteinIgnoreCase("", "abc") shouldBe 3
        levenshteinIgnoreCase("abc", "bcd") shouldBe 2
        levenshteinIgnoreCase("abc", "ABC") shouldBe 0
        levenshteinIgnoreCase("ABC", "DEF") shouldBe 3
        levenshteinIgnoreCase("ABC", "ABCD") shouldBe 1
        levenshteinIgnoreCase("BCD", "ABCD") shouldBe 1
        levenshteinIgnoreCase("ABCD", "ABC") shouldBe 1
        levenshteinIgnoreCase("cba", "ABC") shouldBe 2
        levenshteinIgnoreCase("ABW-286", "ABW-286-uncensored-nyap2p.com") shouldBe 22
    }

    @Test
    fun testAllPossibleKey() {
        JavId.from("heyzo 0999")!!.id shouldBe "0999"
        JavId.from("midv00119")!!.allPossibleNumbers shouldBe listOf("midv00119", "midv0119", "midv-0119", "midv_0119", "midv-00119", "midv_00119", "midv119", "midv-119", "midv_119")
        JavId.from("midv-119")!!.allPossibleNumbers shouldBe listOf("midv-119")
        JavId.from("3xplanet_heyzo_hd_0474_full")!!.id shouldBe "0474"
        JavId.from("Tokyo Hot n9001 FHD.mp4")!!.allPossibleNumbers shouldBe listOf("n9001")
    }
}