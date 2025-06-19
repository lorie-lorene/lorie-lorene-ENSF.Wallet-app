package com.wallet.bank_card_service.service;


import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.wallet.bank_card_service.model.Carte;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {
  /**
     * Notification de création de carte
     */
    public void sendCarteCreationNotification(Carte carte) {
        log.info("📧 Notification création carte envoyée: {}", carte.getMaskedNumber());
        
        // En réalité, envoyer via RabbitMQ vers le service notification
        // String message = String.format("Votre carte %s a été créée avec succès.", 
        //         carte.getType().getLibelle());
        // notificationProducer.sendNotification(carte.getIdClient(), message);
    }

    /**
     * Notification de transfert
     */
    public void sendTransferNotification(Carte carte, BigDecimal montant, String operation) {
        log.info("📧 Notification transfert envoyée: {} {} FCFA sur carte {}", 
                operation, montant, carte.getMaskedNumber());
    }

    /**
     * Notification de blocage/déblocage
     */
    public void sendCardBlockNotification(Carte carte, String reason) {
        log.info("📧 Notification blocage envoyée: carte {} bloquée - {}", 
                carte.getMaskedNumber(), reason);
    }

    public void sendCardUnblockNotification(Carte carte) {
        log.info("📧 Notification déblocage envoyée: carte {} débloquée", 
                carte.getMaskedNumber());
    }

    /**
     * Notification de changement PIN
     */
    public void sendPinChangeNotification(Carte carte) {
        log.info("📧 Notification changement PIN envoyée: carte {}", 
                carte.getMaskedNumber());
    }

    /**
     * Notifications administratives
     */
    public void sendAdminCardBlockNotification(Carte carte, String reason, String adminId) {
        log.info("📧 Notification blocage admin envoyée: carte {} bloquée par admin {} - {}", 
                carte.getMaskedNumber(), adminId, reason);
    }

    /**
     * Notification frais mensuels
     */
    public void sendMonthlyFeesNotification(Carte carte, BigDecimal frais) {
        log.info("📧 Notification frais mensuels envoyée: {} FCFA prélevés sur carte {}", 
                frais, carte.getMaskedNumber());
    }

    /**
     * Notification expiration carte
     */
    public void sendCardExpiryNotification(Carte carte) {
        log.info("📧 Notification expiration envoyée: carte {} expirée", 
                carte.getMaskedNumber());
    }

    /**
     * Notification transfert carte à carte
     */
    public void sendCardToCardTransferNotification(Carte carte, Carte auteCarte, 
            BigDecimal montant, String operation) {
        log.info("📧 Notification transfert carte à carte envoyée: {} {} FCFA - carte {} {} carte {}", 
                operation, montant, carte.getMaskedNumber(), 
                operation.equals("DEBIT") ? "vers" : "depuis", auteCarte.getMaskedNumber());
    }
}