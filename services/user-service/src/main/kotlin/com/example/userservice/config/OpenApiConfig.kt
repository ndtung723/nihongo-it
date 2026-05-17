package com.example.userservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"

        return OpenAPI()
            .info(
                Info()
                    .title("Japanese IT Learning API")
                    .description("Backend API for Japanese IT Learning application")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Development Team")
                            .email("admin@example.com"),
                    ).license(
                        License()
                            .name("MIT License"),
                    ),
            ).addServersItem(
                Server()
                    .url("/")
                    .description("Default Server URL"),
            ).addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Enter JWT Bearer token. Get it from the /api/v1/auth/login endpoint."),
                    ),
            )
    }
}
