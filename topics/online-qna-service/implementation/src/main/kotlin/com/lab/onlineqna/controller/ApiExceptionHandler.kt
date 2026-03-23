package com.lab.onlineqna.controller

import com.lab.onlineqna.exception.DomainException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to ex.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class)
    fun handleValidationException(ex: Exception): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (ex.message ?: "Validation failed")))

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("message" to (ex.message ?: "Internal server error")))
}
