package com.serviceDemande.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/demande/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/demande/dashboard").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers("/api/v1/demande/manual-review/**").hasAnyRole("SUPERVISOR", "ADMIN")

                        // ✅ CORRECTION : Pattern spécifique pour les limits
                        .requestMatchers("/api/v1/demande/*/limits").hasRole("ADMIN") // Un seul segment
                        // OU si vous voulez matcher plusieurs niveaux :
                        // .requestMatchers("/api/v1/demande/{demandeId}/limits").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .httpBasic();

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails supervisor = User.builder()
                .username("supervisor")
                .password(passwordEncoder().encode("supervisor123"))
                .roles("SUPERVISOR")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "SUPERVISOR")
                .build();

        return new InMemoryUserDetailsManager(supervisor, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}