package com.example.common.security

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PreAuthFilter(
    val hasRole: String = "",
    val hasAnyRole: Array<String> = [],
)
