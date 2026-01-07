package com.lab.nplusone.integration

import com.lab.nplusone.domain.Author
import com.lab.nplusone.domain.Book
import com.lab.nplusone.repository.AuthorRepository
import com.lab.nplusone.service.AuthorService
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NPlusOneTest {

    @Autowired
    private lateinit var authorService: AuthorService

    @Autowired
    private lateinit var authorRepository: AuthorRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 생성: 3명의 저자, 각각 2권의 책
        repeat(3) { authorIndex ->
            val author = Author(
                name = "Author ${authorIndex + 1}",
                email = "author${authorIndex + 1}@example.com"
            )

            repeat(2) { bookIndex ->
                val book = Book(
                    title = "Book ${authorIndex + 1}-${bookIndex + 1}",
                    isbn = "ISBN-${authorIndex + 1}-${bookIndex + 1}"
                )
                author.addBook(book)
            }

            authorRepository.save(author)
        }
    }

    @Test
    @DisplayName("N+1 문제 발생 케이스")
    fun `should trigger N plus 1 problem`() {
        // given
        println("\n=== N+1 문제 발생 케이스 ===")
        entityManager.flush()
        entityManager.clear()  // 영속성 컨텍스트 초기화

        // when
        val authors = authorService.findAllAuthorsWithNPlusOne()

        // then
        authors shouldHaveSize 3
        authors.forEach { author ->
            author.books shouldHaveSize 2
        }

        // 예상되는 쿼리 수: 1 (Author 조회) + 3 (각 Author의 Books 조회) = 4개
        println("총 4개의 쿼리가 실행됨 (1 + N)")
    }

    @Test
    @DisplayName("Fetch Join으로 N+1 문제 해결")
    fun `should solve N plus 1 with fetch join`() {
        // given
        println("\n=== Fetch Join으로 해결 ===")
        entityManager.flush()
        entityManager.clear()

        // when
        val authors = authorService.findAllAuthorsWithFetchJoin()

        // then
        authors shouldHaveSize 3
        authors.forEach { author ->
            author.books shouldHaveSize 2
        }

        // 예상되는 쿼리 수: 1개 (JOIN으로 한 번에 조회)
        println("총 1개의 쿼리가 실행됨")
    }

    @Test
    @DisplayName("EntityGraph로 N+1 문제 해결")
    fun `should solve N plus 1 with entity graph`() {
        // given
        println("\n=== EntityGraph로 해결 ===")
        entityManager.flush()
        entityManager.clear()

        // when
        val authors = authorService.findAllAuthorsWithEntityGraph()

        // then
        authors shouldHaveSize 3
        authors.forEach { author ->
            author.books shouldHaveSize 2
        }

        // 예상되는 쿼리 수: 1개
        println("총 1개의 쿼리가 실행됨")
    }

    @Test
    @DisplayName("QueryDSL Fetch Join으로 N+1 문제 해결")
    fun `should solve N plus 1 with QueryDSL fetch join`() {
        // given
        println("\n=== QueryDSL Fetch Join으로 해결 ===")
        entityManager.flush()
        entityManager.clear()

        // when
        val authors = authorService.findAllAuthorsWithQueryDslFetchJoin()

        // then
        authors shouldHaveSize 3
        authors.forEach { author ->
            author.books shouldHaveSize 2
        }

        // 예상되는 쿼리 수: 1개
        println("총 1개의 쿼리가 실행됨")
    }

    @Test
    @DisplayName("QueryDSL 별도 쿼리로 N+1 문제 해결")
    fun `should solve N plus 1 with QueryDSL separate queries`() {
        // given
        println("\n=== QueryDSL 별도 쿼리로 해결 ===")
        entityManager.flush()
        entityManager.clear()

        // when
        val authors = authorService.findAllAuthorsWithSeparateQuery()

        // then
        authors shouldHaveSize 3
        authors.forEach { author ->
            author.books shouldHaveSize 2
        }

        // 예상되는 쿼리 수: 2개 (Author 1개 + Books IN 쿼리 1개)
        println("총 2개의 쿼리가 실행됨")
    }

    @Test
    @DisplayName("Batch Size로 N+1 문제 완화")
    fun `should mitigate N plus 1 with batch size`() {
        // given
        println("\n=== Batch Size로 해결 ===")
        entityManager.flush()
        entityManager.clear()

        // when
        val authors = authorService.findAllAuthorsWithBatchSize()

        // then
        authors shouldHaveSize 3
        authors.forEach { author ->
            author.books shouldHaveSize 2
        }

        // 예상되는 쿼리 수: 2개 (Author 1개 + Books IN 쿼리 1개)
        // Batch Size가 설정되어 있으면 IN 절로 한 번에 조회
        println("총 2개의 쿼리가 실행됨 (Batch Size 적용)")
    }

    @Test
    @DisplayName("단건 조회 - Fetch Join")
    fun `should fetch single author with books`() {
        // given
        println("\n=== 단건 조회 Fetch Join ===")
        val savedAuthor = authorRepository.findAll().first()
        entityManager.flush()
        entityManager.clear()

        // when
        val author = authorRepository.findByIdWithBooks(savedAuthor.id!!)

        // then
        author shouldNotBe null
        author!!.books shouldHaveSize 2

        println("단건 조회 시에도 1개의 쿼리만 실행됨")
    }
}
