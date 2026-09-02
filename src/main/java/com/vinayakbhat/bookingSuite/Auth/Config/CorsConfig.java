package com.vinayakbhat.bookingSuite.Auth.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173/", "https://yourproductiondomain.com")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        // allowCredentials(true) is mandatory if you are passing HttpOnly cookies or Authorization headers
                        .allowCredentials(true)
                        .maxAge(3600); // Cache the preflight OPTIONS request for 1 hour
            }
        };
    }
}
