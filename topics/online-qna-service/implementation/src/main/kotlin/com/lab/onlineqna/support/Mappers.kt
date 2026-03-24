package com.lab.onlineqna.support

import com.lab.onlineqna.domain.Answer
import com.lab.onlineqna.domain.Notification
import com.lab.onlineqna.domain.Question
import com.lab.onlineqna.domain.User
import com.lab.onlineqna.dto.AnswerSummary
import com.lab.onlineqna.dto.NotificationResponse
import com.lab.onlineqna.dto.QuestionDetailResponse
import com.lab.onlineqna.dto.QuestionSearchDocument
import com.lab.onlineqna.dto.QuestionSearchResponse
import com.lab.onlineqna.dto.QuestionSummary
import com.lab.onlineqna.dto.UserSummary
import com.lab.onlineqna.dto.VoteSummary
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun User.toSummary(): UserSummary = UserSummary(
    id = id!!,
    email = email,
    nickname = nickname
)

fun Question.toSummary(votes: VoteSummary): QuestionSummary = QuestionSummary(
    id = id!!,
    title = title,
    contentPreview = content.take(120),
    tags = tags.map { it.name }.toSet(),
    author = author.toSummary(),
    answerCount = answerCount,
    acceptedAnswerId = acceptedAnswerId,
    votes = votes,
    createdAt = createdAt
)

fun Answer.toSummary(votes: VoteSummary): AnswerSummary = AnswerSummary(
    id = id!!,
    content = content,
    accepted = accepted,
    author = author.toSummary(),
    votes = votes,
    createdAt = createdAt
)

fun Question.toDetail(votes: VoteSummary, answers: List<AnswerSummary>): QuestionDetailResponse = QuestionDetailResponse(
    id = id!!,
    title = title,
    content = content,
    tags = tags.map { it.name }.toSet(),
    author = author.toSummary(),
    answerCount = answerCount,
    acceptedAnswerId = acceptedAnswerId,
    votes = votes,
    answers = answers,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Notification.toResponse(): NotificationResponse = NotificationResponse(
    id = id!!,
    message = message,
    referenceId = referenceId,
    referenceType = referenceType,
    read = read,
    createdAt = createdAt
)

fun Question.toSearchDocument(): QuestionSearchDocument = QuestionSearchDocument(
    id = id!!,
    title = title,
    content = content,
    tags = tags.map { it.name }.sorted(),
    authorNickname = author.nickname,
    answerCount = answerCount,
    acceptedAnswerId = acceptedAnswerId,
    createdAt = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
)

fun QuestionSearchDocument.toResponse(): QuestionSearchResponse = QuestionSearchResponse(
    id = id,
    title = title,
    contentSnippet = content.take(140),
    tags = tags,
    authorNickname = authorNickname,
    answerCount = answerCount,
    acceptedAnswerId = acceptedAnswerId,
    createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault())
)
