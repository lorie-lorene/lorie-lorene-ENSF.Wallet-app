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

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TransactionService {

    @Autowired
    public TransactionRepository transactionRepository;
    // AJOUTER cette méthode dans votre classe existante :

    @Autowired
    private CardServiceClient cardServiceClient;

    public void updateStatusFromWebhookWithCardNotification2(String reference, String status, String reason) {
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
    public Transaction createPendingDeposit2(String clientId, String phoneNumber, double amount) {
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
    public void updateFreemoReference2(String transactionId, String freemoReference) {
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
    public void updateStatusFromWebhook2(String reference, String status) {
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
    public Transaction findByExternalId2(String externalId) {
        return transactionRepository.findByExternalId(externalId).orElse(null);
    }

    private String generateExternalId2(String clientId) {
        return "DEP_" + clientId + "_" + System.currentTimeMillis();
    }

    // AJOUTER cette méthode
    @Scheduled(fixedRate = 60000) // Chaque minute
    public void checkExpiredTransactions2() {
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
    public void updateCardWithdrawalStatusFromWebhook2(String reference, String status, String reason) {
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
    public List<Transaction> getClientCardWithdrawals2(String clientId, String idCarte, int limit) {
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

    // ========================================
    // NOUVELLES MÉTHODES POUR RETRAIT
    // ========================================

    /**
     * Notifier le service Carte du résultat du retrait
     */

    /**
     * Envoyer demande de remboursement au service Carte
     */
    private void sendRefundRequest(Transaction transaction, String reason) {
        try {
            CallbackPayload refundPayload = buildRefundCallbackPayload(transaction, reason);

            log.info("💰 [REFUND-REQUEST] Envoi demande remboursement - RequestId: {}, Montant: {}",
                    refundPayload.getRequestId(), refundPayload.getMontant());

            cardServiceClient.sendWithdrawalRefundCallback(transaction.getCallbackUrl(), refundPayload);

        } catch (Exception e) {
            log.error("❌ [REFUND-REQUEST] Erreur envoi demande remboursement: {}", e.getMessage());
        }
    }

    /**
     * Construire payload pour callback retrait
     */
    private CallbackPayload buildWithdrawalCallbackPayload(Transaction transaction, String status, String clientAction,
            String reason) {
        CallbackPayload payload = new CallbackPayload();
        payload.setRequestId(transaction.getExternalId());
        payload.setIdCarte(transaction.getIdCarte());
        payload.setStatus(status);
        payload.setClientAction(clientAction);
        payload.setMontant(transaction.getAmount());
        payload.setTransactionId(transaction.getFreemoReference());
        payload.setCancellationReason(reason);
        payload.setTimestamp(LocalDateTime.now());
        return payload;
    }

    /**
     * Construire payload pour demande de remboursement
     */
    private CallbackPayload buildRefundCallbackPayload(Transaction transaction, String reason) {
        CallbackPayload payload = new CallbackPayload();
        payload.setRequestId(transaction.getExternalId());
        payload.setIdCarte(transaction.getIdCarte());
        payload.setStatus("REFUND_REQUIRED");
        payload.setClientAction("REFUND");
        payload.setMontant(transaction.getAmount());
        payload.setTransactionId(transaction.getFreemoReference());
        payload.setCancellationReason("Retrait échoué: " + reason);
        payload.setTimestamp(LocalDateTime.now());
        return payload;
    }

    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    // ========================================
    // MÉTHODES EXISTANTES (à conserver)
    // ========================================

    public void updateStatusFromWebhook(String reference, String status) {
        Transaction transaction = transactionRepository.findByFreemoReference(reference).orElse(null);

        if (transaction == null) {
            log.warn("⚠️ Transaction non trouvée pour référence: {}", reference);
            return;
        }

        if (!"PENDING".equals(transaction.getStatus())) {
            log.info("🔄 Transaction {} déjà traitée", transaction.getExternalId());
            return;
        }

        String newStatus = "SUCCESS".equalsIgnoreCase(status) || "SUCCES".equalsIgnoreCase(status) ? "SUCCESS"
                : "FAILED";
        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        log.info("✅ Transaction mise à jour - ExternalId: {} | Statut: {}",
                transaction.getExternalId(), newStatus);
    }

    // ========================================
    // TÂCHES PROGRAMMÉES
    // ========================================

    @Scheduled(fixedRate = 60000) // Chaque minute
    public void checkExpiredTransactions() {
        LocalDateTime expired = LocalDateTime.now().minusMinutes(15); // 15 minutes timeout
        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusAndCreatedAtBefore("PENDING", expired);

        for (Transaction t : pendingTransactions) {
            t.setStatus("EXPIRED");
            t.setFailureReason("Timeout - Non validé par le client");
            t.setClientAction("TIMEOUT");
            t.setValidationTimestamp(LocalDateTime.now());
            transactionRepository.save(t);

            // Si c'est un retrait carte, notifier pour remboursement
            if ("CARD_WITHDRAWAL".equals(t.getType()) && t.getCallbackUrl() != null) {
                sendRefundRequest(t, "Transaction expirée");
            }

            log.info("⏰ Transaction expirée: {}", t.getExternalId());
        }
    }

    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void retryFailedCallbacks() {
        List<Transaction> failedCallbacks = transactionRepository
                .findTransactionsWithFailedCallbacks(3); // Max 3 tentatives

        for (Transaction transaction : failedCallbacks) {
            try {
                if ("CARD_WITHDRAWAL".equals(transaction.getType())) {
                    notifyCardServiceForWithdrawalResult(transaction, transaction.getStatus(),
                            transaction.getClientAction(), transaction.getCancellationReason());
                } else if ("CARD_RECHARGE".equals(transaction.getType())) {
                    notifyCardServiceForRecharge(transaction, transaction.getStatus(),
                            transaction.getClientAction());
                }

                log.info("🔄 Retry callback réussi pour transaction: {}", transaction.getExternalId());

            } catch (Exception e) {
                log.error("❌ Échec retry callback pour transaction {}: {}",
                        transaction.getExternalId(), e.getMessage());
            }
        }
    }

    private void scheduleRetryCallback(Transaction transaction) {
        // En production, utiliser un système de queue comme RabbitMQ
        // Ici, on se contente de loguer
        log.info("⏲️ Programmation retry callback pour transaction: {}", transaction.getExternalId());
    }

    public void updateStatusFromWebhookWithCardNotification(String reference, String status, String reason) {
        log.info("🔄 [WEBHOOK] Traitement webhook - Référence: {}, Statut: {}, Raison: {}", reference, status, reason);

        // CORRECTION: Utiliser la méthode sécurisée pour éviter "non unique result"
        Optional<Transaction> transactionOpt = transactionRepository.findMostRecentByFreemoReference(reference);

        if (transactionOpt.isEmpty()) {
            log.warn("⚠️ [WEBHOOK] Transaction non trouvée pour référence: {}", reference);
            return;
        }

        Transaction transaction = transactionOpt.get();

        if (!"PENDING".equals(transaction.getStatus())) {
            log.info("🔄 [WEBHOOK] Transaction {} déjà traitée (statut: {})",
                    transaction.getExternalId(), transaction.getStatus());
            return;
        }

        String newStatus = determineNewStatus(status, reason);
        String clientAction = determineClientAction(status, reason);

        // Mettre à jour la transaction
        updateTransactionStatus(transaction, newStatus, clientAction, reason);

        log.info("✅ [WEBHOOK] Transaction mise à jour - ExternalId: {}, Ancien statut: PENDING, Nouveau statut: {}",
                transaction.getExternalId(), newStatus);

        // Notifier le service Carte si c'est une recharge
        if ("CARD_RECHARGE".equals(transaction.getType()) && transaction.getCallbackUrl() != null) {
            notifyCardServiceForRecharge(transaction, newStatus, clientAction);
        } else {
            log.warn("⚠️ [WEBHOOK] Pas de callback pour transaction type: {}, CallbackUrl: {}",
                    transaction.getType(), transaction.getCallbackUrl());
        }
    }

    // ========================================
    // MÉTHODE CORRIGÉE POUR LES RETRAITS
    // ========================================

    public void updateCardWithdrawalStatusFromWebhook(String reference, String status, String reason) {
        log.info("🔄 [WITHDRAWAL-WEBHOOK] Traitement webhook retrait - Référence: {}, Statut: {}", reference, status);

        // CORRECTION: Utiliser recherche par type pour éviter confusion
        Optional<Transaction> transactionOpt = transactionRepository
                .findFirstByFreemoReferenceAndTypeOrderByCreatedAtDesc(reference, "CARD_WITHDRAWAL");

        if (transactionOpt.isEmpty()) {
            log.warn("⚠️ [WITHDRAWAL-WEBHOOK] Transaction retrait non trouvée pour référence: {}", reference);
            return;
        }

        Transaction transaction = transactionOpt.get();

        if (!"PENDING".equals(transaction.getStatus())) {
            log.info("🔄 [WITHDRAWAL-WEBHOOK] Transaction retrait {} déjà traitée (statut: {})",
                    transaction.getExternalId(), transaction.getStatus());
            return;
        }

        String newStatus = determineWithdrawalStatus(status, reason);
        String clientAction = determineWithdrawalAction(status, reason);

        // Mettre à jour la transaction
        updateTransactionStatus(transaction, newStatus, clientAction, reason);

        // Notifier le service Carte du résultat
        notifyCardServiceForWithdrawalResult(transaction, newStatus, clientAction, reason);

        log.info("✅ [WITHDRAWAL-WEBHOOK] Webhook retrait traité - ExternalId: {}, Final Status: {}",
                transaction.getExternalId(), newStatus);
    }

    // ========================================
    // MÉTHODE CORRIGÉE POUR NOTIFICATION CARTE
    // ========================================

    private void notifyCardServiceForRecharge(Transaction transaction, String newStatus, String clientAction) {
        try {
            log.info("🔄 [CARD-CALLBACK] Envoi callback au service Carte - ExternalId: {}, Status: {}",
                    transaction.getExternalId(), newStatus);

            CallbackPayload payload = new CallbackPayload();
            payload.setRequestId(transaction.getExternalId());
            payload.setIdCarte(transaction.getIdCarte());
            payload.setStatus(newStatus);
            payload.setClientAction(clientAction);
            payload.setMontant(transaction.getAmount());
            payload.setTransactionId(transaction.getFreemoReference());
            payload.setCancellationReason(transaction.getCancellationReason());
            payload.setTimestamp(LocalDateTime.now());

            // CORRECTION: Utiliser l'URL callback correcte
            String callbackUrl = transaction.getCallbackUrl();
            if (callbackUrl == null || callbackUrl.isEmpty()) {
                // URL par défaut si pas définie
                callbackUrl = "http://localhost:8096/api/v1/cartes/webhooks/money-callback";
                log.warn("⚠️ [CARD-CALLBACK] URL callback manquante, utilisation URL par défaut: {}", callbackUrl);
            }

            cardServiceClient.sendRechargeCallback(callbackUrl, payload);

            log.info("✅ [CARD-CALLBACK] Callback envoyé avec succès au service Carte");

        } catch (Exception e) {
            log.error("❌ [CARD-CALLBACK] Erreur notification service Carte: {}", e.getMessage(), e);

            // Incrémenter compteur d'erreurs
            transaction.setCallbackRetries(transaction.getCallbackRetries() + 1);
            transactionRepository.save(transaction);

            // Programmer retry si pas trop d'échecs
            if (transaction.getCallbackRetries() < 3) {
                log.info("🔄 [CARD-CALLBACK] Programmation retry #{} pour transaction: {}",
                        transaction.getCallbackRetries(), transaction.getExternalId());
            }
        }
    }

    // ========================================
    // MÉTHODES UTILITAIRES AMÉLIORÉES
    // ========================================

    private String determineNewStatus(String status, String reason) {
        if (status == null) {
            log.warn("⚠️ Statut null reçu, considéré comme FAILED");
            return "FAILED";
        }

        switch (status.toUpperCase()) {
            case "SUCCESS":
            case "SUCCES":
            case "COMPLETED":
                return "SUCCESS";
            case "FAILED":
            case "ECHEC":
                if (reason != null && reason.toLowerCase().contains("cancelled")) {
                    return "CANCELLED";
                }
                return "FAILED";
            case "CANCELLED":
            case "ANNULE":
                return "CANCELLED";
            case "PENDING":
                return "PENDING";
            default:
                log.warn("⚠️ Statut inconnu: {}, considéré comme FAILED", status);
                return "FAILED";
        }
    }

    private String determineClientAction(String status, String reason) {
        if (status == null)
            return "TECHNICAL_ERROR";

        switch (status.toUpperCase()) {
            case "SUCCESS":
            case "SUCCES":
            case "COMPLETED":
                return "VALIDATED";
            case "CANCELLED":
            case "ANNULE":
                return "CANCELLED";
            case "FAILED":
            case "ECHEC":
                if (reason != null && reason.toLowerCase().contains("cancelled")) {
                    return "CANCELLED";
                } else if (reason != null && reason.toLowerCase().contains("insufficient")) {
                    return "INSUFFICIENT_FUNDS";
                } else {
                    return "TECHNICAL_ERROR";
                }
            default:
                return "UNKNOWN";
        }
    }

    private void updateTransactionStatus(Transaction transaction, String newStatus, String clientAction,
            String reason) {
        transaction.setStatus(newStatus);
        transaction.setClientAction(clientAction);
        transaction.setCancellationReason(reason);
        transaction.setValidationTimestamp(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);

        log.info("💾 [UPDATE] Transaction sauvegardée - ID: {}, ExternalId: {}, Nouveau statut: {}",
                saved.getId(), saved.getExternalId(), saved.getStatus());
    }

    // ========================================
    // MÉTHODES EXISTANTES (améliorées)
    // ========================================

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
        log.info("💾 [CREATE] Transaction dépôt créée: {}", transaction.getExternalId());

        return transaction;
    }

    public void updateFreemoReference(String transactionId, String freemoReference) {
        log.info("🔗 [UPDATE-REF] Mise à jour référence FreemoPay - TransactionId: {}, FreemoRef: {}",
                transactionId, freemoReference);

        // CORRECTION: Vérifier que la référence FreemoPay n'existe pas déjà
        if (transactionRepository.existsByFreemoReference(freemoReference)) {
            log.warn("⚠️ [UPDATE-REF] Référence FreemoPay {} existe déjà!", freemoReference);
        }

        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setFreemoReference(freemoReference);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            log.info("✅ [UPDATE-REF] Référence FreemoPay mise à jour pour transaction: {}",
                    transaction.getExternalId());
        } else {
            log.error("❌ [UPDATE-REF] Transaction non trouvée avec ID: {}", transactionId);
        }
    }

    public Transaction findByExternalId(String externalId) {
        return transactionRepository.findByExternalId(externalId).orElse(null);
    }

    private String generateExternalId(String clientId) {
        return "DEP_" + clientId + "_" + System.currentTimeMillis();
    }

    // ========================================
    // MÉTHODES DE DIAGNOSTIC ET NETTOYAGE
    // ========================================

    /**
     * Diagnostiquer les problèmes de doublons
     */
    public void diagnoseDuplicates() {
        log.info("🔍 [DIAGNOSTIC] Recherche de doublons...");

        long totalTransactions = transactionRepository.count();
        long transactionsWithExternalId = transactionRepository.countTransactionsWithExternalId();
        long transactionsWithFreemoRef = transactionRepository.countTransactionsWithFreemoReference();

        log.info("📊 [DIAGNOSTIC] Total transactions: {}", totalTransactions);
        log.info("📊 [DIAGNOSTIC] Avec ExternalId: {}", transactionsWithExternalId);
        log.info("📊 [DIAGNOSTIC] Avec FreemoReference: {}", transactionsWithFreemoRef);

        // Rechercher transactions orphelines
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<Transaction> orphaned = transactionRepository.findOrphanedTransactions(cutoff);
        log.info("📊 [DIAGNOSTIC] Transactions orphelines: {}", orphaned.size());

        if (!orphaned.isEmpty()) {
            log.warn("⚠️ [DIAGNOSTIC] Transactions orphelines trouvées:");
            orphaned.forEach(t -> log.warn("  - ExternalId: {}, CreatedAt: {}",
                    t.getExternalId(), t.getCreatedAt()));
        }
    }

    /**
     * Nettoyer les transactions orphelines
     */
    @Scheduled(fixedRate = 3600000) // Chaque heure
    public void cleanupOrphanedTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<Transaction> orphaned = transactionRepository.findOrphanedTransactions(cutoff);

        if (!orphaned.isEmpty()) {
            log.info("🧹 [CLEANUP] Nettoyage de {} transactions orphelines", orphaned.size());

            for (Transaction t : orphaned) {
                t.setStatus("FAILED");
                t.setFailureReason("Transaction orpheline - pas de référence FreemoPay");
                t.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(t);
            }

            log.info("✅ [CLEANUP] Nettoyage terminé");
        }
    }

    // ========================================
    // MÉTHODES POUR LES RETRAITS (existantes)
    // ========================================

    private String determineWithdrawalStatus(String freemoStatus, String reason) {
        if (freemoStatus == null)
            return "FAILED";

        switch (freemoStatus.toUpperCase()) {
            case "SUCCESS":
            case "SUCCES":
            case "COMPLETED":
                return "SUCCESS";
            case "FAILED":
            case "ECHEC":
            case "ERROR":
                return "FAILED";
            case "CANCELLED":
            case "ANNULE":
                return "CANCELLED";
            default:
                return "FAILED";
        }
    }

    private String determineWithdrawalAction(String freemoStatus, String reason) {
        if (freemoStatus == null)
            return "TECHNICAL_ERROR";

        switch (freemoStatus.toUpperCase()) {
            case "SUCCESS":
            case "SUCCES":
                return "COMPLETED";
            case "CANCELLED":
            case "ANNULE":
                return "CANCELLED";
            default:
                return "TECHNICAL_ERROR";
        }
    }

    private void notifyCardServiceForWithdrawalResult(Transaction transaction, String status, String clientAction,
            String reason) {
        try {
            CallbackPayload payload = new CallbackPayload();
            payload.setRequestId(transaction.getExternalId());
            payload.setIdCarte(transaction.getIdCarte());
            payload.setStatus(status);
            payload.setClientAction(clientAction);
            payload.setMontant(transaction.getAmount());
            payload.setTransactionId(transaction.getFreemoReference());
            payload.setCancellationReason(reason);
            payload.setTimestamp(LocalDateTime.now());

            cardServiceClient.sendWithdrawalCallback(transaction.getCallbackUrl(), payload);

        } catch (Exception e) {
            log.error("❌ [WITHDRAWAL-CALLBACK] Erreur notification retrait: {}", e.getMessage());
        }
    }

    public List<Transaction> getClientCardWithdrawals(String clientId, String idCarte, int limit) {
        if (idCarte != null && !idCarte.isEmpty()) {
            return transactionRepository.findByTypeAndIdCarteOrderByCreatedAtDesc(idCarte)
                    .stream()
                    .limit(limit)
                    .toList();
        } else {
            return transactionRepository.findByClientIdAndTypeOrderByCreatedAtDesc(clientId, "CARD_WITHDRAWAL")
                    .stream()
                    .limit(limit)
                    .toList();
        }
    }
}
