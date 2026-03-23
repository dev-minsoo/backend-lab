package com.lab.onlineqna.service

import com.lab.onlineqna.dto.QuestionSearchDocument
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "false")
class InMemorySearchIndexService : SearchIndexService {

    private val store = ConcurrentHashMap<Long, QuestionSearchDocument>()

    override fun upsert(document: QuestionSearchDocument) {
        store[document.id] = document
    }

    override fun delete(questionId: Long) {
        store.remove(questionId)
    }

    override fun search(keyword: String?, tags: List<String>): List<QuestionSearchDocument> {
        return store.values
            .filter { document ->
                val matchesKeyword = keyword.isNullOrBlank() || listOf(
                    document.title,
                    document.content,
                    document.authorNickname,
                    document.tags.joinToString(" ")
                ).any { it.contains(keyword, ignoreCase = true) }
                val matchesTags = tags.all { document.tags.contains(it) }
                matchesKeyword && matchesTags
            }
            .sortedByDescending { it.createdAt }
    }
}
