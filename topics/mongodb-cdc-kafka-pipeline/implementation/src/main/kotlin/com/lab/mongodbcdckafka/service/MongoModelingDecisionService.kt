package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.ModelingProfile
import com.lab.mongodbcdckafka.domain.MongoModelingStrategy
import org.springframework.stereotype.Service

@Service
class MongoModelingDecisionService {

    fun chooseStrategy(profile: ModelingProfile): MongoModelingStrategy {
        return if (
            profile.readTogether &&
            profile.updateTogether &&
            profile.boundedCardinality &&
            !profile.independentlyQueriedChildren
        ) {
            MongoModelingStrategy.EMBED
        } else {
            MongoModelingStrategy.REFERENCE
        }
    }

    fun explain(profile: ModelingProfile): String =
        when (chooseStrategy(profile)) {
            MongoModelingStrategy.EMBED ->
                "함께 읽고 함께 바꾸며 cardinality가 제한적이므로 Embed가 적합합니다."
            MongoModelingStrategy.REFERENCE ->
                "독립 조회 또는 큰 cardinality 가능성이 있어 Reference가 안전합니다."
        }
}
