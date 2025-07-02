package com.wallet.bank_card_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.extern.slf4j.Slf4j;

/**
 * 🌐 Web MVC Configuration for Bank Card Service
 * Handles additional CORS configuration at MVC level
 * 
 * Note: This works together with SecurityConfig CORS configuration
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    /**
     * ✅ Global CORS configuration at MVC level
     * This complements the SecurityConfig CORS configuration
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("🌐 Configuring MVC-level CORS mappings...");
        
        registry.addMapping("/**")  // ✅ Changed from "/api/**" to "/**"
                .allowedOriginPatterns("*")  // ✅ Allow all origins for development
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        
        log.info("✅ MVC-level CORS mappings configured");
    }

    /**
     * ✅ Static resource handlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("📁 Configuring static resource handlers...");
        
        // Static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // Webjars (for Swagger UI, etc.)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
        
        log.info("✅ Static resource handlers configured");
    }
}