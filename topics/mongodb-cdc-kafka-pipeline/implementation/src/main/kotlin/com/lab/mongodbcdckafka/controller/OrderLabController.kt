package com.lab.mongodbcdckafka.controller

import com.lab.mongodbcdckafka.domain.OrderProjectionDocument
import com.lab.mongodbcdckafka.service.OrderEventPublisherService
import com.lab.mongodbcdckafka.service.OrderProjectionService
import com.lab.mongodbcdckafka.service.PublishOrderEventRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/lab/orders")
class OrderLabController(
    private val orderEventPublisherService: OrderEventPublisherService,
    private val orderProjectionService: OrderProjectionService,
) {

    @PostMapping("/events")
    fun publish(@RequestBody request: PublishOrderEventRequest) =
        orderEventPublisherService.publish(request)

    @GetMapping("/{aggregateId}/projection")
    fun findProjection(@PathVariable aggregateId: String): OrderProjectionDocument? =
        orderProjectionService.findProjection(aggregateId)

    @GetMapping("/projections")
    fun findAll(): List<OrderProjectionDocument> =
        orderProjectionService.findAll()
}
