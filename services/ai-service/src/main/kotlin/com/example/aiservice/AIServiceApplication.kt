package com.example.aiservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableDiscoveryClient // Kích hoạt Service Discovery (Eureka Client)
@EnableFeignClients // Kích hoạt Feign Client để microservice có thể gọi đến nhau
@ComponentScan(basePackages = ["com.example.aiservice"])
class AIServiceApplication

fun main(args: Array<String>) {
    runApplication<AIServiceApplication>(*args)
}
