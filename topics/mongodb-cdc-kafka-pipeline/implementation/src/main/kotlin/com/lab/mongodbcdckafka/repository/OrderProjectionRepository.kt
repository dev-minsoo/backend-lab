package com.lab.mongodbcdckafka.repository

import com.lab.mongodbcdckafka.domain.OrderProjectionDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface OrderProjectionRepository : MongoRepository<OrderProjectionDocument, String>

