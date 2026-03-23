package com.lab.onlineqna.controller

import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.domain.VoteType
import com.lab.onlineqna.dto.ReportRequest
import com.lab.onlineqna.dto.UpdateAnswerRequest
import com.lab.onlineqna.dto.VoteRequest
import com.lab.onlineqna.security.currentUserId
import com.lab.onlineqna.service.AnswerService
import com.lab.onlineqna.service.VoteService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/answers")
class AnswerController(
    private val answerService: AnswerService,
    private val voteService: VoteService
) {

    @PutMapping("/{answerId}")
    fun updateAnswer(@PathVariable answerId: Long, @Valid @RequestBody request: UpdateAnswerRequest) =
        answerService.update(currentUserId(), answerId, request)

    @DeleteMapping("/{answerId}")
    fun deleteAnswer(@PathVariable answerId: Long) = answerService.delete(currentUserId(), answerId)

    @PostMapping("/{answerId}/accept")
    fun acceptAnswer(@PathVariable answerId: Long, @RequestBody payload: Map<String, Long>) =
        answerService.accept(currentUserId(), payload.getValue("questionId"), answerId)

    @PostMapping("/{answerId}/vote")
    fun voteAnswer(@PathVariable answerId: Long, @RequestBody request: VoteRequest) {
        voteService.vote(currentUserId(), VoteTargetType.ANSWER, answerId, VoteType.valueOf(request.type.uppercase()))
    }

    @PostMapping("/{answerId}/report")
    fun reportAnswer(@PathVariable answerId: Long, @Valid @RequestBody request: ReportRequest) {
        answerService.reportAnswer(currentUserId(), answerId, request)
    }
}
