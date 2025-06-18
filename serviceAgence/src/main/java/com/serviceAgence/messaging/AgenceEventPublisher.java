package com.serviceAgence.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.serviceAgence.dto.RegistrationProcessingResult;
import com.serviceAgence.dto.TransactionResult;
import com.serviceAgence.event.PasswordResetResponseEvent;
import com.serviceAgence.event.RegistrationResponseEvent;
import com.serviceAgence.event.TransactionResponseEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AgenceEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String AGENCE_EXCHANGE = "agence-exchange";

    /**
     * Envoi réponse de création de compte vers UserService
     */
    public void sendRegistrationResponse(String idClient, String idAgence, String email, 
                                       RegistrationProcessingResult result) {
        try {
            RegistrationResponseEvent event = new RegistrationResponseEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setIdClient(idClient);
            event.setIdAgence(idAgence);
            event.setEmail(email);
            event.setStatut(result.isAccepted() ? "ACCEPTE" : "REFUSE");
            event.setProbleme(result.getErrorCode());
            event.setNumeroCompte(result.getNumeroCompte());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(AGENCE_EXCHANGE, "agence.registration.response", event);
            
            log.info("Réponse création compte envoyée: client={}, statut={}", 
                    idClient, event.getStatut());

        } catch (Exception e) {
            log.error("Erreur envoi réponse création: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoi réponse de transaction vers UserService
     */
    public void sendTransactionResponse(String eventId, TransactionResult result, String numeroCompte) {
        try {
            TransactionResponseEvent event = new TransactionResponseEvent();
            event.setEventId(eventId);
            event.setTransactionId(result.getTransactionId());
            event.setStatut(result.isSuccess() ? "SUCCESS" : "FAILED");
            event.setMessage(result.getMessage());
            event.setMontant(result.getMontant());
            event.setFrais(result.getFrais());
            event.setNumeroCompte(numeroCompte);
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(AGENCE_EXCHANGE, "agence.transaction.response", event);
            
            log.info("Réponse transaction envoyée: {} - Statut: {}", 
                    result.getTransactionId(), event.getStatut());

        } catch (Exception e) {
            log.error("Erreur envoi réponse transaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoi réponse reset password vers UserService
     */
    public void sendPasswordResetResponse(String cni, String newPassword, String email, String agence) {
        try {
            PasswordResetResponseEvent event = new PasswordResetResponseEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setCni(cni);
            event.setNewPassword(newPassword);
            event.setEmail(email);
            event.setAgence(agence);
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(AGENCE_EXCHANGE, "agence.password.reset.response", event);
            
            log.info("Réponse reset password envoyée pour CNI: {}", cni);

        } catch (Exception e) {
            log.error("Erreur envoi réponse reset password: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoi alerte fraude vers AdminService
     */
    public void sendFraudAlert(String idClient, String alertType, String details) {
        try {
            // TODO: Implémenter événement fraude vers AdminService
            log.warn("Alerte fraude détectée: client={}, type={}, détails={}", 
                    idClient, alertType, details);

        } catch (Exception e) {
            log.error("Erreur envoi alerte fraude: {}", e.getMessage(), e);
        }
    }
}
