package com.m1_fonda.serviceUser.web.controler;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.DepositRequest;
import com.m1_fonda.serviceUser.request.PasswordResetRequest;
import com.m1_fonda.serviceUser.request.ProfileUpdateRequest;
import com.m1_fonda.serviceUser.request.TransferRequest;
import com.m1_fonda.serviceUser.request.WithdrawalRequest;
import com.m1_fonda.serviceUser.response.ClientProfileResponse;
import com.m1_fonda.serviceUser.response.ClientStatisticsResponse;
import com.m1_fonda.serviceUser.response.ErrorResponse;
import com.m1_fonda.serviceUser.response.PasswordResetResponse;
import com.m1_fonda.serviceUser.response.RegisterResponse;
import com.m1_fonda.serviceUser.response.RegistrationStatusResponse;
import com.m1_fonda.serviceUser.response.TransactionResponse;
import com.m1_fonda.serviceUser.response.ValidationErrorResponse;
import com.m1_fonda.serviceUser.service.UserService;
import com.m1_fonda.serviceUser.service.UserServiceRabbit;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
@Validated
@Slf4j
// @CrossOrigin(origins = "${app.cors.allowed-origins}")
public class UserController {

    @Autowired
    private UserRepository userService;

    @Autowired
    private UserServiceRabbit userServiceRabbit;

    @Autowired
    private UserService repository;

    // =====================================
    // ENDPOINTS D'AUTHENTIFICATION
    // =====================================

