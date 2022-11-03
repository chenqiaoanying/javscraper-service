package com.github.javscraper.data.entity

import com.github.javscraper.extension.toUriOrNull
import java.net.URI
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class UriConvertor : AttributeConverter<URI, String> {
    override fun convertToDatabaseColumn(attribute: URI?): String? = attribute?.toString()
    override fun convertToEntityAttribute(dbData: String?): URI? = dbData?.toUriOrNull()
}

/*
@Converter(autoApply = true)
class StringSetConvertor: AttributeConverter<Set<String>, String> {
    override fun convertToDatabaseColumn(attribute: Set<String>?): String? = attribute?.joinToString(",") { it }
    override fun convertToEntityAttribute(dbData: String?): Set<String>? = dbData?.splitToSequence(",")?.toSet()
}*/
