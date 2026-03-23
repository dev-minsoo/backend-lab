package com.lab.onlineqna.service

import com.lab.onlineqna.dto.QuestionSearchDocument
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "true", matchIfMissing = true)
class ElasticSearchIndexService(
    private val operations: ElasticsearchOperations
) : SearchIndexService {

    private val indexCoordinates = IndexCoordinates.of("questions")

    override fun upsert(document: QuestionSearchDocument) {
        operations.save(document, indexCoordinates)
    }

    override fun delete(questionId: Long) {
        operations.delete(questionId.toString(), indexCoordinates)
    }

    override fun search(keyword: String?, tags: List<String>): List<QuestionSearchDocument> {
        val boolQuery = co.elastic.clients.elasticsearch._types.query_dsl.Query.of { root ->
            root.bool { bool ->
                if (!keyword.isNullOrBlank()) {
                    bool.must {
                        it.multiMatch { query ->
                            query.query(keyword).fields("title", "content", "authorNickname", "tags")
                        }
                    }
                }
                tags.forEach { tag ->
                    bool.filter {
                        it.term { term ->
                            term.field("tags.keyword").value(tag)
                        }
                    }
                }
                bool
            }
        }

        val query = NativeQuery.builder()
            .withQuery(boolQuery)
            .withPageable(PageRequest.of(0, 20))
            .build()

        return operations.search(query, QuestionSearchDocument::class.java, indexCoordinates)
            .searchHits
            .map { it.content }
    }
}
