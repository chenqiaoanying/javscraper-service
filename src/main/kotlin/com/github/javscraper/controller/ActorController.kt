package com.github.javscraper.controller

import com.github.javscraper.data.entity.ActorEntity
import com.github.javscraper.extension.toResponseMessage
import com.github.javscraper.service.ActorService
import com.github.javscraper.service.model.ActorIndex
import com.github.javscraper.service.model.ResponseMessage
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.net.URI

@RestController
@RequestMapping("/actor")
class ActorController(private val actorService: ActorService) {
    @GetMapping("/auto/search")
    fun autoSearchActor(@RequestParam("name") name: String, @RequestParam("number") number: String?): Mono<ResponseMessage<ActorEntity>> =
        actorService.autoSearch(name, number).toResponseMessage()

    @GetMapping("/search")
    fun searchActor(@RequestParam("name") name: String): Mono<ResponseMessage<List<ActorIndex>>> =
        actorService.search(name).collectList().toResponseMessage()

    @PostMapping("/get")
    fun searchActor(@RequestBody index: ActorIndex): Mono<ResponseMessage<ActorEntity>> =
        actorService.getDetail(index).toResponseMessage()

    @GetMapping("/avatar/search")
    fun searchAvatarByName(@RequestParam("names") names: Set<String>): Mono<ResponseMessage<Map<String, URI>>> =
        actorService.searchAvatarByName(names).toResponseMessage()
}