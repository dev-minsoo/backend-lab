package com.lab.onlineqna.performance

import com.lab.onlineqna.dto.CreateQuestionRequest
import com.lab.onlineqna.dto.SignUpRequest
import com.lab.onlineqna.service.AuthService
import com.lab.onlineqna.service.QuestionService
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.system.measureTimeMillis

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OnlineQnaPerformanceTest {

    companion object {
        const val DATA_SIZE = 100
    }

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var questionService: QuestionService

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = authService.signUp(
            SignUpRequest(
                email = "perf@example.com",
                nickname = "perf",
                password = "password1234"
            )
        ).user.id

        repeat(DATA_SIZE) { index ->
            questionService.create(
                userId,
                CreateQuestionRequest(
                    title = "question-$index",
                    content = "read heavy benchmark content $index",
                    tags = setOf("cache", "redis")
                )
            )
        }
    }

    @Test
    @DisplayName("캐시된 상세 조회가 반복 조회에서 유리하다")
    fun `performance comparison`() {
        val target = questionService.getQuestions(0, 1).first()

        val coldRead = measureTimeMillis {
            repeat(30) {
                questionService.getQuestion(target.id)
            }
        }

        val warmRead = measureTimeMillis {
            repeat(30) {
                questionService.getQuestion(target.id)
            }
        }

        println("coldRead=$coldRead ms, warmRead=$warmRead ms")
        (warmRead <= coldRead) shouldBe true
    }
}
