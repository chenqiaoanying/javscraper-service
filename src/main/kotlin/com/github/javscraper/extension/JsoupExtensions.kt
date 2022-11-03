package com.github.javscraper.extension

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

fun List<TextNode>.text() =
    when (size) {
        0 -> ""
        1 -> first().text()
        else -> joinToString("") { it.text() }
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Node> Element.selectXpath(xpath: String): MutableList<T> =
    this.selectXpath(xpath, T::class.java)