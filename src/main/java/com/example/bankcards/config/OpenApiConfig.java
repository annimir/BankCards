package com.example.bankcards.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация SpringDoc OpenAPI / Swagger UI.
 *
 * <p>Определяет метаданные API (название, версия, описание) и схему аутентификации
 * {@code bearerAuth} — позволяет вводить JWT-токен прямо в Swagger UI через кнопку Authorize.
 *
 * <p>Доступно на {@code /swagger-ui.html} и {@code /v3/api-docs}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Bank Cards Management API",
                version = "1.0",
                description = "REST API for managing bank cards with JWT authentication",
                contact = @Contact(name = "BankCards Team", email = "dev@bankcards.com")
        ),
        servers = @Server(url = "http://localhost:8080", description = "Local Dev Server")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
 