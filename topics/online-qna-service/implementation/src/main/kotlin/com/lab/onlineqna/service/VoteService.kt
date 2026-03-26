package com.lab.onlineqna.service

import com.lab.onlineqna.domain.Vote
import com.lab.onlineqna.domain.VoteTargetType
import com.lab.onlineqna.domain.VoteType
import com.lab.onlineqna.dto.VoteSummary
import com.lab.onlineqna.exception.DomainException
import com.lab.onlineqna.repository.UserRepository
import com.lab.onlineqna.repository.VoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun vote(userId: Long, targetType: VoteTargetType, targetId: Long, voteType: VoteType) {
        val user = userRepository.findById(userId).orElseThrow { DomainException("사용자를 찾을 수 없습니다.") }
        val existing = voteRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
        if (existing == null) {
            voteRepository.save(Vote(user = user, targetType = targetType, targetId = targetId, type = voteType))
            return
        }
        existing.updateType(voteType)
    }

    @Transactional(readOnly = true)
    fun summarize(targetType: VoteTargetType, targetId: Long): VoteSummary = VoteSummary(
        likes = voteRepository.countByTargetTypeAndTargetIdAndType(targetType, targetId, VoteType.LIKE),
        dislikes = voteRepository.countByTargetTypeAndTargetIdAndType(targetType, targetId, VoteType.DISLIKE)
    )

    @Transactional(readOnly = true)
    fun summarizeAll(targetType: VoteTargetType, targetIds: Collection<Long>): Map<Long, VoteSummary> {
        if (targetIds.isEmpty()) {
            return emptyMap()
        }

        val summaries = targetIds.distinct().associateWith { VoteSummary(likes = 0, dislikes = 0) }.toMutableMap()

        voteRepository.countGroupedByTargetIds(targetType, targetIds).forEach { projection ->
            val current = summaries.getValue(projection.targetId)
            summaries[projection.targetId] = when (projection.type) {
                VoteType.LIKE -> current.copy(likes = projection.count)
                VoteType.DISLIKE -> current.copy(dislikes = projection.count)
            }
        }

        return summaries
    }
}
