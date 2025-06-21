package com.wallet.money.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.wallet.money.carteclient.CallbackPayload;
import com.wallet.money.entity.Transaction;
import com.wallet.money.repository.TransactionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TransactionService {

    @Autowired
    public TransactionRepository transactionRepository;
    // AJOUTER cette méthode dans votre classe existante :

    @Autowired
    private CardServiceClient cardServiceClient;

    public void updateStatusFromWebhookWithCardNotification(String reference, String status, String reason) {
        Transaction transaction = transactionRepository.findByFreemoReference(reference).orElse(null);

        if (transaction == null) {
            log.warn("⚠️ Transaction non trouvée pour référence: {}", reference);
            return;
        }

        if (!"PENDING".equals(transaction.getStatus())) {
            log.info("🔄 Transaction {} déjà traitée", transaction.getExternalId());
            return;
        }

        String newStatus;
        String clientAction;

        switch (status.toUpperCase()) {
            case "SUCCESS":
            case "SUCCES":
                newStatus = "SUCCESS";
                clientAction = "VALIDATED";
                break;
            case "FAILED":
                if (reason != null && reason.toLowerCase().contains("cancelled")) {
                    newStatus = "CANCELLED";
                    clientAction = "CANCELLED";
                } else {
                    newStatus = "FAILED";
                    clientAction = "TECHNICAL_ERROR";
                }
                break;
            default:
                newStatus = "FAILED";
                clientAction = "UNKNOWN";
        }

        transaction.setStatus(newStatus);
        transaction.setClientAction(clientAction);
        transaction.setCancellationReason(reason);
        transaction.setValidationTimestamp(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Notifier le service Carte si c'est une recharge
        if ("CARD_RECHARGE".equals(transaction.getType()) && transaction.getCallbackUrl() != null) {
            try {
                CallbackPayload payload = new CallbackPayload();
                payload.setRequestId(transaction.getExternalId());
                payload.setIdCarte(transaction.getIdCarte());
                payload.setStatus(newStatus);
                payload.setClientAction(clientAction);
                payload.setMontant(transaction.getAmount());
                payload.setTransactionId(transaction.getFreemoReference());
                payload.setCancellationReason(transaction.getCancellationReason());
                payload.setTimestamp(LocalDateTime.now());

                cardServiceClient.sendRechargeCallback(transaction.getCallbackUrl(), payload);

            } catch (Exception e) {
                log.error("❌ Erreur notification service Carte: {}", e.getMessage());
                transaction.setCallbackRetries(transaction.getCallbackRetries() + 1);
                transactionRepository.save(transaction);
            }
        }
    }

    /**
     * Créer une transaction en attente
     */
    public Transaction createPendingDeposit(String clientId, String phoneNumber, double amount) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId);
        transaction.setExternalId(generateExternalId(clientId));
        transaction.setPhoneNumber(phoneNumber);
        transaction.setAmount(BigDecimal.valueOf(amount));
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        transaction = transactionRepository.save(transaction);
        log.info("💾 Transaction créée: {}", transaction.getExternalId());

        return transaction;
    }

    /**
     * Mettre à jour avec la référence FreemoPay
     */
    public void updateFreemoReference(String transactionId, String freemoReference) {
        Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
        if (transaction != null) {
            transaction.setFreemoReference(freemoReference);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        }
    }

    /**
     * Mettre à jour le statut depuis le webhook
     */
    public void updateStatusFromWebhook(String reference, String status) {
        // Chercher par référence FreemoPay
        Transaction transaction = transactionRepository.findByFreemoReference(reference).orElse(null);

        if (transaction == null) {
            log.warn("⚠️ Transaction non trouvée pour référence: {}", reference);
            return;
        }

        // Seulement si encore PENDING
        if (!"PENDING".equals(transaction.getStatus())) {
            log.info("🔄 Transaction {} déjà traitée", transaction.getExternalId());
            return;
        }

        // Mettre à jour le statut
        String newStatus = "SUCCESS".equalsIgnoreCase(status) || "SUCCES".equalsIgnoreCase(status) ? "SUCCESS"
                : "FAILED";
        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        log.info("✅ Transaction mise à jour - ExternalId: {} | Statut: {}",
                transaction.getExternalId(), newStatus);
    }

    /**
     * Trouver une transaction par externalId
     */
    public Transaction findByExternalId(String externalId) {
        return transactionRepository.findByExternalId(externalId).orElse(null);
    }

    private String generateExternalId(String clientId) {
        return "DEP_" + clientId + "_" + System.currentTimeMillis();
    }

    // AJOUTER cette méthode
    @Scheduled(fixedRate = 60000) // Chaque minute
    public void checkExpiredTransactions() {
        LocalDateTime expired = LocalDateTime.now().minusMinutes(10);
        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusAndCreatedAtBefore("PENDING", expired);

        for (Transaction t : pendingTransactions) {
            t.setStatus("EXPIRED");
            t.setFailureReason("Timeout - Non validé par le client");
            transactionRepository.save(t);

        }
    }

}
