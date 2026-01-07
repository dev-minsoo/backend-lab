package com.lab.nplusone.repository

import com.lab.nplusone.domain.Author
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AuthorRepository : JpaRepository<Author, Long> {

    // 해결 방법 1: Fetch Join 사용 (JPQL)
    @Query("SELECT DISTINCT a FROM Author a JOIN FETCH a.books")
    fun findAllWithFetchJoin(): List<Author>

    // 해결 방법 2: EntityGraph 사용 (애너테이션 방식)
    @EntityGraph(attributePaths = ["books"])
    @Query("SELECT a FROM Author a")
    fun findAllWithEntityGraph(): List<Author>

    // 해결 방법 3: Batch Size는 application.yml에서 설정
    // default_batch_fetch_size: 100

    // 단일 조회 - Fetch Join
    @Query("SELECT a FROM Author a JOIN FETCH a.books WHERE a.id = :id")
    fun findByIdWithBooks(id: Long): Author?
}
