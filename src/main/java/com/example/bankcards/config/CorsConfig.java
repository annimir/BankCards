package com.example.bankcards.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Конфигурация CORS (Cross-Origin Resource Sharing).
 *
 * <p>Разрешает запросы с фронтенд-приложений, перечисленных в {@code cors.allowed-origins}.
 * В дев-окружении по умолчанию разрешён {@code localhost:3000} и {@code localhost:5173}.
 *
 * <p>В продакшене необходимо задать переменную окружения {@code CORS_ALLOWED_ORIGINS}
 * со списком реальных доменов через запятую.
 *
 * <p>Сама конфигурация подключается в {@link SecurityConfig#securityFilterChain}
 * через {@code .cors(cors -> cors.configurationSource(corsConfigurationSource()))}.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-Trace-Id"
        ));
        config.setExposedHeaders(List.of(
                "X-Trace-Id"          // клиент может читать traceId из ответа
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);      // кэш preflight на 1 час

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}