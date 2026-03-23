package com.lab.onlineqna.event

data class QuestionChangedEvent(
    val questionId: Long,
    val changeType: ChangeType
)

data class NotificationCreatedEvent(
    val userId: Long,
    val message: String,
    val referenceId: Long,
    val referenceType: String
)

enum class ChangeType {
    UPSERT,
    DELETE
}
