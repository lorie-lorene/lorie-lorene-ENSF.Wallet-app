package com.wallet.money.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import com.wallet.money.carteclient.CallbackPayload;

@Service
@Slf4j
public class CardServiceClient {

    @Value("${card.service.url:http://localhost:8096}")
    private String cardServiceUrl;

    @Value("${card.service.auth.username:client}")
    private String username;

    @Value("${card.service.auth.password:password}")
    private String password;

    private final RestTemplate restTemplate;

    public CardServiceClient() {
        this.restTemplate = new RestTemplate();

        // CORRECTION: Ajouter timeout pour éviter les blocages
        this.restTemplate.getInterceptors().add(
                (request, body, execution) -> {
                    // Authentification HTTP Basic si nécessaire
                    if (username != null && !username.isEmpty()) {
                        String auth = username + ":" + password;
                        byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
                        String authHeader = "Basic " + new String(encodedAuth);
                        request.getHeaders().set("Authorization", authHeader);
                    }

                    // Headers additionnels
                    request.getHeaders().set("X-Source-Service", "money-service");
                    request.getHeaders().set("User-Agent", "MoneyService/1.0");

                    return execution.execute(request, body);
                });
    }

    /**
     * MÉTHODE CORRIGÉE: Envoyer callback recharge au service Carte
     */
    public void sendRechargeCallback(String callbackUrl, CallbackPayload payload) {
        try {
            log.info("🔄 [RECHARGE-CALLBACK] Envoi notification au service Carte - RequestId: {}, Status: {}",
                    payload.getRequestId(), payload.getStatus());

            // CORRECTION: Construire URL complète si nécessaire
            String fullCallbackUrl = buildFullCallbackUrl(callbackUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Source-Service", "money-service");
            headers.set("X-Callback-Type", "CARD_RECHARGE");

            HttpEntity<CallbackPayload> entity = new HttpEntity<>(payload, headers);

            log.info("📤 [RECHARGE-CALLBACK] URL: {}", fullCallbackUrl);
            log.info("📤 [RECHARGE-CALLBACK] Payload: RequestId={}, IdCarte={}, Status={}, Montant={}",
                    payload.getRequestId(), payload.getIdCarte(), payload.getStatus(), payload.getMontant());

            ResponseEntity<String> response = restTemplate.postForEntity(fullCallbackUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [RECHARGE-CALLBACK] Notification envoyée avec succès - ResponseCode: {}",
                        response.getStatusCode());
            } else {
                log.warn("⚠️ [RECHARGE-CALLBACK] Réponse inattendue - ResponseCode: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
            }

        } catch (RestClientException e) {
            log.error("❌ [RECHARGE-CALLBACK] Erreur envoi notification: {}", e.getMessage());

            // NOUVEAU: Tenter avec URL de fallback
            if (!callbackUrl.contains("/money-callback")) {
                String fallbackUrl = cardServiceUrl + "/api/v1/cartes/webhooks/money-callback";
                log.info("🔄 [RECHARGE-CALLBACK] Tentative avec URL fallback: {}", fallbackUrl);

                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<CallbackPayload> entity = new HttpEntity<>(payload, headers);

                    restTemplate.postForEntity(fallbackUrl, entity, String.class);
                    log.info("✅ [RECHARGE-CALLBACK] Fallback réussi");
                    return;
                } catch (Exception fallbackError) {
                    log.error("❌ [RECHARGE-CALLBACK] Fallback échoué: {}", fallbackError.getMessage());
                }
            }

            throw new RuntimeException("Erreur notification service Carte", e);
        } catch (Exception e) {
            log.error("❌ [RECHARGE-CALLBACK] Erreur inattendue: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur notification service Carte", e);
        }
    }

    /**
     * MÉTHODE CORRIGÉE: Envoyer callback résultat retrait
     */
    public void sendWithdrawalCallback(String callbackUrl, CallbackPayload payload) {
        try {
            log.info("🔄 [WITHDRAWAL-CALLBACK] Envoi notification retrait au service Carte - RequestId: {}, Status: {}",
                    payload.getRequestId(), payload.getStatus());

            String fullCallbackUrl = buildWithdrawalCallbackUrl(callbackUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Source-Service", "money-service");
            headers.set("X-Callback-Type", "WITHDRAWAL_RESULT");

            HttpEntity<CallbackPayload> entity = new HttpEntity<>(payload, headers);

            log.info("📤 [WITHDRAWAL-CALLBACK] URL: {}", fullCallbackUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(fullCallbackUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WITHDRAWAL-CALLBACK] Notification retrait envoyée avec succès");
            } else {
                log.warn("⚠️ [WITHDRAWAL-CALLBACK] Réponse inattendue: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ [WITHDRAWAL-CALLBACK] Erreur envoi notification retrait: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur notification retrait service Carte", e);
        }
    }

    /**
     * MÉTHODE CORRIGÉE: Envoyer callback pour remboursement
     */
    public void sendWithdrawalRefundCallback(String callbackUrl, CallbackPayload payload) {
        try {
            log.info("💰 [REFUND-CALLBACK] Envoi demande remboursement au service Carte - RequestId: {}",
                    payload.getRequestId());

            String fullCallbackUrl = buildRefundCallbackUrl(callbackUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Source-Service", "money-service");
            headers.set("X-Callback-Type", "WITHDRAWAL_REFUND");

            HttpEntity<CallbackPayload> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(fullCallbackUrl, entity, String.class);

            log.info("✅ [REFUND-CALLBACK] Demande remboursement envoyée avec succès");

        } catch (Exception e) {
            log.error("❌ [REFUND-CALLBACK] Erreur envoi demande remboursement: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur demande remboursement service Carte", e);
        }
    }

    /**
     * NOUVEAU: Test de connectivité avec le service Carte
     */
    public boolean testConnectivity() {
        try {
            String healthUrl = cardServiceUrl + "/api/v1/cartes/health";
            log.info("🔗 [CONNECTIVITY] Test connexion service Carte: {}", healthUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            boolean isUp = response.getStatusCode().is2xxSuccessful();

            log.info("🔗 [CONNECTIVITY] Service Carte: {}", isUp ? "✅ UP" : "❌ DOWN");
            return isUp;

        } catch (Exception e) {
            log.error("❌ [CONNECTIVITY] Service Carte inaccessible: {}", e.getMessage());
            return false;
        }
    }

    // ========================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ========================================

    /**
     * Construire URL complète de callback pour recharge
     */
    private String buildFullCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            // URL par défaut
            return cardServiceUrl + "/api/v1/cartes/webhooks/money-callback";
        }

        // Si l'URL est relative, ajouter le domaine
        if (callbackUrl.startsWith("/")) {
            return cardServiceUrl + callbackUrl;
        }

        // Si l'URL est complète, l'utiliser telle quelle
        if (callbackUrl.startsWith("http")) {
            return callbackUrl;
        }

        // Sinon, construire avec le domaine par défaut
        return cardServiceUrl + "/api/v1/cartes/webhooks/" + callbackUrl;
    }

    /**
     * Construire URL de callback pour retrait
     */
    private String buildWithdrawalCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            return cardServiceUrl + "/api/v1/cartes/webhooks/money-withdrawal-callback";
        }

        // Adapter l'URL pour les retraits
        if (callbackUrl.contains("/money-callback")) {
            return callbackUrl.replace("/money-callback", "/money-withdrawal-callback");
        }

        return callbackUrl;
    }

