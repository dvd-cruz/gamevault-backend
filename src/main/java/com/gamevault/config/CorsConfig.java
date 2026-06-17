package com.gamevault.config;

import com.gamevault.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final FileStorageService storage;

    /** Comma-separated list of allowed origins. Override in prod, e.g. app.cors.allowed-origins=https://gamevault.cc */
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    public CorsConfig(FileStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only needed in disk mode; in R2 mode images are served from R2's public URL.
        java.nio.file.Path dir = storage.getUploadsDir();
        if (dir != null) {
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(dir.toUri().toString())
                    .setCachePeriod(60 * 60 * 24 * 365);
        }
    }
}
