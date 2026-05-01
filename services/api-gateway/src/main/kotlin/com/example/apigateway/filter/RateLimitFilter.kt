package com.example.apigateway.filter

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private data class RateLimitRule(val pathPrefix: String, val capacity: Long, val refillPeriod: Duration)

private data class BucketEntry(val bucket: Bucket, @Volatile var lastAccess: Instant = Instant.now())

@Component
class RateLimitFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)

    private val rules = listOf(
        RateLimitRule("/api/v1/user/auth/login",           5,  Duration.ofMinutes(1)),
        RateLimitRule("/api/v1/user/auth/forgot-password", 3,  Duration.ofMinutes(5)),
        RateLimitRule("/api/v1/user/auth/refresh-token",   10, Duration.ofMinutes(1)),
        RateLimitRule("/api/v1/ai/speech/",                10, Duration.ofMinutes(1)),
        RateLimitRule("/api/v1/ai/",                       30, Duration.ofMinutes(1)),
    )

    private val buckets = ConcurrentHashMap<String, BucketEntry>()

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        val rule = rules.firstOrNull { path.startsWith(it.pathPrefix) } ?: return chain.filter(exchange)

        val key = buildKey(exchange, rule)
        val entry = buckets.computeIfAbsent(key) { newEntry(rule) }
        entry.lastAccess = Instant.now()

        return if (entry.bucket.tryConsume(1)) {
            chain.filter(exchange)
        } else {
            logger.warn("Rate limit exceeded: key=$key path=$path")
            tooManyRequests(exchange, rule.refillPeriod)
        }
    }

    private fun buildKey(exchange: ServerWebExchange, rule: RateLimitRule): String {
        val userId = exchange.request.headers.getFirst("X-User-Id")
        val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
        return "${rule.pathPrefix}:${userId ?: ip}"
    }

    private fun newEntry(rule: RateLimitRule): BucketEntry {
        val bucket = Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(rule.capacity).refillGreedy(rule.capacity, rule.refillPeriod).build())
            .build()
        return BucketEntry(bucket)
    }

    private fun tooManyRequests(exchange: ServerWebExchange, refillPeriod: Duration): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.add("Content-Type", "application/json")
        response.headers.add("Retry-After", refillPeriod.seconds.toString())
        val body = """{"status":"NG","message":"Too many requests. Please try again later."}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    @Scheduled(fixedDelay = 3_600_000)
    fun evictStaleBuckets() {
        val cutoff = Instant.now().minus(Duration.ofMinutes(30))
        val removed = buckets.entries.removeIf { it.value.lastAccess.isBefore(cutoff) }
        if (removed) logger.debug("Evicted stale rate-limit buckets")
    }

    override fun getOrder(): Int = -50
}
