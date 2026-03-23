package com.lab.onlineqna.service

import com.lab.onlineqna.dto.QuestionSearchResponse
import com.lab.onlineqna.support.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchService(
    private val searchIndexService: SearchIndexService
) {

    @Transactional(readOnly = true)
    fun search(keyword: String?, tags: List<String>): List<QuestionSearchResponse> =
        searchIndexService.search(keyword, tags).map { it.toResponse() }
}
