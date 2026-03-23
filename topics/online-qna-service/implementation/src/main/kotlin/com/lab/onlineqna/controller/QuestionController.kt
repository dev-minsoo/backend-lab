package com.lab.onlineqna.controller

import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.domain.VoteType
import com.lab.onlineqna.dto.CreateAnswerRequest
import com.lab.onlineqna.dto.CreateQuestionRequest
import com.lab.onlineqna.dto.QuestionDetailResponse
import com.lab.onlineqna.dto.QuestionSummary
import com.lab.onlineqna.dto.ReportRequest
import com.lab.onlineqna.dto.UpdateQuestionRequest
import com.lab.onlineqna.dto.VoteRequest
import com.lab.onlineqna.security.currentUserId
import com.lab.onlineqna.service.AnswerService
import com.lab.onlineqna.service.QuestionService
import com.lab.onlineqna.service.VoteService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/questions")
class QuestionController(
    private val questionService: QuestionService,
    private val answerService: AnswerService,
    private val voteService: VoteService
) {

    @PostMapping
    fun createQuestion(@Valid @RequestBody request: CreateQuestionRequest): QuestionDetailResponse =
        questionService.create(currentUserId(), request)

    @GetMapping
    fun getQuestions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<QuestionSummary> = questionService.getQuestions(page, size)

    @GetMapping("/{questionId}")
    fun getQuestion(@PathVariable questionId: Long): QuestionDetailResponse = questionService.getQuestion(questionId)

    @PutMapping("/{questionId}")
    fun updateQuestion(
        @PathVariable questionId: Long,
        @Valid @RequestBody request: UpdateQuestionRequest
    ): QuestionDetailResponse = questionService.update(currentUserId(), questionId, request)

    @DeleteMapping("/{questionId}")
    fun deleteQuestion(@PathVariable questionId: Long) = questionService.delete(currentUserId(), questionId)

    @PostMapping("/{questionId}/answers")
    fun createAnswer(
        @PathVariable questionId: Long,
        @Valid @RequestBody request: CreateAnswerRequest
    ) = answerService.create(currentUserId(), questionId, request)

    @PostMapping("/{questionId}/vote")
    fun voteQuestion(@PathVariable questionId: Long, @RequestBody request: VoteRequest) {
        voteService.vote(currentUserId(), VoteTargetType.QUESTION, questionId, VoteType.valueOf(request.type.uppercase()))
    }

    @PostMapping("/{questionId}/report")
    fun reportQuestion(@PathVariable questionId: Long, @Valid @RequestBody request: ReportRequest) {
        questionService.reportQuestion(currentUserId(), questionId, request)
    }
}
