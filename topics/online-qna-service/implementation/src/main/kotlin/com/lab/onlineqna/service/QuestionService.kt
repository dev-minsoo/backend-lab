package com.lab.onlineqna.service

import com.lab.onlineqna.domain.Question
import com.lab.onlineqna.domain.Report
import com.lab.onlineqna.domain.ReportTargetType
import com.lab.onlineqna.domain.User
import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.dto.CreateQuestionRequest
import com.lab.onlineqna.dto.QuestionDetailResponse
import com.lab.onlineqna.dto.QuestionSummary
import com.lab.onlineqna.dto.ReportRequest
import com.lab.onlineqna.dto.UpdateQuestionRequest
import com.lab.onlineqna.event.ChangeType
import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent
import com.lab.onlineqna.exception.DomainException
import com.lab.onlineqna.repository.AnswerRepository
import com.lab.onlineqna.repository.QuestionRepository
import com.lab.onlineqna.repository.ReportRepository
import com.lab.onlineqna.repository.UserRepository
import com.lab.onlineqna.support.toDetail
import com.lab.onlineqna.support.toSummary
import org.springframework.cache.annotation.Caching
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val tagService: TagService,
    private val voteService: VoteService,
    private val eventPublisher: EventPublisher
) {

    @Transactional
    @CacheEvict(cacheNames = ["questionList"], allEntries = true)
    fun create(userId: Long, request: CreateQuestionRequest): QuestionDetailResponse {
        val author = loadUser(userId)
        val question = questionRepository.save(
            Question(
                author = author,
                title = request.title,
                content = request.content
            )
        )
        question.tags.addAll(tagService.resolveTags(request.tags))
        val questionId = question.id ?: throw DomainException("질문 저장에 실패했습니다.")
        publishQuestionChanged(questionId)
        publishNotification(author.id!!, "질문이 등록되었습니다: ${question.title}", questionId, "QUESTION")
        return getQuestion(questionId)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["questionDetail"], key = "#questionId"),
            CacheEvict(cacheNames = ["questionList"], allEntries = true)
        ]
    )
    fun update(userId: Long, questionId: Long, request: UpdateQuestionRequest): QuestionDetailResponse {
        val question = loadQuestion(questionId)
        validateOwner(question.author.id!!, userId)
        question.update(request.title, request.content, tagService.resolveTags(request.tags))
        publishQuestionChanged(questionId)
        return getQuestion(questionId)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["questionDetail"], key = "#questionId"),
            CacheEvict(cacheNames = ["questionList"], allEntries = true)
        ]
    )
    fun delete(userId: Long, questionId: Long) {
        val question = loadQuestion(questionId)
        validateOwner(question.author.id!!, userId)
        question.markDeleted()
        eventPublisher.publishQuestionChanged(QuestionChangedEvent(questionId, ChangeType.DELETE))
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["questionDetail"], key = "#questionId")
    fun getQuestion(questionId: Long): QuestionDetailResponse {
        val question = loadQuestion(questionId)
        val answerEntities = answerRepository.findActiveAnswersByQuestionId(questionId)
        val answerVoteSummaries = voteService.summarizeAll(
            VoteTargetType.ANSWER,
            answerEntities.mapNotNull { it.id }
        )
        val answers = answerEntities.map {
            it.toSummary(answerVoteSummaries[it.id!!] ?: com.lab.onlineqna.dto.VoteSummary(likes = 0, dislikes = 0))
        }
        return question.toDetail(voteService.summarize(VoteTargetType.QUESTION, questionId), answers)
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ["questionList"], key = "#page + ':' + #size")
    fun getQuestions(page: Int, size: Int): List<QuestionSummary> {
        val questions = questionRepository.findActiveQuestions(PageRequest.of(page, size)).content
        val voteSummaries = voteService.summarizeAll(
            VoteTargetType.QUESTION,
            questions.mapNotNull { it.id }
        )
        return questions.map { question ->
            question.toSummary(voteSummaries[question.id!!] ?: com.lab.onlineqna.dto.VoteSummary(likes = 0, dislikes = 0))
        }
    }

    @Transactional
    fun reportQuestion(userId: Long, questionId: Long, request: ReportRequest) {
        val user = loadUser(userId)
        loadQuestion(questionId)
        reportRepository.save(
            Report(
                reporter = user,
                targetType = ReportTargetType.QUESTION,
                targetId = questionId,
                reason = request.reason
            )
        )
    }

    @Transactional(readOnly = true)
    fun getQuestionsByAuthor(userId: Long): List<QuestionSummary> {
        val questions = questionRepository.findActiveQuestionsByAuthorId(userId, PageRequest.of(0, 20)).content
        val voteSummaries = voteService.summarizeAll(
            VoteTargetType.QUESTION,
            questions.mapNotNull { it.id }
        )
        return questions.map { question ->
            question.toSummary(voteSummaries[question.id!!] ?: com.lab.onlineqna.dto.VoteSummary(likes = 0, dislikes = 0))
        }
    }

    private fun publishQuestionChanged(questionId: Long) {
        eventPublisher.publishQuestionChanged(QuestionChangedEvent(questionId, ChangeType.UPSERT))
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

    private fun loadQuestion(questionId: Long): Question =
        questionRepository.findActiveQuestionById(questionId)
            .orElseThrow { DomainException("질문을 찾을 수 없습니다.") }

    private fun loadUser(userId: Long): User =
        userRepository.findById(userId).orElseThrow { DomainException("사용자를 찾을 수 없습니다.") }

    private fun validateOwner(ownerId: Long, currentUserId: Long) {
        if (ownerId != currentUserId) {
            throw DomainException("작성자만 수정 또는 삭제할 수 있습니다.")
        }
    }
}
