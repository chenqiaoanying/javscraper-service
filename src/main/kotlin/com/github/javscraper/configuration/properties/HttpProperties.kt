package com.github.javscraper.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("http")
data class HttpProperties(
    val baseUrl: UrlProperties,
    val connectionTimeout: Int,
    val socketTimeout: Int,
    val maxConnections: Int,
    val poolName: String,
    val compressionEnabled: Boolean,
    val retry: RetryConfig = RetryConfig(),
) {
    data class UrlProperties(
        val fc2: String,
        val jav321: String,
        val javbus: String,
        val javdb: String,
        val mgstage: String
    )

    data class RetryConfig(
        val maxAttempts: Long = 0,
        val minBackoff: Long = 0,
        val maxBackoff: Long = 0,
        val jitterFactor: Double = 0.0
    )
}