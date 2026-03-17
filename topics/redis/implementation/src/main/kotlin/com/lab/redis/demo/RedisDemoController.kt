package com.lab.redis.demo

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/demo/redis")
class RedisDemoController {
    @GetMapping
    fun page(): String = "redis-demo"
}
