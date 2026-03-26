package com.lab.onlineqna.service

import com.lab.onlineqna.domain.Answer
import com.lab.onlineqna.domain.Report
import com.lab.onlineqna.domain.ReportTargetType
import com.lab.onlineqna.domain.User
import com.lab.onlineqna.dto.AnswerSummary
import com.lab.onlineqna.dto.CreateAnswerRequest
import com.lab.onlineqna.dto.ReportRequest
import com.lab.onlineqna.dto.UpdateAnswerRequest
import com.lab.onlineqna.event.ChangeType
import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent
import com.lab.onlineqna.exception.DomainException
import com.lab.onlineqna.repository.AnswerRepository
import com.lab.onlineqna.repository.QuestionRepository
import com.lab.onlineqna.repository.ReportRepository
import com.lab.onlineqna.repository.UserRepository
import com.lab.onlineqna.support.toSummary
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnswerService(
    private val answerRepository: AnswerRepository,
    private val questionRepository: QuestionRepository,
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val voteService: VoteService,
    private val eventPublisher: EventPublisher
) {

    @Transactional
    @CacheEvict(cacheNames = ["questionDetail", "questionList"], allEntries = true)
    fun create(userId: Long, questionId: Long, request: CreateAnswerRequest): AnswerSummary {
        val user = loadUser(userId)
        val question = questionRepository.findActiveQuestionById(questionId)
            .orElseThrow { DomainException("질문을 찾을 수 없습니다.") }
        val answer = answerRepository.save(
            Answer(
                question = question,
                author = user,
                content = request.content
            )
        )
        question.increaseAnswerCount()
        eventPublisher.publishQuestionChanged(QuestionChangedEvent(questionId, ChangeType.UPSERT))
        if (question.author.id != userId) {
            publishNotification(
                question.author.id!!,
                "내 질문에 새 답변이 등록되었습니다: ${question.title}",
                question.id!!,
                "QUESTION"
            )
        }
        publishNotification(user.id!!, "답변이 등록되었습니다.", answer.id!!, "ANSWER")
        return answer.toSummary(voteService.summarize(com.lab.onlineqna.domain.VoteTargetType.ANSWER, answer.id!!))
    }

    @Transactional
    @CacheEvict(cacheNames = ["questionDetail"], allEntries = true)
    fun update(userId: Long, answerId: Long, request: UpdateAnswerRequest): AnswerSummary {
        val answer = loadAnswer(answerId)
        validateOwner(answer.author.id!!, userId)
        answer.update(request.content)
        eventPublisher.publishQuestionChanged(QuestionChangedEvent(answer.question.id!!, ChangeType.UPSERT))
        return answer.toSummary(voteService.summarize(com.lab.onlineqna.domain.VoteTargetType.ANSWER, answer.id!!))
    }

    @Transactional
    @CacheEvict(cacheNames = ["questionDetail", "questionList"], allEntries = true)
    fun delete(userId: Long, answerId: Long) {
        val answer = loadAnswer(answerId)
        validateOwner(answer.author.id!!, userId)
        answer.markDeleted()
        answer.question.decreaseAnswerCount()
        eventPublisher.publishQuestionChanged(QuestionChangedEvent(answer.question.id!!, ChangeType.UPSERT))
    }

    @Transactional
    @CacheEvict(cacheNames = ["questionDetail", "questionList"], allEntries = true)
    fun accept(userId: Long, questionId: Long, answerId: Long): AnswerSummary {
        val question = questionRepository.findActiveQuestionById(questionId)
            .orElseThrow { DomainException("질문을 찾을 수 없습니다.") }
        validateOwner(question.author.id!!, userId)

        val answer = loadAnswer(answerId)
        if (answer.question.id != questionId) {
            throw DomainException("질문에 속한 답변이 아닙니다.")
        }

        answerRepository.findActiveAnswersByQuestionId(questionId).forEach { it.revokeAcceptance() }
        answer.accept()
        question.accept(answerId)
        publishNotification(answer.author.id!!, "답변이 채택되었습니다.", answer.id!!, "ANSWER")
        eventPublisher.publishQuestionChanged(QuestionChangedEvent(questionId, ChangeType.UPSERT))
        return answer.toSummary(voteService.summarize(com.lab.onlineqna.domain.VoteTargetType.ANSWER, answer.id!!))
    }

    @Transactional
    fun reportAnswer(userId: Long, answerId: Long, request: ReportRequest) {
        val user = loadUser(userId)
        loadAnswer(answerId)
        reportRepository.save(
            Report(
                reporter = user,
                targetType = ReportTargetType.ANSWER,
                targetId = answerId,
                reason = request.reason
            )
        )
    }

    @Transactional(readOnly = true)
    fun getAnswersByAuthor(userId: Long): List<AnswerSummary> {
        val answers = answerRepository.findActiveAnswersByAuthorId(userId)
        val voteSummaries = voteService.summarizeAll(
            com.lab.onlineqna.domain.VoteTargetType.ANSWER,
            answers.mapNotNull { it.id }
        )
        return answers.map { answer ->
            answer.toSummary(voteSummaries[answer.id!!] ?: com.lab.onlineqna.dto.VoteSummary(likes = 0, dislikes = 0))
        }
    }

    private fun publishNotification(userId: Long, message: String, referenceId: Long, referenceType: String) {
        eventPublisher.publishNotificationCreated(
            NotificationCreatedEvent(
                userId = userId,
                message = message,
                referenceId = referenceId,
                referenceType = referenceType
            )
        )
    }

    private fun loadUser(userId: Long): User =
        userRepository.findById(userId).orElseThrow { DomainException("사용자를 찾을 수 없습니다.") }

    private fun loadAnswer(answerId: Long): Answer =
        answerRepository.findActiveAnswerById(answerId)
            .orElseThrow { DomainException("답변을 찾을 수 없습니다.") }

    private fun validateOwner(ownerId: Long, currentUserId: Long) {
        if (ownerId != currentUserId) {
            throw DomainException("작성자만 수정 또는 삭제할 수 있습니다.")
        }
    }
}
