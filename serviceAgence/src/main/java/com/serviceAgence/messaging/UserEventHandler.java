package com.serviceAgence.messaging;

import java.util.Base64;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.serviceAgence.dto.UserRegistrationRequest;
import com.serviceAgence.event.PasswordResetRequestEvent;
import com.serviceAgence.event.UserRegistrationEventReceived;
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

    /**
     * Réception des demandes de création de compte depuis UserService
     */
    @RabbitListener(queues = "Demande-Queue")
    public void handleUserRegistration(UserRegistrationEventReceived event) {
        log.info("Réception demande création compte: client={}, agence={}",
                event.getIdClient(), event.getIdAgence());

        try {
            // Conversion de l'événement vers DTO
            UserRegistrationRequest request = convertToRegistrationRequest(event);

            // Traitement de la demande
            RegistrationProcessingResult result = agenceService.processRegistrationRequest(request);

            // Envoi de la réponse vers UserService
            eventPublisher.sendRegistrationResponse(event.getIdClient(), event.getIdAgence(),
                    event.getEmail(), result);

            log.info("Demande traitée: client={}, résultat={}",
                    event.getIdClient(), result.isAccepted() ? "ACCEPTE" : "REFUSE");

        } catch (Exception e) {
            log.error("Erreur traitement demande création: {}", e.getMessage(), e);

            // Envoi réponse d'erreur
            RegistrationProcessingResult errorResult = RegistrationProcessingResult.rejected(
                    "ERREUR_TECHNIQUE", "Erreur technique lors du traitement");
            eventPublisher.sendRegistrationResponse(event.getIdClient(), event.getIdAgence(),
                    event.getEmail(), errorResult);
        }
    }

    /**
     * Réception des demandes de reset password
     */
    @RabbitListener(queues = "Demande-Reset-passWord-Queue")
    public void handlePasswordResetRequest(PasswordResetRequestEvent event) {
        log.info("Réception demande reset password: client={}", event.getIdClient());

        try {
            // TODO: Implémenter logique de reset password
            // Pour l'instant, génération d'un mot de passe temporaire
            String tempPassword = generateTemporaryPassword();

            eventPublisher.sendPasswordResetResponse(event.getCni(), tempPassword,
                    event.getEmail(), "AGENCE_SYSTEM");

            log.info("Reset password traité pour: {}", event.getIdClient());

        } catch (Exception e) {
            log.error("Erreur traitement reset password: {}", e.getMessage(), e);
        }
    }

    /**
     * Conversion événement vers DTO
     */
    private UserRegistrationRequest convertToRegistrationRequest(UserRegistrationEventReceived event) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setIdClient(event.getIdClient());
        request.setIdAgence(event.getIdAgence());
        request.setCni(event.getCni());
        request.setEmail(event.getEmail());
        request.setNom(event.getNom());
        request.setPrenom(event.getPrenom());
        request.setNumero(event.getNumero());

        // Décodage des images Base64
        try {
            if (event.getRectoCni() != null && !event.getRectoCni().trim().isEmpty()) {
                request.setRectoCni(Base64.getDecoder().decode(event.getRectoCni()));
            }
            if (event.getVersoCni() != null && !event.getVersoCni().trim().isEmpty()) {
                request.setVersoCni(Base64.getDecoder().decode(event.getVersoCni()));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Erreur décodage Base64 pour client {}: {}", event.getIdClient(), e.getMessage());
            // Les images seront null, la validation KYC pourra les détecter
        }

        return request;
    }

    /**
     * Génération mot de passe temporaire sécurisé
     */
    private String generateTemporaryPassword() {
        // Génération plus sécurisée
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }

        return "TEMP" + password.toString();
    }
}