    /**
     * Enregistrement d'un nouveau client
     */
    @PostMapping("/register")
    @Operation(summary = "Créer un nouveau compte client", description = "Enregistre une demande de création de compte qui sera validée par l'agence")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Demande acceptée et en cours de traitement"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Compte déjà existant"),
            @ApiResponse(responseCode = "503", description = "Service temporairement indisponible")
    })
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody ClientRegistrationDTO request,
            HttpServletRequest httpRequest) {

        try {
            log.info("Nouvelle demande d'enregistrement: {} depuis IP: {}",
                    request.getEmail(), getClientIpAddress(httpRequest));

            RegisterResponse response = repository.register(request);

            log.info("Demande enregistrée avec succès: {}", request.getEmail());

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (BusinessValidationException e) {
            log.warn("Validation échouée pour {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RegisterResponse("REJECTED", e.getMessage()));

        } catch (ServiceException e) {
            log.error("Erreur service pour {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new RegisterResponse("ERROR", "Service temporairement indisponible"));
        }
    }

    /**
     * Vérification statut d'une demande
     */
    @GetMapping("/registration-status")
    @Operation(summary = "Vérifier le statut d'une demande d'enregistrement")
    public ResponseEntity<RegistrationStatusResponse> checkRegistrationStatus(
            @RequestParam @Email String email) {

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
                        .body(new RegistrationStatusResponse("NOT_FOUND", "Aucune demande trouvée", null));
            }

        } catch (Exception e) {
            log.error("Erreur vérification statut: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RegistrationStatusResponse("ERROR", "Erreur technique", null));
        }
    }

    // =====================================
    // ENDPOINTS OPÉRATIONS FINANCIÈRES
    // =====================================

    /**
     * Effectuer un dépôt
     */
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Effectuer un dépôt sur un compte")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("Demande dépôt: {} FCFA sur compte {} par client {}",
                    request.getMontant(), request.getNumeroCompte(), clientId);

            TransactionResponse response = userServiceRabbit.sendDepot(request, clientId);

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(null, "REJECTED", e.getMessage(), null, LocalDateTime.now()));

        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TransactionResponse(null, "ERROR", "Service indisponible", null, LocalDateTime.now()));
        }
    }

    /**
     * Effectuer un retrait
     */
    @PostMapping("/withdrawal")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Effectuer un retrait d'un compte")
    public ResponseEntity<TransactionResponse> withdrawal(
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("Demande retrait: {} FCFA du compte {} par client {}",
                    request.getMontant(), request.getNumeroCompte(), clientId);

            TransactionResponse response = userServiceRabbit.sendRetrait(request, clientId);

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(null, "REJECTED", e.getMessage(), null, LocalDateTime.now()));

        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TransactionResponse(null, "ERROR", "Service indisponible", null, LocalDateTime.now()));
        }
    }

    /**
     * Effectuer un transfert inter-comptes
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Effectuer un transfert entre comptes")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            log.info("Demande transfert: {} FCFA de {} vers {} par client {}",
                    request.getMontant(), request.getNumeroCompteSend(),
                    request.getNumeroCompteReceive(), clientId);

            TransactionResponse response = userServiceRabbit.sendTransaction(request, clientId);

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(null, "REJECTED", e.getMessage(), null, LocalDateTime.now()));

        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TransactionResponse(null, "ERROR", "Service indisponible", null, LocalDateTime.now()));
        }
    }

    // =====================================
    // ENDPOINTS GESTION COMPTE
    // =====================================

    /**
     * Récupérer profil utilisateur
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    public ResponseEntity<ClientProfileResponse> getProfile(Authentication authentication) {

        try {
            String clientId = extractClientId(authentication);

            Optional<Client> client = userService.findById(clientId);

            if (client.isPresent()) {
                ClientProfileResponse profile = mapToProfileResponse(client.get());
                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

        } catch (Exception e) {
            log.error("Erreur récupération profil: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mettre à jour le profil
     */
    @PutMapping("/profile/{id}")
    public ResponseEntity<Client> updateProfile(
            @PathVariable String id,
            @RequestBody ProfileUpdateRequest request) {

        try {
            // Chercher le client
            Client client = userService.findById(id).orElse(null);
            if (client == null) {
                return ResponseEntity.notFound().build();
            }

            // Mise à jour simple des champs
            if (request.getEmail() != null) {
                Optional<Client> existingClientOpt = userService.findByEmail(request.getEmail());
                if (existingClientOpt.isPresent()) {
                    Client existingClient = existingClientOpt.get();
                    if (!existingClient.getIdClient().equals(id)) {
                        return ResponseEntity.badRequest().build();
                    }
                }
                client.setEmail(request.getEmail());
            }

            if (request.getNumero() != null && !request.getNumero().trim().isEmpty()) {
                Optional<Client> existingClientOpt = userService.findByNumero(request.getNumero());

                if (existingClientOpt.isPresent()) {
                    Client existingClient = existingClientOpt.get(); // Récupérer le Client
                    if (!existingClient.getIdClient().equals(id)) {
                        return ResponseEntity.badRequest().build(); // Numéro déjà utilisé
                    }
                }

                client.setNumero(request.getNumero());
            }

            if (request.getNom() != null) {
                client.setNom(request.getNom());
            }

            if (request.getPrenom() != null) {
                client.setPrenom(request.getPrenom());
            }

            // Sauvegarder
            Client updatedClient = userService.save(client);

            log.info("Profil mis à jour pour client: {}", id);
            return ResponseEntity.ok(updatedClient);

        } catch (Exception e) {
            log.error("Erreur mise à jour profil: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Demande de reset password
     */
    @PostMapping("/password-reset/request")
    @Operation(summary = "Demander une réinitialisation de mot de passe")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        try {
            log.info("Demande reset password pour: {}", request.getEmail());

            userServiceRabbit.sendPasswordResetRequest(request);

            PasswordResetResponse response = new PasswordResetResponse(
                    "SUCCESS",
                    "Un email de réinitialisation a été envoyé à votre adresse",
                    LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PasswordResetResponse("ERROR", e.getMessage(), LocalDateTime.now()));

        } catch (Exception e) {
            log.error("Erreur demande reset password: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PasswordResetResponse("ERROR", "Erreur technique", LocalDateTime.now()));
        }
    }

    // =====================================
    // ENDPOINTS ADMINISTRATIFS
    // =====================================

    /**
     * Rechercher des clients (Admin uniquement)
     */
    /**
     * SIMPLE : Recherche clients SANS pagination complexe
     */
    @GetMapping("/search")
    public ResponseEntity<List<Client>> searchClients(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            List<Client> allClients = userService.findAll();
            List<Client> filteredClients = new ArrayList<>();

            // Filtrage simple par terme de recherche
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String term = searchTerm.toLowerCase();

                for (Client client : allClients) {
                    // Rechercher dans nom, prénom, email
                    boolean matches = client.getNom().toLowerCase().contains(term) ||
                            client.getPrenom().toLowerCase().contains(term) ||
                            client.getEmail().toLowerCase().contains(term);

                    if (matches) {
                        filteredClients.add(client);
                    }
                }
            } else {
                filteredClients = allClients;
            }

            // Pagination simple
            int start = page * size;
            int end = Math.min(start + size, filteredClients.size());

            if (start >= filteredClients.size()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Client> pagedClients = filteredClients.subList(start, end);
            return ResponseEntity.ok(pagedClients);

        } catch (Exception e) {
            log.error("Erreur recherche clients: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Statistiques clients (Admin uniquement)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques des clients (Admin)")
    public ResponseEntity<ClientStatisticsResponse> getClientStatistics() {

        try {
            Map<String, Long> stats = repository.getClientStatistics();

            ClientStatisticsResponse response = new ClientStatisticsResponse(
                    stats.get("total"),
                    stats.get("active"),
                    stats.get("pending"),
                    stats.get("blocked"),
                    stats.get("newToday"),
                    LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur récupération statistiques: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    private String extractClientId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername(); // Supposé contenir l'ID client
        }
        throw new SecurityException("Client non authentifié");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getStatusMessage(ClientStatus status) {
        return switch (status) {
            case PENDING -> "Votre demande est en cours de traitement";
            case ACTIVE -> "Votre compte est actif";
            case SUSPENDED -> "Votre compte est temporairement suspendu";
            case BLOCKED -> "Votre compte est bloqué";
            case REJECTED -> "Votre demande a été rejetée";
        };
    }

    private ClientProfileResponse mapToProfileResponse(Client client) {
        return new ClientProfileResponse(
                client.getIdClient(),
                client.getNom(),
                client.getPrenom(),
                client.getEmail(),
                client.getNumero(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getLastLogin());
    }

    // =====================================
    // GESTION D'ERREURS GLOBALE
    // =====================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ValidationErrorResponse response = new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Données invalides",
                errors,
                LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
                "ACCESS_DENIED",
                "Accès non autorisé",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Erreur non gérée: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                "INTERNAL_ERROR",
                "Erreur technique interne",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
