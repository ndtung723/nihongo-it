package com.example.learningservice.security

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PreAuthFilter(
    val hasRole: String = "", // Vai trò bắt buộc (user hoặc admin)
    val hasAnyRole: Array<String> = [], // Danh sách vai trò cho phép (user, admin)
)
