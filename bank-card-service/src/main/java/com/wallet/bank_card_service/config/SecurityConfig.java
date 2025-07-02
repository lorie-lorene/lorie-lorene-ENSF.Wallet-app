package com.wallet.bank_card_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 🔐 Security Configuration for Bank Card Service
 * Fixed CORS configuration to allow frontend requests
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    /**
     * ✅ MAIN Security Filter Chain with CORS enabled
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("🔧 Configuring Bank Card Service Security with CORS...");
        
        http
            // ✅ CRITICAL: Enable CORS FIRST
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ✅ Disable CSRF for REST API
            .csrf(csrf -> csrf.disable())
            
            // ✅ Session management: stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // ✅ Authorization rules
            .authorizeHttpRequests(auth -> auth
                // CRITICAL: Allow OPTIONS requests first (preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/v*/admin/**").permitAll()
                .requestMatchers("/api/v1/cartes/admin/all").permitAll()
                .requestMatchers("/api/v1/cartes//admin/{idCarte}/admin-block").permitAll()
                
                // Public card endpoints
                .requestMatchers("/api/v1/cartes/my-cards").permitAll()
              //  .requestMatchers(HttpMethod.POST, "/api/v1/cartes/create").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/cartes/recharge-orange-money/{idCarte}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/cartes/{idCarte}/withdraw-to-mobile-money").permitAll()
                
                // Webhook endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/cartes/webhooks/money-callback").permitAll()
                
                // Health check
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // Swagger/OpenAPI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            );

        log.info("✅ Bank Card Service Security configured successfully");
        return http.build();
    }

    /**
     * ✅ CORS Configuration Source - PERMISSIVE for development
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("🌐 Configuring CORS for Bank Card Service...");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ Allow all origins (permissive for development)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // ✅ Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // ✅ Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // ✅ Allow credentials
        configuration.setAllowCredentials(true);
        
        // ✅ Cache preflight for 1 hour
        configuration.setMaxAge(3600L);
        
        // ✅ Apply to ALL paths (not just /api/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("✅ CORS configured permissively for development");
        return source;
    }

    /**
     * ✅ In-memory users for testing
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails client = User.builder()
            .username("client")
            .password(passwordEncoder().encode("password"))
            .roles("CLIENT")
            .build();

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(client, admin);
    }

    /**
     * ✅ Password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}