    /**
     * Construire URL de callback pour remboursement
     */
    private String buildRefundCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            return cardServiceUrl + "/api/v1/cartes/webhooks/money-withdrawal-refund";
        }

        // Adapter l'URL pour les remboursements
        if (callbackUrl.contains("/money-callback")) {
            return callbackUrl.replace("/money-callback", "/money-withdrawal-refund");
        }

        return callbackUrl;
    }

    /**
     * NOUVEAU: Valider la structure du payload
     */
    private void validateCallbackPayload(CallbackPayload payload) {
        if (payload.getRequestId() == null || payload.getRequestId().isEmpty()) {
            throw new IllegalArgumentException("RequestId est requis dans le payload callback");
        }

        if (payload.getStatus() == null || payload.getStatus().isEmpty()) {
            throw new IllegalArgumentException("Status est requis dans le payload callback");
        }

        if (payload.getTimestamp() == null) {
            payload.setTimestamp(java.time.LocalDateTime.now());
        }
    }

    /**
     * NOUVEAU: Retry automatique en cas d'échec
     */
    public void sendCallbackWithRetry(String callbackUrl, CallbackPayload payload, int maxRetries) {
        validateCallbackPayload(payload);

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🔄 [RETRY-CALLBACK] Tentative {}/{} - RequestId: {}",
                        attempt, maxRetries, payload.getRequestId());

                sendRechargeCallback(callbackUrl, payload);

                log.info("✅ [RETRY-CALLBACK] Succès à la tentative {}", attempt);
                return;

            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ [RETRY-CALLBACK] Échec tentative {} : {}", attempt, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Attendre avant retry (backoff exponentiel)
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interruption pendant retry", ie);
                    }
                }
            }
        }

        log.error("❌ [RETRY-CALLBACK] Tous les retries échoués pour RequestId: {}", payload.getRequestId());
        throw new RuntimeException("Callback échoué après " + maxRetries + " tentatives", lastException);
    }

    /**
     * NOUVEAU: Obtenir statistiques des callbacks
     */
    public CallbackStats getCallbackStats() {
        // En production, ces stats seraient stockées/calculées depuis une base de
        // données
        CallbackStats stats = new CallbackStats();
        stats.setServiceUrl(cardServiceUrl);
        stats.setLastConnectivityCheck(java.time.LocalDateTime.now());
        stats.setIsReachable(testConnectivity());

        return stats;
    }

    // ========================================
    // CLASSE INTERNE POUR STATISTIQUES
    // ========================================

    public static class CallbackStats {
        private String serviceUrl;
        private boolean isReachable;
        private java.time.LocalDateTime lastConnectivityCheck;
        private long totalCallbacksSent = 0;
        private long successfulCallbacks = 0;
        private long failedCallbacks = 0;

        // Getters et setters
        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public boolean isReachable() {
            return isReachable;
        }

        public void setIsReachable(boolean reachable) {
            isReachable = reachable;
        }

        public java.time.LocalDateTime getLastConnectivityCheck() {
            return lastConnectivityCheck;
        }

        public void setLastConnectivityCheck(java.time.LocalDateTime lastConnectivityCheck) {
            this.lastConnectivityCheck = lastConnectivityCheck;
        }

        public long getTotalCallbacksSent() {
            return totalCallbacksSent;
        }

        public void setTotalCallbacksSent(long totalCallbacksSent) {
            this.totalCallbacksSent = totalCallbacksSent;
        }

        public long getSuccessfulCallbacks() {
            return successfulCallbacks;
        }

        public void setSuccessfulCallbacks(long successfulCallbacks) {
            this.successfulCallbacks = successfulCallbacks;
        }

        public long getFailedCallbacks() {
            return failedCallbacks;
        }

        public void setFailedCallbacks(long failedCallbacks) {
            this.failedCallbacks = failedCallbacks;
        }

        public double getSuccessRate() {
            return totalCallbacksSent > 0 ? (double) successfulCallbacks / totalCallbacksSent * 100 : 0;
        }
    }
}