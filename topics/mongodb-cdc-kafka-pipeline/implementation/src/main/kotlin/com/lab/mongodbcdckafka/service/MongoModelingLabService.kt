package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.ModelingProfile
import com.lab.mongodbcdckafka.domain.MongoModelingStrategy
import org.springframework.stereotype.Service

@Service
class MongoModelingLabService(
    private val mongoModelingDecisionService: MongoModelingDecisionService,
) {

    fun decide(request: ModelingDecisionRequest): ModelingDecisionResponse {
        val profile = ModelingProfile(
            readTogether = request.readTogether,
            updateTogether = request.updateTogether,
            boundedCardinality = request.boundedCardinality,
            independentlyQueriedChildren = request.independentlyQueriedChildren,
        )

        val strategy = mongoModelingDecisionService.chooseStrategy(profile)
        return ModelingDecisionResponse(
            strategy = strategy,
            reason = mongoModelingDecisionService.explain(profile),
        )
    }
}

data class ModelingDecisionRequest(
    val readTogether: Boolean,
    val updateTogether: Boolean,
    val boundedCardinality: Boolean,
    val independentlyQueriedChildren: Boolean,
)

data class ModelingDecisionResponse(
    val strategy: MongoModelingStrategy,
    val reason: String,
)

