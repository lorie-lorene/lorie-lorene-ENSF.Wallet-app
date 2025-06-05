package com.m1_fonda.serviceUser.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientRegistrationDTO;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.response.RegisterResponse;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserService {
    @Autowired
    private UserServiceRabbit rabbit;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Recherche sécurisée d'un client pour authentification
     */
    public Optional<Client> findForAuthentication(String identifier) {
        // Essayer par numéro d'abord
        if (identifier.matches("^6[5-9]\\d{7}$")) {
            return repository.findActiveClientByNumero(identifier);
        }
        // Puis par email
        if (identifier.contains("@")) {
            return repository.findActiveClientByEmail(identifier);
        }
        return Optional.empty();
    }

    /**
     * Vérification d'unicité pour registration
     */
    public void validateUniqueness(String email, String cni, String numero) {
        if (repository.existsByEmail(email)) {
            throw new BusinessValidationException("Un compte existe déjà avec cet email");
        }
        if (repository.existsByCni(cni)) {
            throw new BusinessValidationException("Un compte existe déjà avec cette CNI");
        }
        if (repository.existsByNumero(numero)) {
            throw new BusinessValidationException("Un compte existe déjà avec ce numéro");
        }
    }

    /**
     * Mise à jour sécurisée de connexion
     */
    public void recordSuccessfulLogin(String clientId) {
        repository.updateLastLogin(clientId, LocalDateTime.now());
        log.info("Connexion réussie enregistrée pour client: {}", clientId);
    }

    /**
     * Enregistrement tentative échouée
     */
    public void recordFailedLogin(String clientId) {
        repository.incrementFailedLoginAttempts(clientId, LocalDateTime.now());

        // Vérifier si le compte doit être bloqué
        Optional<Client> client = repository.findById(clientId);
        if (client.isPresent() && client.get().getLoginAttempts() >= 5) {
            repository.updateStatus(clientId, ClientStatus.BLOCKED, LocalDateTime.now());
            log.warn("Compte bloqué pour trop de tentatives échouées: {}", clientId);
        }
    }

    /**
     * Statistiques pour dashboard admin
     */
    public Map<String, Long> getClientStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", repository.count());
        stats.put("active", repository.countByStatus(ClientStatus.ACTIVE));
        stats.put("pending", repository.countByStatus(ClientStatus.PENDING));
        stats.put("blocked", repository.countByStatus(ClientStatus.BLOCKED));
        stats.put("newToday", repository.countNewClientsToday(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)));
        return stats;
    }

    /**
     * Enregistrement sécurisé d'un nouveau client
     */
    public RegisterResponse register(ClientRegistrationDTO request) {
        log.info("Début enregistrement client: {}", request.getEmail());

        try {
            // 1. Validation métier
            validateRegistrationData(request);

            // 2. Créer client avec données sécurisées
            Client client = createSecureClient(request);

            // 3. Envoyer event vers AgenceService (SANS sauvegarder)
            rabbit.sendRegistrationEvent(client);

            log.info("Demande envoyée vers AgenceService pour: {}", request.getEmail());

            return new RegisterResponse("PENDING",
                    "Votre demande a été transmise et sera traitée dans les plus brefs délais");

        } catch (BusinessValidationException e) {
            log.warn("Validation échouée pour {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement: {}", e.getMessage(), e);
            throw new ServiceException("Erreur technique lors de l'enregistrement");
        }
    }

    private void validateRegistrationData(ClientRegistrationDTO request) {
        // Vérification unicité email
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessValidationException("Un compte existe déjà avec cet email");
        }

        // Vérification unicité CNI
        if (repository.findByCni(request.getCni()).isPresent()) {
            throw new BusinessValidationException("Un compte existe déjà avec cette CNI");
        }

        // Vérification unicité numéro
        if (repository.findByNumero(request.getNumero()).isPresent()) {
            throw new BusinessValidationException("Un compte existe déjà avec ce numéro");
        }

        // Validation format CNI camerounais
        if (!isValidCameroonianCNI(request.getCni())) {
            throw new BusinessValidationException("Format CNI camerounais invalide");
        }
    }

    private Client createSecureClient(ClientRegistrationDTO request) {
        Client client = new Client();
        client.setIdClient(UUID.randomUUID().toString());
        client.setCni(request.getCni());
        client.setEmail(request.getEmail().toLowerCase());
        client.setNom(request.getNom().toUpperCase());
        client.setPrenom(request.getPrenom());
        client.setNumero(request.getNumero());
        client.setIdAgence(request.getIdAgence());
        client.setRectoCni(request.getRectoCni());
        client.setVersoCni(request.getVersoCni());

        // SÉCURITÉ : Hash du mot de passe
        String salt = generateSalt();
        String passwordHash = passwordEncoder.encode(request.getPassword() + salt);

        client.setPasswordHash(passwordHash);
        client.setSalt(salt);
        client.setPasswordChangedAt(LocalDateTime.now());
        client.setStatus(ClientStatus.PENDING);

        return client;
    }

    private String generateSalt() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    private boolean isValidCameroonianCNI(String cni) {
        // Logique validation CNI camerounaise
        return cni != null && cni.matches("\\d{8,12}");
    }

    /**
     * Activation du compte après validation AgenceService
     */
    public void activateAccount(String clientId) {
        Client client = repository.findById(clientId)
                .orElseThrow(() -> new BusinessValidationException("Client introuvable"));

        if (client.getStatus() != ClientStatus.PENDING) {
            throw new BusinessValidationException("Compte déjà traité");
        }

        client.setStatus(ClientStatus.ACTIVE);
        repository.save(client);

        log.info("Compte activé pour client: {}", clientId);
    }

    /**
     * Rejet de la demande
     */
    public void rejectAccount(String clientId, String reason) {
        Client client = repository.findById(clientId)
                .orElseThrow(() -> new BusinessValidationException("Client introuvable"));

        client.setStatus(ClientStatus.REJECTED);
        repository.save(client);

        log.info("Compte rejeté pour client: {} - Raison: {}", clientId, reason);
    }

    public void register(Client demande) {
        this.rabbit.sendRegistrationEvent(demande);
    }

    public List<Client> findUser() {
        return repository.findAll();
    }

    public void addUser(Client user) {
        repository.save(user);
    }

}
