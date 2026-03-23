package com.lab.onlineqna.service

import com.lab.onlineqna.domain.Tag
import com.lab.onlineqna.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
    private val tagRepository: TagRepository
) {

    @Transactional
    fun resolveTags(names: Set<String>): Set<Tag> {
        if (names.isEmpty()) {
            return emptySet()
        }
        val normalized = names.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val existing = tagRepository.findAllByNameIn(normalized).associateBy { it.name }
        val missing = normalized
            .filterNot(existing::containsKey)
            .map { Tag(name = it) }
            .map(tagRepository::save)

        return (existing.values + missing).toSet()
    }
}
