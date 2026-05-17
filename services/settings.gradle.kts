pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "nihongo-it"

include(
    "common",
    "api-gateway",
    "eureka-server",
    "learning-service",
    "ai-service",
    "notification-service",
    "user-service",
)

project(":notification-service").projectDir = file("notification")
