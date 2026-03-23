package com.lab.onlineqna.dto

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.time.LocalDateTime

@Document(indexName = "questions")
data class QuestionSearchDocument(
    @Id
    val id: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val authorNickname: String,
    val answerCount: Int,
    val acceptedAnswerId: Long?,
    val createdAt: LocalDateTime
)

data class QuestionSearchResponse(
    val id: Long,
    val title: String,
    val contentSnippet: String,
    val tags: List<String>,
    val authorNickname: String,
    val answerCount: Int,
    val acceptedAnswerId: Long?,
    val createdAt: LocalDateTime
)
