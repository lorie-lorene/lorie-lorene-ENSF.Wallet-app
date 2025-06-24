package com.m1_fonda.serviceUser.service;

import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.PasswordResetRequest;
import com.m1_fonda.serviceUser.request.ProfileUpdateRequest;
import com.m1_fonda.serviceUser.response.ClientStatisticsResponse;
import com.m1_fonda.serviceUser.response.PasswordResetResponse;
import com.m1_fonda.serviceUser.response.RegisterResponse;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 🏦 User Service - Complete Implementation
 * Handles user registration, profile management, and business logic
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository repository;
    private final UserServiceRabbit rabbit;
    private final PasswordEncoder passwordEncoder;

    // =====================================
    // 👤 USER MANAGEMENT METHODS
    // =====================================

    /**
     * Register new client
     */
    public RegisterResponse registerClient(ClientRegistrationDTO registration) {
        log.info("Starting registration for email: {}", registration.getEmail());

        try {
            // Validate uniqueness
            validateUniqueness(registration.getEmail(), registration.getCni(), registration.getNumero());

            // Create new client
            Client client = createClientFromRegistration(registration);
            
            // Save to database
            Client savedClient = repository.save(client);
            log.info("Client saved with ID: {}", savedClient.getIdClient());

            // Send to AgenceService via RabbitMQ for validation
            try {
                rabbit.sendRegistrationEvent(savedClient);
                log.info("Registration sent to AgenceService for validation");
            } catch (Exception e) {
                log.error("Failed to send registration to AgenceService: {}", e.getMessage());
                // Continue with registration but log the error
            }

            return RegisterResponse.builder()
                    .status("PENDING")
                    .message("Your registration has been submitted and will be processed shortly")
                    .requestId(savedClient.getIdClient())
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (BusinessValidationException e) {
            log.warn("Registration validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            throw new ServiceException("Registration failed due to technical error");
        }
    }

    /**
     * Create Client entity from registration DTO
     */
    private Client createClientFromRegistration(ClientRegistrationDTO registration) {
        Client client = new Client();
        
        // Basic information
        client.setEmail(registration.getEmail().toLowerCase().trim());
        client.setNom(registration.getNom().toUpperCase().trim());
        client.setPrenom(registration.getPrenom().trim());
        client.setNumero(registration.getNumero().trim());
        client.setCni(registration.getCni().trim());
        client.setIdAgence(registration.getIdAgence());
        
        // Security
        String salt = generateSalt();
        client.setSalt(salt);
        client.setPasswordHash(passwordEncoder.encode(registration.getPassword() + salt));
        
        // Documents (if provided)
        if (registration.getRectoCni() != null) {
            client.setRectoCni(registration.getRectoCni());
        }
        if (registration.getVersoCni() != null) {
            client.setVersoCni(registration.getVersoCni());
        }
        
        // Metadata
        client.setCreatedAt(LocalDateTime.now());
        client.setStatus(ClientStatus.PENDING);
        client.setLoginAttempts(0);
        
        return client;
    }

    /**
     * Generate random salt for password security
     */
    private String generateSalt() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    /**
     * Validate uniqueness of email, CNI, and phone number
     */
    public void validateUniqueness(String email, String cni, String numero) {
        if (repository.findByEmail(email).isPresent()) {
            throw new BusinessValidationException("An account already exists with this email");
        }
        if (repository.findByCni(cni).isPresent()) {
            throw new BusinessValidationException("An account already exists with this CNI");
        }
        if (repository.findByNumero(numero).isPresent()) {
            throw new BusinessValidationException("An account already exists with this phone number");
        }
    }

    /**
     * Find client by ID
     */
    public Optional<Client> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Find client by email
     */
    public Optional<Client> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    /**
     * Find client by phone number
     */
    public Optional<Client> findByNumero(String numero) {
        return repository.findByNumero(numero);
    }

    /**
     * Find client for authentication (active clients only)
     */
    public Optional<Client> findForAuthentication(String identifier) {
        // Try by phone number first
        if (identifier.matches("^6[5-9]\\d{7}$")) {
            return repository.findActiveClientByNumero(identifier);
        }
        // Then try by email
        if (identifier.contains("@")) {
            return repository.findActiveClientByEmail(identifier);
        }
        return Optional.empty();
    }

    /**
     * Update client profile
     */
    @Transactional
    public Client updateProfile(String clientId, ProfileUpdateRequest request) {
        Optional<Client> clientOpt = repository.findById(clientId);
        
        if (clientOpt.isEmpty()) {
            throw new BusinessValidationException("Client not found");
        }

        Client client = clientOpt.get();

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(client.getEmail())) {
            // Check if new email is already taken
            Optional<Client> existingClient = repository.findByEmail(request.getEmail());
            if (existingClient.isPresent() && !existingClient.get().getIdClient().equals(clientId)) {
                throw new BusinessValidationException("Email is already in use");
            }
            client.setEmail(request.getEmail().toLowerCase().trim());
        }

        // Update phone number if provided and different
        if (request.getNumero() != null && !request.getNumero().equals(client.getNumero())) {
            // Check if new number is already taken
            Optional<Client> existingClient = repository.findByNumero(request.getNumero());
            if (existingClient.isPresent() && !existingClient.get().getIdClient().equals(clientId)) {
                throw new BusinessValidationException("Phone number is already in use");
            }
            client.setNumero(request.getNumero().trim());
        }

        // Update name fields if provided
        if (request.getNom() != null && !request.getNom().trim().isEmpty()) {
            client.setNom(request.getNom().toUpperCase().trim());
        }

        if (request.getPrenom() != null && !request.getPrenom().trim().isEmpty()) {
            client.setPrenom(request.getPrenom().trim());
        }

        Client updatedClient = repository.save(client);
        log.info("Profile updated for client: {}", clientId);
        
        return updatedClient;
    }

    // =====================================
    // 🔐 PASSWORD MANAGEMENT
    // =====================================

    /**
     * Request password reset
     */
    public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());

        try {
            // Find client by email
            Optional<Client> clientOpt = repository.findByEmail(request.getEmail());
            
            if (clientOpt.isEmpty()) {
                // Don't reveal that account doesn't exist for security
                return PasswordResetResponse.builder()
                        .message("If an account with this email exists, you will receive reset instructions")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            Client client = clientOpt.get();

            // Verify other details match (additional security)
            if (!client.getCni().equals(request.getCni()) || 
                !client.getNumero().equals(request.getNumero()) ||
                !client.getNom().equalsIgnoreCase(request.getNom())) {
                
                log.warn("Password reset attempt with mismatched details for: {}", request.getEmail());
                throw new BusinessValidationException("Provided information does not match our records");
            }

            // Generate reset token (you might want to implement a proper token system)
            String resetToken = UUID.randomUUID().toString();
            
            // TODO: Store reset token with expiration time
            // TODO: Send email with reset link
            
            log.info("Password reset token generated for client: {}", client.getIdClient());

            return PasswordResetResponse.builder()
                    .message("Password reset instructions have been sent to your email")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Password reset request failed: {}", e.getMessage(), e);
            throw new ServiceException("Password reset request failed");
        }
    }

    // =====================================
    // 📊 STATISTICS AND SEARCH
    // =====================================

    /**
     * Search clients by term
     */
    public List<Client> searchClients(String searchTerm, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            
            // This is a simple implementation - you might want to use more sophisticated search
            // For now, search by name, email, or phone number
            List<Client> results = new ArrayList<>();
            
            // Search by email
            if (searchTerm.contains("@")) {
                repository.findByEmail(searchTerm).ifPresent(results::add);
            }
            
            // Search by phone number
            if (searchTerm.matches("^6[5-9]\\d{7}$")) {
                repository.findByNumero(searchTerm).ifPresent(results::add);
            }
            
            // Search by name (case insensitive)
            // You might want to implement a proper text search here
            
            return results;

        } catch (Exception e) {
            log.error("Client search failed: {}", e.getMessage(), e);
            throw new ServiceException("Client search failed");
        }
    }

    /**
     * Get client statistics
     */
    public ClientStatisticsResponse getClientStatistics() {
        try {
            long totalClients = repository.count();
            long activeClients = repository.findByStatus(ClientStatus.ACTIVE).size();
            long pendingClients = repository.findByStatus(ClientStatus.PENDING).size();
            long blockedClients = repository.findByStatus(ClientStatus.BLOCKED).size();
            long rejectedClients = repository.findByStatus(ClientStatus.REJECTED).size();
            long suspendedClients = repository.findByStatus(ClientStatus.SUSPENDED).size();
            
            // Count new clients today
            LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            long newClientsToday = repository.countNewClientsToday(startOfDay);
            
            // Count new clients this week
            LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.DAYS);
            long newClientsThisWeek = repository.countNewClientsToday(startOfWeek);
            
            // Count new clients this month
            LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30).truncatedTo(ChronoUnit.DAYS);
            long newClientsThisMonth = repository.countNewClientsToday(startOfMonth);
            
            // KYC statistics (active = KYC verified)
            long kycVerifiedClients = activeClients;
            long kycPendingClients = pendingClients;
            
            // Activity statistics
            LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            long clientsLoggedInToday = repository.findClientsLoggedInSince(todayStart).size();
            
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            long clientsLoggedInThisWeek = repository.findClientsLoggedInSince(weekStart).size();

            return ClientStatisticsResponse.builder()
                    .totalClients(totalClients)
                    .activeClients(activeClients)
                    .pendingClients(pendingClients)
                    .blockedClients(blockedClients)
                    .rejectedClients(rejectedClients)
                    .suspendedClients(suspendedClients)
                    .newClientsToday(newClientsToday)
                    .newClientsThisWeek(newClientsThisWeek)
                    .newClientsThisMonth(newClientsThisMonth)
                    .kycVerifiedClients(kycVerifiedClients)
                    .kycPendingClients(kycPendingClients)
                    .clientsLoggedInToday(clientsLoggedInToday)
                    .clientsLoggedInThisWeek(clientsLoggedInThisWeek)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate client statistics: {}", e.getMessage(), e);
            throw new ServiceException("Failed to generate statistics");
        }
    }


    // =====================================
    // 🔄 SESSION MANAGEMENT
    // =====================================

    /**
     * Record successful login
     */
    @Transactional
    public void recordSuccessfulLogin(String clientId) {
        Optional<Client> clientOpt = repository.findById(clientId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            client.setLastLogin(LocalDateTime.now());
            client.setLoginAttempts(0); // Reset failed attempts
            client.setLastFailedLogin(null);
            repository.save(client);
            log.info("Successful login recorded for client: {}", clientId);
        }
    }

    /**
     * Record failed login attempt
     */
    @Transactional
    public void recordFailedLogin(String identifier) {
        Optional<Client> clientOpt = findForAuthentication(identifier);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            client.setLoginAttempts(client.getLoginAttempts() + 1);
            client.setLastFailedLogin(LocalDateTime.now());
            repository.save(client);
            log.warn("Failed login attempt #{} recorded for client: {}", 
                    client.getLoginAttempts(), client.getIdClient());
        }
    }

    // =====================================
    // 📋 STATUS MANAGEMENT
    // =====================================

    /**
     * Update client status (typically called by AgenceService)
     */
    @Transactional
    public void updateClientStatus(String clientId, ClientStatus newStatus, String reason) {
        Optional<Client> clientOpt = repository.findById(clientId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            ClientStatus oldStatus = client.getStatus();
            client.setStatus(newStatus);
            repository.save(client);
            
            log.info("Client {} status updated from {} to {} - Reason: {}", 
                    clientId, oldStatus, newStatus, reason);
        } else {
            log.error("Attempted to update status for non-existent client: {}", clientId);
        }
    }

    /**
     * Get clients by status
     */
    public List<Client> getClientsByStatus(ClientStatus status) {
        return repository.findByStatus(status);
    }

    /**
     * Get clients by agency
     */
    public List<Client> getClientsByAgency(String agencyId) {
        return repository.findByAgence(agencyId);
    }

    // =====================================
    // 🛠️ UTILITY METHODS
    // =====================================

    /**
     * Check if client exists by any identifier
     */
    public boolean clientExists(String email, String cni, String numero) {
        return repository.findByEmail(email).isPresent() ||
               repository.findByCni(cni).isPresent() ||
               repository.findByNumero(numero).isPresent();
    }

    /**
     * Get total client count
     */
    public long getTotalClientCount() {
        return repository.count();
    }

    /**
     * Validate client data integrity
     */
    public void validateClientData(Client client) {
        if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
            throw new BusinessValidationException("Email is required");
        }
        if (client.getCni() == null || client.getCni().trim().isEmpty()) {
            throw new BusinessValidationException("CNI is required");
        }
        if (client.getNumero() == null || client.getNumero().trim().isEmpty()) {
            throw new BusinessValidationException("Phone number is required");
        }
        if (!client.getNumero().matches("^6[5-9]\\d{7}$")) {
            throw new BusinessValidationException("Invalid Cameroonian phone number format");
        }
        if (!client.getCni().matches("\\d{8,12}")) {
            throw new BusinessValidationException("Invalid CNI format");
        }
    }

    /**
     * Update client status with reason
     */
    @Transactional
    public void updateStatus(String clientId, ClientStatus newStatus, String reason) {
        Optional<Client> clientOpt = repository.findById(clientId);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            ClientStatus oldStatus = client.getStatus();
            client.setStatus(newStatus);
            repository.save(client);
            
            log.info("Client {} status updated from {} to {} - Reason: {}", 
                    clientId, oldStatus, newStatus, reason);
        } else {
            log.error("Attempted to update status for non-existent client: {}", clientId);
            throw new BusinessValidationException("Client not found");
        }
    }

    /**
     * Increment failed login attempts
     */
    @Transactional
    public void incrementFailedLoginAttempts(String clientId, LocalDateTime failedTime) {
        try {
            repository.incrementFailedLoginAttempts(clientId, failedTime);
            
            // Check if client should be blocked
            Optional<Client> clientOpt = repository.findById(clientId);
            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                if (client.getLoginAttempts() >= 3) { // Max attempts reached
                    client.setStatus(ClientStatus.BLOCKED);
                    repository.save(client);
                    log.warn("Client {} blocked due to too many failed login attempts", clientId);
                }
            }
            
            log.info("Failed login attempt recorded for client: {}", clientId);
        } catch (Exception e) {
            log.error("Failed to increment login attempts for client {}: {}", clientId, e.getMessage());
            throw new ServiceException("Failed to record login attempt");
        }
    }

        /**
         * Activate account
         */
        @Transactional
        public void activateAccount(String clientId) {
            updateStatus(clientId, ClientStatus.ACTIVE, "Account activated");
        }

        /**
         * Reject account with reason
         */
        @Transactional
        public void rejectAccount(String clientId, String reason) {
            updateStatus(clientId, ClientStatus.REJECTED, reason);
        }

        /**
         * Count new clients today
         */
        public long countNewClientsToday(LocalDateTime startOfDay) {
            return repository.countNewClientsToday(startOfDay);
        }

        public RegisterResponse register(ClientRegistrationDTO registration) {
        return registerClient(registration);
    }

    /**
     * Création client sécurisée avec selfie
     */
    private Client createSecureClient(ClientRegistrationDTO request) {
        Client client = new Client();
        client.setIdClient(UUID.randomUUID().toString());
        client.setCni(request.getCni());
        client.setEmail(request.getEmail());
        client.setNom(request.getNom().toUpperCase());
        client.setPrenom(request.getPrenom());
        client.setNumero(request.getNumero());
        client.setIdAgence(request.getIdAgence());
        
        // Hash du mot de passe
        client.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Documents KYC
        client.setRectoCni(request.getRectoCni());
        client.setVersoCni(request.getVersoCni());
        client.setSelfieImage(request.getSelfieImage()); // ← NEW: Store selfie
        
        // Métadonnées selfie
        if (request.getSelfieImage() != null) {
            try {
                String base64Data = extractBase64Data(request.getSelfieImage());
                byte[] selfieBytes = Base64.getDecoder().decode(base64Data);
                client.setSelfieFileSize((long) selfieBytes.length);
                client.setSelfieUploadedAt(LocalDateTime.now());
                
                log.info("📸 Selfie traité pour client: {} - Taille: {} KB", 
                        client.getEmail(), selfieBytes.length / 1024);
            } catch (Exception e) {
                log.warn("⚠️ Erreur traitement métadonnées selfie pour {}: {}", 
                        client.getEmail(), e.getMessage());
            }
        }
        
        // Statut et audit
        client.setStatus(ClientStatus.PENDING);
        client.setCreatedAt(LocalDateTime.now());
        
        return client;
    }

    /**
     * Extraction des données Base64 propres
     */
    private String extractBase64Data(String base64String) {
        if (base64String.startsWith("data:image/")) {
            int commaIndex = base64String.indexOf(",");
            if (commaIndex > 0) {
                return base64String.substring(commaIndex + 1);
            }
        }
        return base64String;
    }

    /**
     * Validation spécifique du selfie
     */
    private void validateSelfieImage(String selfieImage) {
        if (selfieImage == null || selfieImage.trim().isEmpty()) {
            throw new BusinessValidationException("Selfie utilisateur obligatoire");
        }
        
        try {
            String base64Data = extractBase64Data(selfieImage);
            byte[] decoded = Base64.getDecoder().decode(base64Data);
            
            // Vérifications de taille
            if (decoded.length < 20 * 1024) { // 20KB minimum
                throw new BusinessValidationException("Selfie trop petit (minimum 20KB)");
            }
            
            if (decoded.length > 10 * 1024 * 1024) { // 10MB maximum
                throw new BusinessValidationException("Selfie trop volumineux (maximum 10MB)");
            }
            
            // Vérification du format d'image basique
            if (!isValidImageFormat(decoded)) {
                throw new BusinessValidationException("Format de selfie invalide (JPEG/PNG requis)");
            }
            
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("Format Base64 du selfie invalide");
        }
    }

    /**
     * Validation format d'image
     */
    private boolean isValidImageFormat(byte[] imageData) {
        if (imageData.length < 4) return false;
        
        // JPEG: FF D8 FF
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF) {
            return true;
        }
        
        // PNG: 89 50 4E 47
        if (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && 
            imageData[2] == 0x4E && imageData[3] == 0x47) {
            return true;
        }
        
        return false;
    }

    // Modify the existing validateRegistrationData method to include selfie validation:
    private void validateRegistrationData(ClientRegistrationDTO request) {
        // Vérifications existantes
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessValidationException("Un compte existe déjà avec cet email");
        }

        if (repository.findByCni(request.getCni()).isPresent()) {
            throw new BusinessValidationException("Un compte existe déjà avec cette CNI");
        }

        if (repository.findByNumero(request.getNumero()).isPresent()) {
            throw new BusinessValidationException("Un compte existe déjà avec ce numéro");
        }
        
        // ← NEW: Validation du selfie
        validateSelfieImage(request.getSelfieImage());
    }

}