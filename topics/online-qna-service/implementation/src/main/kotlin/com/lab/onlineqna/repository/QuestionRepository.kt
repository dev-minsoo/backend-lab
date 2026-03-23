package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Question
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface QuestionRepository : JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = ["author", "tags"])
    @Query("select q from Question q where q.deleted = false order by q.createdAt desc")
    fun findActiveQuestions(pageable: Pageable): Page<Question>

    @EntityGraph(attributePaths = ["author", "tags"])
    @Query("select q from Question q where q.id = :id and q.deleted = false")
    fun findActiveQuestionById(@Param("id") id: Long): Optional<Question>

    @EntityGraph(attributePaths = ["author", "tags"])
    @Query("select q from Question q where q.author.id = :authorId and q.deleted = false order by q.createdAt desc")
    fun findActiveQuestionsByAuthorId(@Param("authorId") authorId: Long, pageable: Pageable): Page<Question>
}
