package com.Authentication.authService.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.Authentication.authService.model.Role;
import com.Authentication.authService.model.User;
import com.Authentication.authService.repository.UserRepository;

@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create admin user if it doesn't exist
            if (!userRepository.existsByEmail("admin@example.com")) {
                User admin = new User();
                admin.setName("Admin User");
                admin.setEmail("admin@example.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ROLE_ADMIN);
                userRepository.save(admin);
            }

            // Create a test user if it doesn't exist
            if (!userRepository.existsByEmail("user@example.com")) {
                User user = new User();
                user.setName("Test User");
                user.setEmail("user@example.com");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setRole(Role.ROLE_USER);
                userRepository.save(user);
            }
        };
    }
}