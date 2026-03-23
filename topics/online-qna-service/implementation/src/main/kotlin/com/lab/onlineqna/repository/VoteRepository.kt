package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Vote
import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.domain.VoteType
import org.springframework.data.jpa.repository.JpaRepository

interface VoteRepository : JpaRepository<Vote, Long> {
    fun findByUserIdAndTargetTypeAndTargetId(userId: Long, targetType: VoteTargetType, targetId: Long): Vote?
    fun countByTargetTypeAndTargetIdAndType(targetType: VoteTargetType, targetId: Long, type: VoteType): Long
}
