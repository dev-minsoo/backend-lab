package com.lab.onlineqna.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "questions")
class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,

    @Column(nullable = false, length = 150)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(nullable = false)
    var answerCount: Int = 0,

    @Column(nullable = true)
    var acceptedAnswerId: Long? = null
) : BaseEntity() {

    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.MERGE, CascadeType.PERSIST])
    @JoinTable(
        name = "question_tags",
        joinColumns = [JoinColumn(name = "question_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    val tags: MutableSet<Tag> = linkedSetOf()

    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], orphanRemoval = true)
    val answers: MutableList<Answer> = mutableListOf()

    fun update(title: String, content: String, tags: Set<Tag>) {
        this.title = title
        this.content = content
        this.tags.clear()
        this.tags.addAll(tags)
    }

    fun markDeleted() {
        deleted = true
    }

    fun increaseAnswerCount() {
        answerCount += 1
    }

    fun decreaseAnswerCount() {
        answerCount = (answerCount - 1).coerceAtLeast(0)
    }

    fun accept(answerId: Long) {
        acceptedAnswerId = answerId
    }
}
