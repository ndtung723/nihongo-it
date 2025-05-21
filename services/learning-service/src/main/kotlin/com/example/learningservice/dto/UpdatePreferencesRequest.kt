package com.example.learningservice.dto

data class UpdatePreferencesRequest(
    val notificationPreferences: String? = null,
    val reminderEnabled: Boolean? = null,
    val reminderTime: String? = null,
    val minCardThreshold: Int? = null,
    val leechNotificationsEnabled: Boolean? = null
) 