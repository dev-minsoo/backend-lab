package com.lab.onlineqna.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 200)
    val message: String,

    @Column(nullable = false)
    val referenceId: Long,

    @Column(nullable = false, length = 30)
    val referenceType: String,

    @Column(name = "is_read", nullable = false)
    var read: Boolean = false
) : BaseEntity() {
    fun markRead() {
        read = true
    }
}
