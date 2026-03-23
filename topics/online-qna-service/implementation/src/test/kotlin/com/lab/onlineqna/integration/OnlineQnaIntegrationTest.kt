package com.lab.onlineqna.integration

import com.lab.onlineqna.dto.CreateAnswerRequest
import com.lab.onlineqna.dto.CreateQuestionRequest
import com.lab.onlineqna.dto.LoginRequest
import com.lab.onlineqna.dto.SignUpRequest
import com.lab.onlineqna.service.AnswerService
import com.lab.onlineqna.service.AuthService
import com.lab.onlineqna.service.ProfileService
import com.lab.onlineqna.service.QuestionService
import com.lab.onlineqna.service.SearchService
import com.lab.onlineqna.service.VoteService
import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.domain.VoteType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OnlineQnaIntegrationTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var questionService: QuestionService

    @Autowired
    private lateinit var answerService: AnswerService

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var voteService: VoteService

    @Autowired
    private lateinit var profileService: ProfileService

    private var askerId: Long = 0
    private var responderId: Long = 0

    @BeforeEach
    fun setUp() {
        askerId = authService.signUp(
            SignUpRequest(
                email = "asker@example.com",
                nickname = "asker",
                password = "password1234"
            )
        ).user.id

        responderId = authService.signUp(
            SignUpRequest(
                email = "responder@example.com",
                nickname = "responder",
                password = "password1234"
            )
        ).user.id
    }

    @Test
    @DisplayName("회원가입과 로그인")
    fun `should sign up and login`() {
        val response = authService.login(LoginRequest("asker@example.com", "password1234"))

        response.accessToken shouldNotBe null
        response.user.email shouldBe "asker@example.com"
    }

    @Test
    @DisplayName("질문 생성 후 상세 조회와 태그 검색")
    fun `should create question and search by tag`() {
        val created = questionService.create(
            askerId,
            CreateQuestionRequest(
                title = "Redis 캐시 무효화 전략이 궁금합니다",
                content = "cache aside 와 write through 중 무엇이 적합한지 궁금합니다",
                tags = setOf("redis", "cache", "system-design")
            )
        )

        val loaded = questionService.getQuestion(created.id)
        val searchResult = searchService.search(keyword = "캐시", tags = listOf("redis"))

        loaded.tags shouldBe setOf("redis", "cache", "system-design")
        searchResult shouldHaveSize 1
        searchResult.first().id shouldBe created.id
    }

    @Test
    @DisplayName("답변 등록 후 채택과 알림 생성")
    fun `should create answer accept it and create notifications`() {
        val question = questionService.create(
            askerId,
            CreateQuestionRequest(
                title = "ES 인덱싱 타이밍은 언제가 좋을까",
                content = "동기식과 비동기식 중 무엇이 적절한가",
                tags = setOf("elasticsearch", "kafka")
            )
        )

        val answer = answerService.create(
            responderId,
            question.id,
            CreateAnswerRequest("비동기 이벤트로 색인하면 쓰기 지연을 줄일 수 있습니다.")
        )

        val accepted = answerService.accept(askerId, question.id, answer.id)
        val profile = profileService.getProfile(responderId)

        accepted.accepted shouldBe true
        profile.notifications.shouldHaveSize(2)
    }

    @Test
    @DisplayName("좋아요와 싫어요 집계")
    fun `should aggregate votes`() {
        val question = questionService.create(
            askerId,
            CreateQuestionRequest(
                title = "read replica lag 대응",
                content = "쓰자마자 읽기 일관성은 어떻게 보장할까",
                tags = setOf("mysql", "replica")
            )
        )

        voteService.vote(
            userId = responderId,
            targetType = VoteTargetType.QUESTION,
            targetId = question.id,
            voteType = VoteType.LIKE
        )

        val summary = questionService.getQuestion(question.id).votes
        summary.likes shouldBe 1
        summary.dislikes shouldBe 0
    }
}
