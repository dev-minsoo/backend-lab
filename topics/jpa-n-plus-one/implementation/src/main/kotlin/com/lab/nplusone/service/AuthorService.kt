package com.lab.nplusone.service

import com.lab.nplusone.domain.Author
import com.lab.nplusone.repository.AuthorQueryDslRepository
import com.lab.nplusone.repository.AuthorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthorService(
    private val authorRepository: AuthorRepository,
    private val authorQueryDslRepository: AuthorQueryDslRepository
) {

    /**
     * N+1 문제 발생 케이스
     * - Author를 조회한 후 books에 접근하면 각 Author마다 추가 쿼리 발생
     */
    fun findAllAuthorsWithNPlusOne(): List<Author> {
        val authors = authorRepository.findAll()
        // books에 접근하는 순간 N+1 발생
        authors.forEach { author ->
            println("Author: ${author.name}, Books count: ${author.books.size}")
        }
        return authors
    }

    /**
     * 해결 방법 1: Fetch Join (JPQL)
     * - 한 번의 쿼리로 Author와 Book을 모두 조회
     */
    fun findAllAuthorsWithFetchJoin(): List<Author> {
        return authorRepository.findAllWithFetchJoin()
    }

    /**
     * 해결 방법 2: EntityGraph
     * - 애너테이션 기반으로 연관 엔티티 함께 조회
     */
    fun findAllAuthorsWithEntityGraph(): List<Author> {
        return authorRepository.findAllWithEntityGraph()
    }

    /**
     * 해결 방법 3: QueryDSL Fetch Join
     */
    fun findAllAuthorsWithQueryDslFetchJoin(): List<Author> {
        return authorQueryDslRepository.findAllWithFetchJoin()
    }

    /**
     * 해결 방법 4: QueryDSL 별도 쿼리
     * - Author와 Book을 각각 조회하지만 IN 쿼리로 한 번에 조회
     */
    fun findAllAuthorsWithSeparateQuery(): List<Author> {
        return authorQueryDslRepository.findAllWithSeparateQuery()
    }

    /**
     * Batch Size 테스트용
     * - application.yml의 default_batch_fetch_size 설정 확인
     */
    fun findAllAuthorsWithBatchSize(): List<Author> {
        val authors = authorRepository.findAll()
        // Batch Size가 설정되어 있으면 IN 쿼리로 한 번에 조회
        authors.forEach { author ->
            println("Author: ${author.name}, Books count: ${author.books.size}")
        }
        return authors
    }
}
