package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        System.out.println("🌐 CORS Filter initialized!");
        CorsConfiguration config = new CorsConfiguration();

        // Permite request-uri de la frontend (Vite dev server)
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:5174"
        ));

        // Permite toate metodele HTTP
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Permite toate header-ele
        config.setAllowedHeaders(Arrays.asList("*"));

        // Permite trimiterea de credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Expune header-ele în răspuns
        config.setExposedHeaders(Arrays.asList("Authorization"));

        // Cât timp browser-ul cache-ază preflight request-ul (în secunde)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}