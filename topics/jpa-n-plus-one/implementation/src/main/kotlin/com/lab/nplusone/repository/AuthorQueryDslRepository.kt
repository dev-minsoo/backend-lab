package com.lab.nplusone.repository

import com.lab.nplusone.domain.Author
import com.lab.nplusone.domain.QAuthor
import com.lab.nplusone.domain.QBook
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class AuthorQueryDslRepository(
    private val queryFactory: JPAQueryFactory
) {
    private val author = QAuthor.author
    private val book = QBook.book

    // N+1 문제 발생
    fun findAllBasic(): List<Author> {
        return queryFactory
            .selectFrom(author)
            .fetch()
    }

    // 해결 방법 1: Fetch Join (QueryDSL)
    fun findAllWithFetchJoin(): List<Author> {
        return queryFactory
            .selectFrom(author)
            .distinct()
            .leftJoin(author.books, book).fetchJoin()
            .fetch()
    }

    // 해결 방법 2: 별도 쿼리로 분리 후 메모리에서 조합
    fun findAllWithSeparateQuery(): List<Author> {
        val authors = queryFactory
            .selectFrom(author)
            .fetch()

        val authorIds = authors.map { it.id }

        // Books를 한 번에 조회
        queryFactory
            .selectFrom(book)
            .where(book.author.id.`in`(authorIds))
            .fetch()

        return authors
    }

    // 단건 조회 - Fetch Join
    fun findByIdWithBooks(id: Long): Author? {
        return queryFactory
            .selectFrom(author)
            .leftJoin(author.books, book).fetchJoin()
            .where(author.id.eq(id))
            .fetchOne()
    }
}
