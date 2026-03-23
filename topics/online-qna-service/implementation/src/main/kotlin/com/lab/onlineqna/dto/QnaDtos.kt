package com.lab.onlineqna.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateQuestionRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val title: String,
    @field:NotBlank
    val content: String,
    val tags: Set<@NotBlank String> = emptySet()
)

data class UpdateQuestionRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val title: String,
    @field:NotBlank
    val content: String,
    val tags: Set<@NotBlank String> = emptySet()
)

data class CreateAnswerRequest(
    @field:NotBlank
    val content: String
)

data class UpdateAnswerRequest(
    @field:NotBlank
    val content: String
)

data class VoteRequest(
    val type: String
)

data class ReportRequest(
    @field:NotBlank
    @field:Size(max = 500)
    val reason: String
)

data class UserSummary(
    val id: Long,
    val email: String,
    val nickname: String
)

data class VoteSummary(
    val likes: Long,
    val dislikes: Long
)

data class AnswerSummary(
    val id: Long,
    val content: String,
    val accepted: Boolean,
    val author: UserSummary,
    val votes: VoteSummary,
    val createdAt: LocalDateTime
)

data class QuestionSummary(
    val id: Long,
    val title: String,
    val contentPreview: String,
    val tags: Set<String>,
    val author: UserSummary,
    val answerCount: Int,
    val acceptedAnswerId: Long?,
    val votes: VoteSummary,
    val createdAt: LocalDateTime
)

data class QuestionDetailResponse(
    val id: Long,
    val title: String,
    val content: String,
    val tags: Set<String>,
    val author: UserSummary,
    val answerCount: Int,
    val acceptedAnswerId: Long?,
    val votes: VoteSummary,
    val answers: List<AnswerSummary>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class NotificationResponse(
    val id: Long,
    val message: String,
    val referenceId: Long,
    val referenceType: String,
    val read: Boolean,
    val createdAt: LocalDateTime
)

data class UserProfileResponse(
    val user: UserSummary,
    val myQuestions: List<QuestionSummary>,
    val myAnswers: List<AnswerSummary>,
    val notifications: List<NotificationResponse>
)
