package com.m1_fonda.serviceUser.service;

import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.LoginRequest;
import com.m1_fonda.serviceUser.response.LoginResponse;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 🔐 Authentication Service
 * Handles user login, password verification, and session management
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    // Security constants
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    /**
     * Authenticate user with email/phone and password
     */
    public LoginResponse authenticate(LoginRequest loginRequest) {
        log.info("Authentication attempt for identifier: {}", loginRequest.getIdentifier());

        try {
            log.info("Login request details: {}", loginRequest);
            // Find user by email or phone number
            Optional<Client> clientOpt = findUserByIdentifier(loginRequest.getIdentifier());

            log.info("User found: {}", clientOpt.get().getNom());
            log.info("isEmpty?: {}", clientOpt.isEmpty());

            if (clientOpt.isEmpty()) {
                log.info("No user found with identifier: {}", loginRequest.getIdentifier());
                recordFailedLoginAttempt(loginRequest.getIdentifier());
                throw new BusinessValidationException("Invalid credentials");
            }

            Client client = clientOpt.get();
            log.info("client found: {}", client.getNom());
            // Check if account is locked
            if (isAccountLocked(client)) {
                throw new BusinessValidationException(
                    String.format("Account locked due to too many failed attempts. Try again after %d minutes.", 
                                LOCKOUT_DURATION_MINUTES));
            }

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword() + client.getSalt(), client.getPasswordHash())) {
                log.info("Invalid password for user: {}", client.getEmail());
                recordFailedLoginAttempt(client);
                throw new BusinessValidationException("Invalid credentials");
            }

            // Successful authentication
            recordSuccessfulLogin(client);
            
            // Generate tokens
            String accessToken = jwtService.generateToken(
                client.getIdClient(),
                client.getEmail(),
                client.getNumero(),
                client.getStatus().toString()
            );
            
            String refreshToken = jwtService.generateRefreshToken(client.getEmail());

            log.info("Authentication successful for client: {}", client.getIdClient());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400) // 24 hours
                    .clientId(client.getIdClient())
                    .email(client.getEmail())
                    .status(client.getStatus().toString())
                    .isKycVerified(client.getStatus() == ClientStatus.ACTIVE)
                    .lastLogin(client.getLastLogin())
                    .build();

        } catch (BusinessValidationException e) {
            log.warn("Authentication failed for {}: {}", loginRequest.getIdentifier(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication for {}: {}", 
                     loginRequest.getIdentifier(), e.getMessage(), e);
            throw new BusinessValidationException("Authentication failed");
        }
    }

    /**
     * Refresh authentication token
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            if (!jwtService.isTokenValid(refreshToken)) {
                throw new BusinessValidationException("Invalid refresh token");
            }

            String email = jwtService.extractSubject(refreshToken);
            Optional<Client> clientOpt = userRepository.findByEmail(email);

            if (clientOpt.isEmpty()) {
                throw new BusinessValidationException("User not found");
            }

            Client client = clientOpt.get();
            validateAccountStatus(client);

            // Generate new tokens
            String newAccessToken = jwtService.generateToken(
                client.getIdClient(),
                client.getEmail(),
                client.getNumero(),
                client.getStatus().toString()
            );
            
            String newRefreshToken = jwtService.generateRefreshToken(client.getEmail());

            log.info("Token refreshed for client: {}", client.getIdClient());

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400)
                    .clientId(client.getIdClient())
                    .email(client.getEmail())
                    .status(client.getStatus().toString())
                    .isKycVerified(client.getStatus() == ClientStatus.ACTIVE)
                    .lastLogin(client.getLastLogin())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BusinessValidationException("Token refresh failed");
        }
    }

    /**
     * Validate user account status
     */
    private void validateAccountStatus(Client client) {
        switch (client.getStatus()) {
            case PENDING:
                throw new BusinessValidationException("Account pending approval. Please wait for verification.");
            case REJECTED:
                throw new BusinessValidationException("Account rejected. Please contact support or register again.");
            case BLOCKED:
                throw new BusinessValidationException("Account blocked. Please contact customer support.");
            case SUSPENDED:
                throw new BusinessValidationException("Account temporarily suspended. Please contact support.");
            case ACTIVE:
                // Account is active, proceed
                break;
            default:
                throw new BusinessValidationException("Invalid account status. Please contact support.");
        }
    }

    /**
     * Find user by email or phone number
     */
    private Optional<Client> findUserByIdentifier(String identifier) {
        if( identifier.contains("@")) {
            // Identifier is an email
            return userRepository.findByEmail(identifier);
        } else if( identifier.matches("\\d{10,15}")) {
            // Identifier is a phone number
            return userRepository.findByNumero(identifier);
        }
        return Optional.empty();
    }

    /**
     * Check if account is locked due to failed attempts
     */
    private boolean isAccountLocked(Client client) {
        if (client.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            LocalDateTime lockoutTime = client.getLastFailedLogin()
                    .plusMinutes(LOCKOUT_DURATION_MINUTES);
            return LocalDateTime.now().isBefore(lockoutTime);
        }
        return false;
    }

    /**
     * Record successful login
     */
    private void recordSuccessfulLogin(Client client) {
        client.setLastLogin(LocalDateTime.now());
        client.setLoginAttempts(0); // Reset failed attempts
        client.setLastFailedLogin(null);
        userRepository.save(client);
    }

    /**
     * Record failed login attempt
     */
    private void recordFailedLoginAttempt(Client client) {
        client.setLoginAttempts(client.getLoginAttempts() + 1);
        client.setLastFailedLogin(LocalDateTime.now());
        userRepository.save(client);
        
        log.warn("Failed login attempt #{} for client: {}", 
                client.getLoginAttempts(), client.getIdClient());
    }

    /**
     * Record failed login attempt by identifier (when user not found)
     */
    private void recordFailedLoginAttempt(String identifier) {
        log.warn("Failed login attempt for unknown identifier: {}", identifier);
        // You might want to implement additional security measures here
        // like rate limiting by IP address
    }

    /**
     * Logout user (invalidate token)
     * Note: In a stateless JWT system, logout is typically handled client-side
     * by removing the token. For server-side logout, you'd need a token blacklist.
     */
    public Map<String, Object> logout(String token) {
        try {
            String clientId = jwtService.extractClientId(token);
            log.info("Logout requested for client: {}", clientId);
            
            // Here you could add the token to a blacklist if needed
            // tokenBlacklistService.blacklistToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logout successful");
            response.put("timestamp", LocalDateTime.now());
            
            return response;
            
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new BusinessValidationException("Logout failed");
        }
    }

    /**
     * Change user password
     */
    @Transactional
    public Map<String, Object> changePassword(String clientId, String currentPassword, String newPassword) {
        try {
            Optional<Client> clientOpt = userRepository.findById(clientId);
            
            if (clientOpt.isEmpty()) {
                throw new BusinessValidationException("User not found");
            }

            Client client = clientOpt.get();

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, client.getPasswordHash())) {
                throw new BusinessValidationException("Current password is incorrect");
            }

            // Validate new password strength
            validatePasswordStrength(newPassword);

            // Update password
            client.setPasswordHash(passwordEncoder.encode(newPassword));
            client.setPasswordChangedAt(LocalDateTime.now());
            userRepository.save(client);

            log.info("Password changed successfully for client: {}", clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            response.put("timestamp", LocalDateTime.now());
            
            return response;

        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Password change failed for client {}: {}", clientId, e.getMessage());
            throw new BusinessValidationException("Password change failed");
        }
    }

    /**
     * Validate password strength
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessValidationException("Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessValidationException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessValidationException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new BusinessValidationException("Password must contain at least one number");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new BusinessValidationException("Password must contain at least one special character");
        }
    }

    /**
     * Unlock user account (admin function)
     */
    @Transactional
    public Map<String, Object> unlockAccount(String clientId) {
        try {
            Optional<Client> clientOpt = userRepository.findById(clientId);
            
            if (clientOpt.isEmpty()) {
                throw new BusinessValidationException("User not found");
            }

            Client client = clientOpt.get();
            client.setLoginAttempts(0);
            client.setLastFailedLogin(null);
            userRepository.save(client);

            log.info("Account unlocked for client: {}", clientId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Account unlocked successfully");
            response.put("clientId", clientId);
            response.put("timestamp", LocalDateTime.now());
            
            return response;

        } catch (Exception e) {
            log.error("Account unlock failed for client {}: {}", clientId, e.getMessage());
            throw new BusinessValidationException("Account unlock failed");
        }
    }
}