package com.lab.nplusone.repository

import com.lab.nplusone.domain.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BookRepository : JpaRepository<Book, Long> {

    // 해결 방법 1: Fetch Join
    @Query("SELECT b FROM Book b JOIN FETCH b.author")
    fun findAllWithAuthor(): List<Book>
}
