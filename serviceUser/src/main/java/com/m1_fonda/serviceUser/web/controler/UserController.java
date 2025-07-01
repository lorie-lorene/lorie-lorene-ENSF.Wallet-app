package com.m1_fonda.serviceUser.web.controler;

import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.*;
import com.m1_fonda.serviceUser.response.*;
import com.m1_fonda.serviceUser.service.AuthenticationService;
import com.m1_fonda.serviceUser.service.JwtService;
import com.m1_fonda.serviceUser.service.UserService;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 🏦 User Controller - Complete REST API for User Management
 * Handles authentication, registration, profile management, and financial operations
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User authentication, registration, and profile management")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final UserServiceRabbit userServiceRabbit;
    private final UserRepository userRepository;

    // =====================================
    // 🔐 AUTHENTICATION ENDPOINTS
    // =====================================

    /**
     * User login/authentication
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email/phone and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked"),
        @ApiResponse(responseCode = "403", description = "Account not active")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, BindingResult result) {
        try {
            // Validate request
            if (result.hasErrors()) {
                return ResponseEntity.badRequest()
                    .body(createValidationErrorResponse(result));
            }

            log.info("Login attempt for identifier: {}", loginRequest.getIdentifier());

            LoginResponse response = authenticationService.authenticate(loginRequest);
            
            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                    .error("AUTHENTICATION_FAILED")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/users/login")
                    .build());
        } catch (Exception e) {
            log.error("Unexpected login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("Login failed due to server error")
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/users/login")
                    .build());
        }
    }

    /**
     * Refresh authentication token
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authenticationService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                    .error("TOKEN_REFRESH_FAILED")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/users/refresh-token")
                    .build());
        }
    }

    /**
     * User logout
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = jwtService.extractTokenFromHeader(authHeader);
            
            if (token != null) {
                Map<String, Object> response = authenticationService.logout(token);
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(Map.of("message", "Logout successful"));

        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("message", "Logout completed"));
        }
    }

    /**
     * Check authentication status
     */
    @GetMapping("/auth-status")
    @Operation(summary = "Check authentication status", description = "Verify if user token is valid")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AuthStatusResponse> checkAuthStatus(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = jwtService.extractTokenFromHeader(authHeader);
            
            if (token == null || !jwtService.isTokenValid(token)) {
                return ResponseEntity.ok(AuthStatusResponse.builder()
                    .authenticated(false)
                    .tokenExpired(true)
                    .build());
            }

            String clientId = jwtService.extractClientId(token);
            String email = jwtService.extractSubject(token);
            String status = jwtService.extractStatus(token);
            
            Optional<Client> clientOpt = userRepository.findById(clientId);
            
            AuthStatusResponse response = AuthStatusResponse.builder()
                .authenticated(true)
                .clientId(clientId)
                .email(email)
                .status(status)
                .isKycVerified(ClientStatus.ACTIVE.toString().equals(status))
                .tokenExpired(false)
                .lastLogin(clientOpt.map(Client::getLastLogin).orElse(null))
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Auth status check failed: {}", e.getMessage());
            return ResponseEntity.ok(AuthStatusResponse.builder()
                .authenticated(false)
                .tokenExpired(true)
                .build());
        }
    }

    // =====================================
    // 👤 REGISTRATION ENDPOINTS (Public)
    // =====================================

    /**
     * Register new user
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register new user", description = "Register a new user account with image uploads")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Registration submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid registration data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<?> register(
            @Valid @ModelAttribute ClientRegistrationDTO registration,
            @RequestParam("images") MultipartFile[] images,
            BindingResult result) {
        
        try {
            // Validate request
            if (result.hasErrors()) {
                return ResponseEntity.badRequest()
                    .body(createValidationErrorResponse(result));
            }

            // Validate images
            if (images == null || images.length != 3) {
                return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                        .error("INVALID_IMAGES")
                        .message("Exactly 3 images required: recto, verso, selfie")
                        .timestamp(LocalDateTime.now())
                        .path("/api/v1/users/register")
                        .build());
            }

            // Validate image types
            for (MultipartFile image : images) {
                if (!isValidImageType(image)) {
                    return ResponseEntity.badRequest()
                        .body(ErrorResponse.builder()
                            .error("INVALID_IMAGE_TYPE")
                            .message("Only PNG, JPG, JPEG images are allowed")
                            .timestamp(LocalDateTime.now())
                            .path("/api/v1/users/register")
                            .build());
                }
            }

            log.info("Registration attempt for email: {} with CNI: {}", 
                    registration.getEmail(), registration.getCni());

            // Save images and set paths in DTO
            saveImagesAndSetPaths(registration, images);

            // Process registration
            RegisterResponse response = userService.registerClient(registration);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (BusinessValidationException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                    .error("REGISTRATION_FAILED")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/users/register")
                    .build());
        } catch (IOException e) {
            log.error("File upload error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("FILE_UPLOAD_ERROR")
                    .message("Failed to save uploaded images")
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/users/register")
                    .build());
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("Registration failed due to server error")
                    .timestamp(LocalDateTime.now())
                    .path("/api/v1/users/register")
                    .build());
        }
    }

    private boolean isValidImageType(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }

        log.info("file", file);
        
        // String contentType = file.getContentType();
        // log.info("contentType:", contentType);
        // return contentType != null && (
        //     contentType.equals("image/png") ||
        //     contentType.equals("image/jpeg") ||
        //     contentType.equals("image/jpg")
        // );

        return true;
    }

    private void saveImagesAndSetPaths(ClientRegistrationDTO registration, MultipartFile[] images) throws IOException {
        String cni = registration.getCni();
        String baseUploadPath = "uploads/cni"; // Configure this path in application.properties
        
        // Create CNI directory
        Path cniDir = Paths.get(baseUploadPath, cni);
        Files.createDirectories(cniDir);
        
        // Save images in order: recto, verso, selfie
        String[] imageTypes = {"recto", "verso", "selfie"};
        
        for (int i = 0; i < images.length && i < imageTypes.length; i++) {
            MultipartFile image = images[i];
            String extension = getFileExtension(image.getOriginalFilename());
            log.info("extension", extension);
            String filename = cni + "_" + imageTypes[i] + extension;
            
            Path filePath = cniDir.resolve(filename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Set the relative path in DTO
            String relativePath = cni + "/" + filename;
            
            switch (i) {
                case 0:
                    registration.setRectoCni(relativePath);
                    break;
                case 1:
                    registration.setVersoCni(relativePath);
                    break;
                case 2:
                    registration.setSelfieImage(relativePath);
                    break;
            }
        }
        
        log.info("Images saved successfully for CNI: {}", cni);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".png"; // default extension
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : ".png";
    }

    /**
     * Check registration status
     */
    @GetMapping("/registration-status")
    @Operation(summary = "Check registration status", description = "Check the status of a user registration")
    public ResponseEntity<?> checkRegistrationStatus(@RequestParam @Email String email) {
        try {
            Optional<Client> client = userService.findByEmail(email);

            if (client.isPresent()) {
                RegistrationStatusResponse response = new RegistrationStatusResponse(
                        client.get().getStatus().toString(),
                        getStatusMessage(client.get().getStatus()),
                        client.get().getCreatedAt());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RegistrationStatusResponse("NOT_FOUND", "No registration found for this email", null));
            }

        } catch (Exception e) {
            log.error("Error checking registration status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RegistrationStatusResponse("ERROR", "Technical error occurred", null));
        }
    }

    // =====================================
    // 🔒 PROFILE MANAGEMENT (Authenticated)
    // =====================================

    /**
     * Get user profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get user profile", description = "Get authenticated user's profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String clientEmail = extractClientId(authentication);

            Optional<Client> client = userService.findByEmail(clientEmail);

            log.info("Retrieving profile for client ID: {}", clientEmail);
            log.info("CLient : {}", client);

            if (client.isPresent()) {
                ClientProfileResponse profile = mapToProfileResponse(client.get());
                log.info("profile : {}", profile);
                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                        .error("USER_NOT_FOUND")
                        .message("User profile not found")
                        .timestamp(LocalDateTime.now())
                        .build());
            }

        } catch (Exception e) {
            log.error("Error retrieving profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("Failed to retrieve profile")
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Update user profile", description = "Update user profile information")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {

        try {
            // Verify user can only update their own profile
            String authenticatedClientId = extractClientId(authentication);
            if (!authenticatedClientId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.builder()
                        .error("ACCESS_DENIED")
                        .message("You can only update your own profile")
                        .timestamp(LocalDateTime.now())
                        .build());
            }

            Client updatedClient = userService.updateProfile(id, request);
            ClientProfileResponse response = mapToProfileResponse(updatedClient);
            
            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Profile update error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("Profile update failed")
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Change password", description = "Change user password")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        try {
            // Validate password confirmation
            if (!request.isPasswordConfirmed()) {
                return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                        .error("PASSWORD_MISMATCH")
                        .message("New password and confirmation do not match")
                        .timestamp(LocalDateTime.now())
                        .build());
            }

            String clientId = extractClientId(authentication);
            
            Map<String, Object> response = authenticationService.changePassword(
                clientId,
                request.getCurrentPassword(),
                request.getNewPassword()
            );
            
            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                    .error("PASSWORD_CHANGE_FAILED")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Password change error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("Password change failed")
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    // =====================================
    // 💰 FINANCIAL OPERATIONS (Authenticated)
    // =====================================

    /**
     * Make a deposit
     */
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Make a deposit", description = "Deposit money to account")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("Deposit request: {} FCFA to account {} by client {}",
                    request.getMontant(), request.getNumeroCompte(), clientId);

            TransactionResponse response = userServiceRabbit.sendDepot(request, clientId);

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(null, "REJECTED", e.getMessage(), null, LocalDateTime.now()));

        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TransactionResponse(null, "ERROR", "Service unavailable", null, LocalDateTime.now()));
        }
    }

    /**
     * Make a withdrawal
     */
    @PostMapping("/withdrawal")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Make a withdrawal", description = "Withdraw money from account")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TransactionResponse> withdrawal(
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("Withdrawal request: {} FCFA from account {} by client {}",
                    request.getMontant(), request.getNumeroCompte(), clientId);

            TransactionResponse response = userServiceRabbit.sendRetrait(request, clientId);

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(null, "REJECTED", e.getMessage(), null, LocalDateTime.now()));

        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TransactionResponse(null, "ERROR", "Service unavailable", null, LocalDateTime.now()));
        }
    }

    /**
     * Make a transfer
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Make a transfer", description = "Transfer money between accounts")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("Transfer request: {} FCFA from {} to {} by client {}",
                    request.getMontant(), request.getNumeroCompteSend(),
                    request.getNumeroCompteReceive(), clientId);

            TransactionResponse response = userServiceRabbit.sendTransaction(request, clientId);

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(null, "REJECTED", e.getMessage(), null, LocalDateTime.now()));

        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TransactionResponse(null, "ERROR", "Service unavailable", null, LocalDateTime.now()));
        }
    }

    // =====================================
    // 🔐 PASSWORD RESET (Public)
    // =====================================

    /**
     * Request password reset
     */
    @PostMapping("/password-reset/request")
    @Operation(summary = "Request password reset", description = "Request password reset for user account")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            PasswordResetResponse response = userService.requestPasswordReset(request);
            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                    .error("PASSWORD_RESET_FAILED")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("Password reset request failed")
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    // =====================================
    // 📊 ADMIN ENDPOINTS (Admin Only)
    // =====================================

    /**
     * Search clients (Admin only)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search clients", description = "Search for clients by term")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ClientProfileResponse>> searchClients(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            List<Client> clients = userService.searchClients(searchTerm, page, size);
            List<ClientProfileResponse> response = clients.stream()
                    .map(this::mapToProfileResponse)
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Client search error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get client statistics (Admin only)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get client statistics", description = "Get overall client statistics")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ClientStatisticsResponse> getStatistics() {
        try {
            ClientStatisticsResponse stats = userService.getClientStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Statistics error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Unlock user account (Admin only)
     */
    @PostMapping("/{clientId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user account", description = "Unlock a locked user account")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> unlockAccount(@PathVariable String clientId) {
        try {
            Map<String, Object> response = authenticationService.unlockAccount(clientId);
            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                    .error("UNLOCK_FAILED")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    // =====================================
    // 🛠️ UTILITY METHODS
    // =====================================

    /**
     * Extract client ID from authentication context
     */
    private String extractClientId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new BusinessValidationException("Invalid authentication context");
    }

    /**
     * Map Client entity to Profile Response DTO
     */
    private ClientProfileResponse mapToProfileResponse(Client client) {
        return ClientProfileResponse.builder()
                .idClient(client.getIdClient())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .email(client.getEmail())
                .numero(client.getNumero())
                .status(client.getStatus().toString())
                .createdAt(client.getCreatedAt())
                .lastLogin(client.getLastLogin())
                .isKycVerified(client.getStatus() == ClientStatus.ACTIVE)
                .build();
    }

    /**
     * Get status message for client status
     */
    private String getStatusMessage(ClientStatus status) {
        return switch (status) {
            case PENDING -> "Your registration is being processed";
            case ACTIVE -> "Your account is active";
            case REJECTED -> "Your registration was rejected";
            case BLOCKED -> "Your account is blocked";
            case SUSPENDED -> "Your account is temporarily suspended";
        };
    }

    /**
     * Create validation error response
     */
    private ValidationErrorResponse createValidationErrorResponse(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));

        return ValidationErrorResponse.builder()
                .message("Validation failed")
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}