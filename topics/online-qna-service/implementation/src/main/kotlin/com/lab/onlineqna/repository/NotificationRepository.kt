package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findTop20ByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
}
