package com.github.javscraper.utils

import org.springframework.core.io.ClassPathResource
import java.io.File

fun getTempPathForResource(name: String): String? =
    ClassPathResource(name).inputStream
        .use {
            val file = File.createTempFile("javscraper", ".tmp")
            file.deleteOnExit()
            file.writeBytes(it.readAllBytes())
            file.absolutePath
        }