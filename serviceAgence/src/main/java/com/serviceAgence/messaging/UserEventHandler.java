package com.serviceAgence.messaging;

import java.util.Base64;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.serviceAgence.dto.UserRegistrationRequest;
import com.serviceAgence.event.PasswordResetRequestEvent;
import com.serviceAgence.event.UserRegistrationEventReceived;
import com.serviceAgence.model.DocumentKYC;
import com.serviceAgence.services.AgenceService;
import com.serviceAgence.dto.RegistrationProcessingResult;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserEventHandler {

    @Autowired
    private AgenceService agenceService;

    @Autowired
    private AgenceEventPublisher eventPublisher;

    @Autowired
    private com.serviceAgence.repository.DocumentKYCRepository documentRepository;

    /**
     * Réception des demandes de création de compte avec selfie
     */
    @RabbitListener(queues = "Demande-Queue")
    public void handleUserRegistration(UserRegistrationEventReceived event) {
        log.info("🔍 Réception demande avec selfie: client={}, agence={}", 
                event.getIdClient(), event.getIdAgence());

        try {
            // Validation préliminaire de la présence du selfie
            if (event.getSelfieImage() == null || event.getSelfieImage().trim().isEmpty()) {
                log.warn("⚠️ Selfie manquant pour client: {}", event.getIdClient());
                
                RegistrationProcessingResult errorResult = RegistrationProcessingResult.rejected(
                    "SELFIE_REQUIRED", "Selfie utilisateur obligatoire pour la vérification d'identité");
                    
                eventPublisher.sendRegistrationResponse(event.getIdClient(), event.getIdAgence(),
                        event.getEmail(), errorResult);
                return;
            }

            DocumentKYC document = agenceService.createDocumentWithSelfie(event);
            documentRepository.save(document);
            System.out.println("Document KYC enregistré: " + document.getIdClient());


        } catch (Exception e) {
            log.error("❌ Erreur traitement demande avec selfie: {}", e.getMessage(), e);

            RegistrationProcessingResult errorResult = RegistrationProcessingResult.rejected(
                    "ERREUR_TECHNIQUE", "Erreur technique lors du traitement avec selfie");
            eventPublisher.sendRegistrationResponse(event.getIdClient(), event.getIdAgence(),
                    event.getEmail(), errorResult);
        }
    }

    /**
     * Conversion événement vers DTO avec selfie
     */
    private UserRegistrationRequest convertToRegistrationRequestWithSelfie(UserRegistrationEventReceived event) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setIdClient(event.getIdClient());
        request.setIdAgence(event.getIdAgence());
        request.setCni(event.getCni());
        request.setEmail(event.getEmail());
        request.setNom(event.getNom());
        request.setPrenom(event.getPrenom());
        request.setNumero(event.getNumero());

        return request;
    }

    // Réception des demandes de reset password (inchangé)
    @RabbitListener(queues = "Demande-Reset-passWord-Queue")
    public void handlePasswordResetRequest(PasswordResetRequestEvent event) {
        log.info("Réception demande reset password: client={}", event.getIdClient());

        try {
            String tempPassword = generateTemporaryPassword();
            eventPublisher.sendPasswordResetResponse(event.getCni(), tempPassword,
                    event.getEmail(), "AGENCE_SYSTEM");

            log.info("Reset password traité pour: {}", event.getIdClient());

        } catch (Exception e) {
            log.error("Erreur traitement reset password: {}", e.getMessage(), e);
        }
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }

        return "TEMP" + password.toString();
    }
}