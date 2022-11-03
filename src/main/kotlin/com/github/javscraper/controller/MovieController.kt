package com.github.javscraper.controller

import com.github.javscraper.data.entity.MovieEntity
import com.github.javscraper.extension.toResponseMessage
import com.github.javscraper.service.MovieService
import com.github.javscraper.service.model.MovieIndex
import com.github.javscraper.service.model.ResponseMessage
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/movie")
class MovieController(private val movieService: MovieService) {
    @GetMapping("/auto/search")
    fun autoSearchMovie(@RequestParam("keyword") keyword: String): Mono<ResponseMessage<MovieEntity>> =
        movieService.autoSearch(keyword).toResponseMessage()

    @GetMapping("/search")
    fun searchMovie(@RequestParam("keyword") keyword: String): Mono<ResponseMessage<List<MovieIndex>>> =
        movieService.search(keyword).collectList().toResponseMessage()

    @PostMapping("/get")
    fun getMovieByIndexAndMerge(@RequestBody index: MovieIndex): Mono<ResponseMessage<MovieEntity>> =
        movieService.getMovie(index).toResponseMessage()

    @PostMapping("/get/merge")
    fun getMovieByIndexAndMerge(@RequestBody indexes: List<MovieIndex>): Mono<ResponseMessage<MovieEntity>> =
        movieService.getMovieAndThenMerge(indexes).toResponseMessage()
}