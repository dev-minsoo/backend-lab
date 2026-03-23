package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findAllByNameIn(names: Set<String>): List<Tag>
}
