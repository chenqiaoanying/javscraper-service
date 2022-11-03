package com.github.javscraper

object Constants {
    object Regex {
        val NUMBER_INT = """\d+""".toRegex()
        val NUMBER_DOUBLE = """\d+(\.\d+)?""".toRegex()
        val DATE = """(?<=^|\D)\d{4}[-/]\d{2}[-/]\d{2}(?=$|\D)""".toRegex()
    }
}