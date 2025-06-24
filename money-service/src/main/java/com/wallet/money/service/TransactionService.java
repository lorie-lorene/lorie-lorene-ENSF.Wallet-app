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

    /**
     * NOUVELLE MÉTHODE: Gestion spécifique des webhooks de retrait carte
     */
    public void updateCardWithdrawalStatusFromWebhook(String reference, String status, String reason) {
        Transaction transaction = transactionRepository.findByFreemoReference(reference).orElse(null);

        if (transaction == null) {
            log.warn("⚠️ Transaction retrait carte non trouvée pour référence: {}", reference);
            return;
        }

        // Vérifier que c'est bien un retrait carte
        if (!"CARD_WITHDRAWAL".equals(transaction.getType())) {
            log.warn("⚠️ Transaction {} n'est pas un retrait carte", transaction.getExternalId());
            return;
        }

        if (!"PENDING".equals(transaction.getStatus())) {
            log.info("🔄 Transaction retrait carte {} déjà traitée", transaction.getExternalId());
            return;
        }

        String newStatus;
        String clientAction;

        switch (status.toUpperCase()) {
            case "SUCCESS":
            case "SUCCES":
                newStatus = "SUCCESS";
                clientAction = "COMPLETED";
                log.info("✅ Retrait carte réussi - Transaction: {}", transaction.getExternalId());
                break;
            case "FAILED":
                newStatus = "FAILED";
                clientAction = "FAILED";
                log.warn("❌ Retrait carte échoué - Transaction: {}, Raison: {}", 
                        transaction.getExternalId(), reason);
                
                // IMPORTANT: Notifier le service Carte pour remboursement
                notifyCardServiceForWithdrawalRefund(transaction, reason);
                break;
            default:
                newStatus = "FAILED";
                clientAction = "UNKNOWN";
                log.error("🔧 Statut retrait carte inconnu: {} - Transaction: {}", status, transaction.getExternalId());
                
                // Aussi déclencher un remboursement par sécurité
                notifyCardServiceForWithdrawalRefund(transaction, "Statut inconnu: " + status);
        }

        // Mettre à jour la transaction
        transaction.setStatus(newStatus);
        transaction.setClientAction(clientAction);
        transaction.setCancellationReason(reason);
        transaction.setValidationTimestamp(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Notifier le service Carte du résultat final
        notifyCardServiceWithdrawalResult(transaction, newStatus, clientAction);
    }

    /**
     * NOUVELLE MÉTHODE: Notifier le service Carte du résultat du retrait
     */
    private void notifyCardServiceWithdrawalResult(Transaction transaction, String status, String clientAction) {
        if (transaction.getCallbackUrl() == null) {
            log.warn("⚠️ Pas d'URL callback pour transaction retrait: {}", transaction.getExternalId());
            return;
        }

        try {
            CallbackPayload payload = new CallbackPayload();
            payload.setRequestId(transaction.getExternalId());
            payload.setIdCarte(transaction.getIdCarte());
            payload.setStatus(status);
            payload.setClientAction(clientAction);
            payload.setMontant(transaction.getAmount());
            payload.setTransactionId(transaction.getFreemoReference());
            payload.setCancellationReason(transaction.getCancellationReason());
            payload.setTimestamp(LocalDateTime.now());

            log.info("🔄 [WITHDRAWAL-CALLBACK] Notification service Carte - RequestId: {}, Status: {}", 
                    payload.getRequestId(), payload.getStatus());

            cardServiceClient.sendWithdrawalCallback(transaction.getCallbackUrl(), payload);

        } catch (Exception e) {
            log.error("❌ Erreur notification service Carte pour retrait: {}", e.getMessage());
            transaction.setCallbackRetries(transaction.getCallbackRetries() + 1);
            transactionRepository.save(transaction);
        }
    }

    /**
     * NOUVELLE MÉTHODE: Notifier service Carte pour remboursement en cas d'échec
     */
    private void notifyCardServiceForWithdrawalRefund(Transaction transaction, String reason) {
        if (transaction.getCallbackUrl() == null) {
            log.warn("⚠️ Pas d'URL callback pour remboursement: {}", transaction.getExternalId());
            return;
        }

        try {
            // Payload spécial pour déclencher remboursement
            CallbackPayload refundPayload = new CallbackPayload();
            refundPayload.setRequestId(transaction.getExternalId());
            refundPayload.setIdCarte(transaction.getIdCarte());
            refundPayload.setStatus("REFUND_REQUIRED");
            refundPayload.setClientAction("REFUND");
            refundPayload.setMontant(transaction.getAmount());
            refundPayload.setTransactionId(transaction.getFreemoReference());
            refundPayload.setCancellationReason("Retrait échoué: " + reason);
            refundPayload.setTimestamp(LocalDateTime.now());

            log.info("💰 [REFUND-CALLBACK] Demande remboursement carte - RequestId: {}", 
                    refundPayload.getRequestId());

            cardServiceClient.sendWithdrawalRefundCallback(transaction.getCallbackUrl(), refundPayload);

        } catch (Exception e) {
            log.error("❌ Erreur notification remboursement: {}", e.getMessage());
        }
    }

    /**
     * NOUVELLE MÉTHODE: Créer transaction retrait carte
     */
    public Transaction createCardWithdrawal(String clientId, String idCarte, String phoneNumber, 
            double amount, String provider) {
        Transaction transaction = new Transaction();
        transaction.setClientId(clientId);
        transaction.setExternalId("CARD_WIT_" + idCarte + "_" + System.currentTimeMillis());
        transaction.setPhoneNumber(phoneNumber);
        transaction.setAmount(BigDecimal.valueOf(amount));
        transaction.setType("CARD_WITHDRAWAL");
        transaction.setStatus("PENDING");
        transaction.setIdCarte(idCarte);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Ajouter provider dans les métadonnées si nécessaire
        transaction.setCancellationReason("Provider: " + provider); // Utilisation temporaire du champ

        transaction = transactionRepository.save(transaction);
        log.info("💾 Transaction retrait carte créée: {}", transaction.getExternalId());

        return transaction;
    }

    /**
     * NOUVELLE MÉTHODE: Récupérer historique retraits carte d'un client
     */
    public List<Transaction> getClientCardWithdrawals(String clientId, String idCarte, int limit) {
        if (idCarte != null) {
            return transactionRepository.findByClientIdAndTypeAndIdCarteOrderByCreatedAtDesc(
                    clientId, "CARD_WITHDRAWAL", idCarte)
                    .stream()
                    .limit(limit)
                    .toList();
        } else {
            return transactionRepository.findByClientIdAndTypeOrderByCreatedAtDesc(
                    clientId, "CARD_WITHDRAWAL")
                    .stream()
                    .limit(limit)
                    .toList();
        }
    }

}
