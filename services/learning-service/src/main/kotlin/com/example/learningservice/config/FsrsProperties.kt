package com.example.learningservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.fsrs")
data class FsrsProperties(
    val weights: List<Double> =
        listOf(
            0.4,
            0.6,
            2.4,
            5.8,
            4.93,
            0.94,
            0.86,
            0.01,
            1.49,
            0.14,
            0.94,
            2.18,
            0.05,
            0.34,
            1.26,
            0.29,
            2.61,
        ),
    val requestRetention: Double = 0.9,
    val maximumInterval: Double = 36500.0,
)
