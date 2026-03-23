package com.lab.onlineqna.service

import com.lab.onlineqna.dto.QuestionSearchDocument

interface SearchIndexService {
    fun upsert(document: QuestionSearchDocument)
    fun delete(questionId: Long)
    fun search(keyword: String?, tags: List<String>): List<QuestionSearchDocument>
}
