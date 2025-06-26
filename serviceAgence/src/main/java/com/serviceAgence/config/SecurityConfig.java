package com.serviceAgence.config;

import com.serviceAgence.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuration de sécurité pour AgenceService
 * - JWT Authentication
 * - CORS pour mobile app et extensions navigateur
 * - Protection des endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        /**
         * Configuration principale de sécurité
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                log.info("🔧 Configuration sécurité AgenceService...");

                http
                                // Configuration CORS AVANT tout le reste - CRITIQUE
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Désactivation CSRF (API REST)
                                .csrf(csrf -> csrf.disable())

                                // Désactiver les headers de sécurité qui peuvent interférer avec CORS
                                .headers(headers -> headers
                                                .frameOptions().disable()
                                                .contentTypeOptions().disable())

                                // Gestion de session : STATELESS (JWT)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Configuration des autorisations
                                .authorizeHttpRequests(authz -> authz
                                                // OPTIONS requests DOIVENT être autorisées en PREMIER
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Endpoints publics
                                                .requestMatchers("/api/v1/agence/auth/**").permitAll()
                                                .requestMatchers("/api/v1/agence/getAgences").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/agence/add").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/agence/register").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/agence/contact").permitAll()
                                                .requestMatchers("/api/v1/agence/public/**").permitAll()

                                                .requestMatchers("/api/v1/agence/health").permitAll()
                                                .requestMatchers("/api/v1/agence/transactions").permitAll()
                                                .requestMatchers("/api/v1/agence/comptes/{numeroCompte}").permitAll()
                                                // test
                                                .requestMatchers("/api/v1/agence/createAccount").permitAll()

                                                // .requestMatchers("/api/v1/cartes/connectivity-test").permitAll()
                                                .requestMatchers("/api/v1/agence/endpoints").permitAll()
                                                .requestMatchers("/actuator/**").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                                // Endpoints admin uniquement
                                                .requestMatchers("/api/v1/agence/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/agence/*/statistics")
                                                .hasAnyRole("ADMIN", "SUPERVISOR")

                                                // Endpoints agence
                                                .requestMatchers("/api/v1/agence/comptes/**")
                                                .hasAnyRole("AGENCE", "ADMIN")
                                                .requestMatchers("/api/v1/agence/kyc/**").hasAnyRole("AGENCE", "ADMIN")
                                                .requestMatchers("/api/v1/agence/transactions/**")
                                                .hasAnyRole("AGENCE", "ADMIN", "CLIENT")

                                                // Tous les autres endpoints nécessitent une authentification
                                                .anyRequest().authenticated())

                                // Ajout du filtre JWT après la configuration CORS
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                log.info("✅ Sécurité AgenceService configurée avec succès");
                return http.build();
        }

        /**
         * Configuration CORS permissive pour développement
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // PERMISSIF pour développement - Autorise TOUTES les origines
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));

                // Toutes les méthodes HTTP
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

                // Tous les en-têtes autorisés
                configuration.setAllowedHeaders(Arrays.asList("*"));

                // En-têtes exposés au client
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With",
                                "Access-Control-Allow-Origin",
                                "Access-Control-Allow-Credentials",
                                "Access-Control-Allow-Headers",
                                "Access-Control-Allow-Methods"));

                // IMPORTANT: Autorisation des credentials
                configuration.setAllowCredentials(true);

                // Cache des pré-requêtes OPTIONS (1 heure)
                configuration.setMaxAge(3600L);

                // Appliquer à TOUS les endpoints
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration); // Changé de "/api/**" à "/**"

                log.info("🌐 CORS configuré de manière PERMISSIVE pour développement");
                return source;
        }

        /**
         * Encodeur de mot de passe BCrypt
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        /**
         * Authentication Manager pour login
         */
        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }
}