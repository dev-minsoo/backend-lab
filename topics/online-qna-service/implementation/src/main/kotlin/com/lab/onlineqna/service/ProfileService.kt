package com.lab.onlineqna.service

import com.lab.onlineqna.config.UsePrimaryDataSource
import com.lab.onlineqna.dto.UserProfileResponse
import com.lab.onlineqna.exception.DomainException
import com.lab.onlineqna.repository.NotificationRepository
import com.lab.onlineqna.repository.UserRepository
import com.lab.onlineqna.support.toResponse
import com.lab.onlineqna.support.toSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val questionService: QuestionService,
    private val answerService: AnswerService,
    private val notificationRepository: NotificationRepository
) {

    @Transactional(readOnly = true)
    @UsePrimaryDataSource
    fun getProfile(userId: Long): UserProfileResponse {
        val user = userRepository.findById(userId).orElseThrow { DomainException("사용자를 찾을 수 없습니다.") }
        return UserProfileResponse(
            user = user.toSummary(),
            myQuestions = questionService.getQuestionsByAuthor(userId),
            myAnswers = answerService.getAnswersByAuthor(userId),
            notifications = notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }
        )
    }
}
