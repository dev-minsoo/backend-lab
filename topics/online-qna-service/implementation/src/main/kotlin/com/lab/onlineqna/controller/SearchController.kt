package com.lab.onlineqna.controller

import com.lab.onlineqna.dto.QuestionSearchResponse
import com.lab.onlineqna.service.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService
) {

    @GetMapping("/questions")
    fun searchQuestions(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) tags: List<String>?
    ): List<QuestionSearchResponse> = searchService.search(keyword, tags ?: emptyList())
}
