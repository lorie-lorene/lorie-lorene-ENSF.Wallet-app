package com.Authentication.authService.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Authentication.authService.dto.AuthRequest;
import com.Authentication.authService.dto.AuthResponse;
import com.Authentication.authService.dto.RegisterRequest;
import com.Authentication.authService.model.Role;
import com.Authentication.authService.model.User;
import com.Authentication.authService.repository.UserRepository;
import com.Authentication.authService.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER); // default role

        User savedUser = userRepository.save(user);
        
        String token = jwtTokenProvider.generateToken(savedUser);
        
        return new AuthResponse(token, savedUser.getEmail(), savedUser.getRole().name());
    }
}