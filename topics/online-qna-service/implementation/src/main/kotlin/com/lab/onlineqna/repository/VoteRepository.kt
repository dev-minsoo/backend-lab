package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Vote
import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.domain.VoteType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VoteRepository : JpaRepository<Vote, Long> {
    fun findByUserIdAndTargetTypeAndTargetId(userId: Long, targetType: VoteTargetType, targetId: Long): Vote?
    fun countByTargetTypeAndTargetIdAndType(targetType: VoteTargetType, targetId: Long, type: VoteType): Long

    @Query(
        """
        select v.targetId as targetId, v.type as type, count(v.id) as count
        from Vote v
        where v.targetType = :targetType and v.targetId in :targetIds
        group by v.targetId, v.type
        """
    )
    fun countGroupedByTargetIds(
        @Param("targetType") targetType: VoteTargetType,
        @Param("targetIds") targetIds: Collection<Long>
    ): List<VoteCountProjection>
}

interface VoteCountProjection {
    val targetId: Long
    val type: VoteType
    val count: Long
}
