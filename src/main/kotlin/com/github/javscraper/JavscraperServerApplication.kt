package com.github.javscraper

import com.github.javscraper.configuration.properties.HttpProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import reactor.tools.agent.ReactorDebugAgent

@SpringBootApplication
@EnableConfigurationProperties(HttpProperties::class)
@EntityScan(basePackages = ["com.github.javscraper.data.entity"])
@EnableJpaRepositories(basePackages = ["com.github.javscraper.data"])
class JavscraperServerApplication

fun main(args: Array<String>) {
    System.setProperty("reactor.netty.tcp.sslHandshakeTimeout", "30000")
    ReactorDebugAgent.init()
    runApplication<JavscraperServerApplication>(*args)
}
