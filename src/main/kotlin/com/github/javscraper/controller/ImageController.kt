package com.github.javscraper.controller

import com.github.javscraper.service.ImageProxyService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono
import java.net.URL

@Controller
@RequestMapping("/image")
class ImageController(
    private val imageProxyService: ImageProxyService
) {
    @GetMapping("/proxy", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun proxy(
        @RequestParam("url") url: URL,
        @RequestParam("enableAutoCut", required = false, defaultValue = "false") enableAutoCut: Boolean,
        @RequestHeader("Accept") accept: MediaType
    ): Mono<ResponseEntity<ByteArray>> {
        return imageProxyService.getImage(url, accept.qualityValue, enableAutoCut)
            .map { ResponseEntity.status(HttpStatus.OK).contentType(MediaType.IMAGE_JPEG.copyQualityValue(accept)).body(it) }
    }
}