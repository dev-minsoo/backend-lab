package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Answer
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface AnswerRepository : JpaRepository<Answer, Long> {

    @EntityGraph(attributePaths = ["author"])
    @Query("select a from Answer a where a.question.id = :questionId and a.deleted = false order by a.accepted desc, a.createdAt asc")
    fun findActiveAnswersByQuestionId(@Param("questionId") questionId: Long): List<Answer>

    @EntityGraph(attributePaths = ["author", "question"])
    @Query("select a from Answer a where a.author.id = :authorId and a.deleted = false order by a.createdAt desc")
    fun findActiveAnswersByAuthorId(@Param("authorId") authorId: Long): List<Answer>

    @EntityGraph(attributePaths = ["author", "question"])
    @Query("select a from Answer a where a.id = :id and a.deleted = false")
    fun findActiveAnswerById(@Param("id") id: Long): Optional<Answer>
}
