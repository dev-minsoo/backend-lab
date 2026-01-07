package com.lab.nplusone.domain

import jakarta.persistence.*

@Entity
@Table(name = "authors")
class Author(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column
    val email: String,

    @OneToMany(mappedBy = "author", cascade = [CascadeType.ALL], orphanRemoval = true)
    val books: MutableList<Book> = mutableListOf()
) {
    fun addBook(book: Book) {
        books.add(book)
        book.author = this
    }

    override fun toString(): String {
        return "Author(id=$id, name='$name', email='$email', booksCount=${books.size})"
    }
}
