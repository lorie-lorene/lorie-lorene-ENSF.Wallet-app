package com.wallet.bank_card_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable()) // Disable CSRF for API
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/v*/admin/**").permitAll() // Allow admin
                                                // endpoints
                                                // temporarily
                                                .requestMatchers("/api/v1/cartes/admin/all").permitAll() // Public
                                                .requestMatchers("/api/v1/cartes//admin/{idCarte}/admin-block")
                                                .permitAll()
                                                .requestMatchers(
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/cartes/{idCarte}/recharge-orange-money",
                                                                                "POST"))
                                                .permitAll()
                                                .requestMatchers(
                                                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                                                "/api/v1/cartes/webhooks/money-callback",
                                                                                "POST"))
                                                .permitAll()

                                                .requestMatchers("/actuator/health").permitAll() // Health check
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                return http.build();
        }

        // NOUVEAU : Utilisateurs de test
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

        // NOUVEAU : Encodeur de mots de passe
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE",
                                "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .anyRequest().permitAll() // Permet tous les accès sans authentification
                                );

                return http.build();
        }
}