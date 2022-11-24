package com.github.javscraper.utils

import com.github.javscraper.extension.levenshteinIgnoreCase

// 7ABC-012
private val serialNumber_1 = "^[0-9][a-z]+[-_a-z0-9]+$".toRegex(RegexOption.IGNORE_CASE)

// ABC-00012
private val serialNumber_2 = "^(?<a>[a-z0-9]{3,5})[-_ ]+(?<b>0{1,2})(?<c>[0-9]{3,5})$".toRegex(RegexOption.IGNORE_CASE)

// midv00119
private val serialNumber_3 = "^(?<a>[a-z]{3,5})(?<b>0{1,2})(?<c>[0-9]{3,5})$".toRegex(RegexOption.IGNORE_CASE)

private fun generateAllPossibleKeywords(originKeyword: String): Set<String> {
    return if (serialNumber_1.matches(originKeyword)) {
        val keywords = (generateAllPossibleKeywords(originKeyword.substring(1)) as MutableSet<String>)
        keywords += originKeyword
        keywords
    } else {
        val keywords = mutableSetOf(originKeyword)
        (serialNumber_2.matchEntire(originKeyword) ?: serialNumber_3.matchEntire(originKeyword))
            ?.run {
                val startSegment = groups["a"]!!.value
                val middleSegment = groups["b"]!!.value
                val endSegment = groups["c"]!!.value
                for (zeroCount in 0..middleSegment.length) {
                    val zeroSegment = String(CharArray(zeroCount) { '0' })
                    keywords += "$startSegment$zeroSegment$endSegment"
                    keywords += "$startSegment-$zeroSegment$endSegment"
                    keywords += "${startSegment}_$zeroSegment$endSegment"
                }
            }
        keywords
    }
}

fun getAllPossibleKeywords(originKeyword: String): List<String> =
    generateAllPossibleKeywords(originKeyword)
        .sortedBy { levenshteinIgnoreCase(originKeyword, it) }
