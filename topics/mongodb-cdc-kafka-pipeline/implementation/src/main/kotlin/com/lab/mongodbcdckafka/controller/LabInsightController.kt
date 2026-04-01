package com.lab.mongodbcdckafka.controller

import com.lab.mongodbcdckafka.service.CdcVerificationLabService
import com.lab.mongodbcdckafka.service.CdcVerificationRequest
import com.lab.mongodbcdckafka.service.KafkaFailureSimulationRequest
import com.lab.mongodbcdckafka.service.KafkaFailureSimulationService
import com.lab.mongodbcdckafka.service.ModelingDecisionRequest
import com.lab.mongodbcdckafka.service.MongoModelingLabService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/lab")
class LabInsightController(
    private val cdcVerificationLabService: CdcVerificationLabService,
    private val mongoModelingLabService: MongoModelingLabService,
    private val kafkaFailureSimulationService: KafkaFailureSimulationService,
) {

    @PostMapping("/cdc/verify")
    fun verifyCdc(@RequestBody request: CdcVerificationRequest) =
        cdcVerificationLabService.verify(request)

    @PostMapping("/modeling/decision")
    fun decideModeling(@RequestBody request: ModelingDecisionRequest) =
        mongoModelingLabService.decide(request)

    @PostMapping("/kafka/failure-simulation")
    fun simulateFailure(@RequestBody request: KafkaFailureSimulationRequest) =
        kafkaFailureSimulationService.simulate(request)
}
