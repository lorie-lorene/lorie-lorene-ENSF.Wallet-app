package com.serviceAgence.services;


import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serviceAgence.event.AccountActivationNotificationEvent;
import com.serviceAgence.event.AccountBlockNotificationEvent;
import com.serviceAgence.event.AccountCreationNotificationEvent;
import com.serviceAgence.event.AccountSuspensionNotificationEvent;
import com.serviceAgence.event.FraudAlertNotificationEvent;
import com.serviceAgence.event.TransactionNotificationEvent;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.model.Transaction;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String NOTIFICATION_EXCHANGE = "Notification-exchange";

    /**
     * Notification de création de compte réussie
     */
    public void sendAccountCreationNotification(CompteUser compte) {
        try {
            AccountCreationNotificationEvent event = new AccountCreationNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setIdClient(compte.getIdClient());
            event.setNumeroCompte(compte.getNumeroCompte());
            event.setIdAgence(compte.getIdAgence());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "account.creation.success", event);
            log.info("Notification création compte envoyée: {}", compte.getNumeroCompte());
        } catch (Exception e) {
            log.error("Erreur envoi notification création compte: {}", e.getMessage(), e);
        }
    }

    /**
     * Notification d'activation de compte
     */
    public void sendAccountActivationNotification(CompteUser compte) {
        try {
            AccountActivationNotificationEvent event = new AccountActivationNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setIdClient(compte.getIdClient());
            event.setNumeroCompte(compte.getNumeroCompte());
            event.setActivatedAt(compte.getActivatedAt());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "account.activation", event);
            log.info("Notification activation compte envoyée: {}", compte.getNumeroCompte());
        } catch (Exception e) {
            log.error("Erreur envoi notification activation: {}", e.getMessage(), e);
        }
    }

    /**
     * Notification de suspension de compte
     */
    public void sendAccountSuspensionNotification(CompteUser compte, String reason) {
        try {
            AccountSuspensionNotificationEvent event = new AccountSuspensionNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setIdClient(compte.getIdClient());
            event.setNumeroCompte(compte.getNumeroCompte());
            event.setReason(reason);
            event.setSuspendedAt(compte.getBlockedAt());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "account.suspension", event);
            log.info("Notification suspension compte envoyée: {}", compte.getNumeroCompte());
        } catch (Exception e) {
            log.error("Erreur envoi notification suspension: {}", e.getMessage(), e);
        }
    }

    /**
     * Notification de blocage de compte
     */
    public void sendAccountBlockNotification(CompteUser compte, String reason) {
        try {
            AccountBlockNotificationEvent event = new AccountBlockNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setIdClient(compte.getIdClient());
            event.setNumeroCompte(compte.getNumeroCompte());
            event.setReason(reason);
            event.setBlockedAt(compte.getBlockedAt());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "account.block", event);
            log.info("Notification blocage compte envoyée: {}", compte.getNumeroCompte());
        } catch (Exception e) {
            log.error("Erreur envoi notification blocage: {}", e.getMessage(), e);
        }
    }

    /**
     * Notification de transaction
     */
    public void sendTransactionNotification(Transaction transaction) {
        try {
            TransactionNotificationEvent event = new TransactionNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setTransactionId(transaction.getTransactionId());
            event.setIdClient(transaction.getIdClient());
            event.setType(transaction.getType());
            event.setMontant(transaction.getMontant());
            event.setFrais(transaction.getFrais());
            event.setCompteSource(transaction.getCompteSource());
            event.setCompteDestination(transaction.getCompteDestination());
            event.setStatus(transaction.getStatus());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "transaction.completed", event);
            log.info("Notification transaction envoyée: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Erreur envoi notification transaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Notification d'alerte fraude
     */
    public void sendFraudAlertNotification(String idClient, String alertType, String details) {
        try {
            FraudAlertNotificationEvent event = new FraudAlertNotificationEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setIdClient(idClient);
            event.setAlertType(alertType);
            event.setDetails(details);
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, "fraud.alert", event);
            log.warn("Alerte fraude envoyée pour client: {} - Type: {}", idClient, alertType);
        } catch (Exception e) {
            log.error("Erreur envoi alerte fraude: {}", e.getMessage(), e);
        }
    }
}

