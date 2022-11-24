package com.github.javscraper.extension

import java.net.MalformedURLException
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ofPattern
import java.time.format.DateTimeParseException

fun levenshteinIgnoreCase(source1: String, source2: String): Int =
    levenshtein(source1.lowercase(), source2.lowercase())

fun levenshtein(source1: String, source2: String): Int {
    if (source1.length < source2.length) {
        return levenshtein(source2, source1)
    }
    val source1Length = source1.length
    val source2Length = source2.length

    // First calculation, if one entry is empty return full length
    if (source2Length == 0) {
        return source1Length
    }

    val costs = IntArray(source2Length + 1) { it }

    // Calculate rows and columns distances
    for (i in 1..source1Length) {
        var topLeftCost = costs[0]
        costs[0] = i
        for (j in 1..source2Length) {
            val match = if (source2[j - 1] == source1[i - 1]) 0 else 1
            val newCost = (topLeftCost + match)
                .coerceAtMost(costs[j] + 1)
                .coerceAtMost(costs[j - 1] + 1)
            topLeftCost = costs[j]
            costs[j] = newCost
        }
    }

    return costs[source2Length]
}

fun String.toLocalDateOrNull(dateTimeFormatter: DateTimeFormatter = ISO_DATE) =
    try {
        LocalDate.parse(this, dateTimeFormatter)
    } catch (e: DateTimeParseException) {
        null
    }

fun String.toLocalDateOrNull(pattern: String) =
    try {
        LocalDate.parse(this, ofPattern(pattern))
    } catch (e: DateTimeParseException) {
        null
    }

fun String.toUriOrNull() =
    try {
        URI(this)
    } catch (e: MalformedURLException) {
        null
    }

fun String.remove(regex: Regex) = replace(regex, "")