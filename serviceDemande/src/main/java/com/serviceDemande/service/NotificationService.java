package com.serviceDemande.service;


import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serviceDemande.dto.FraudAlertNotification;
import com.serviceDemande.dto.ManualReviewNotification;
import com.serviceDemande.model.Demande;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendManualReviewNotification(Demande demande) {
        // Envoyer notification aux superviseurs pour révision manuelle
        ManualReviewNotification notification = new ManualReviewNotification();
        notification.setDemandeId(demande.getId());
        notification.setIdClient(demande.getIdClient());
        notification.setRiskScore(demande.getRiskScore());
        notification.setFraudFlags(demande.getFraudFlags());
        notification.setCreatedAt(demande.getCreatedAt());

        try {
            rabbitTemplate.convertAndSend("notifications-exchange", "manual.review.required", notification);
            log.info("🔔 Notification révision manuelle envoyée pour: {}", demande.getIdClient());
        } catch (Exception e) {
            log.error("❌ Erreur envoi notification révision: {}", e.getMessage());
        }
    }

    public void sendFraudAlert(String idClient, String alertType, String details) {
        FraudAlertNotification alert = new FraudAlertNotification();
        alert.setIdClient(idClient);
        alert.setAlertType(alertType);
        alert.setDetails(details);
        alert.setTimestamp(java.time.LocalDateTime.now());

        try {
            rabbitTemplate.convertAndSend("security-exchange", "fraud.alert", alert);
            log.warn("🚨 Alerte fraude envoyée: client={}, type={}", idClient, alertType);
        } catch (Exception e) {
            log.error("❌ Erreur envoi alerte fraude: {}", e.getMessage());
        }
    }
}
