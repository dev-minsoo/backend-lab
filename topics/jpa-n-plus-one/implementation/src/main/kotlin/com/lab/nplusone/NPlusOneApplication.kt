package com.lab.nplusone

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NPlusOneApplication

fun main(args: Array<String>) {
    runApplication<NPlusOneApplication>(*args)
}
