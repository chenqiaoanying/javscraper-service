package com.github.javscraper.configuration

import com.github.javscraper.configuration.properties.HttpProperties
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class HttpClientConfiguration(
    private val properties: HttpProperties
) {
    @Bean
    fun webClient(clientBuilder: WebClient.Builder): WebClient =
        clientBuilder
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient
                        .create(
                            ConnectionProvider
                                .builder(properties.poolName)
                                .maxConnections(properties.maxConnections)
                                .build()
                        )
                        .proxyWithSystemProperties()
                        .secure {
                            it.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build())
                        }
                        .doOnConnected {
                            it.addHandlerLast(ReadTimeoutHandler(properties.socketTimeout.toLong(), TimeUnit.MILLISECONDS))
                            it.addHandlerLast(WriteTimeoutHandler(properties.socketTimeout.toLong(), TimeUnit.MILLISECONDS))
                        }
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectionTimeout)
                        .followRedirect(true)
                        .compress(properties.compressionEnabled)
                        //.wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
                )
            )
            .build()

    @Bean
    fun retryPolicy(): Retry =
        properties.retry
            .run {
                RetryBackoffSpec.backoff(maxAttempts, Duration.ofMillis(minBackoff))
                    .maxBackoff(Duration.ofMillis(maxBackoff))
                    .jitter(jitterFactor)
                    .filter { it is IOException || it.cause is IOException }
            }
}