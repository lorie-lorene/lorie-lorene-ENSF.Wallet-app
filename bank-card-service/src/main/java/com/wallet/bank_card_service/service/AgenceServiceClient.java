package com.wallet.bank_card_service.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AgenceServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${agence.service.url:http://localhost:8092}")
    private String agenceServiceUrl;
    @Value("${money.service.url:http://localhost:8095}")
    private String moneyServiceUrl;

    /**
     * Vérifier qu'un compte appartient à un client
     */
    public boolean verifyAccountOwnership(String numeroCompte, String clientId) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/comptes/" + numeroCompte;

            log.info("🔍 Vérification propriété compte: {} pour client: {}", numeroCompte, clientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "bank-card-service");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<CompteDetails> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CompteDetails.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                CompteDetails compte = response.getBody();
                boolean isOwner = clientId.equals(compte.getIdClient());

                log.info("✅ Vérification terminée: compte={}, client={}, propriétaire={}",
                        numeroCompte, clientId, isOwner);

                return isOwner;
            }

            log.warn("⚠️ Compte {} non trouvé ou réponse invalide", numeroCompte);
            return false;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("❌ Compte {} n'existe pas", numeroCompte);
            return false;
        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP {} lors de la vérification du compte {}: {}",
                    e.getStatusCode(), numeroCompte, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur vérification propriété compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifier qu'un compte est actif
     */
    public boolean isAccountActive(String numeroCompte) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/comptes/" + numeroCompte;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<CompteDetails> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CompteDetails.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                CompteDetails compte = response.getBody();
                boolean isActive = "ACTIVE".equals(compte.getStatus());

                log.info("✅ Statut compte {}: {}", numeroCompte, compte.getStatus());
                return isActive;
            }

            return false;

        } catch (RestClientException e) {
            log.error("❌ Erreur vérification statut compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Débiter un compte pour les frais ou transferts
     */
    public boolean debitAccount(String numeroCompte, BigDecimal montant, String description, String idAgence) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/transactions";

            Map<String, Object> request = new HashMap<>();
            request.put("type", "RETRAIT_PHYSIQUE");
            request.put("compteSource", numeroCompte);
            request.put("montant", montant);
            request.put("description", description);
            request.put("idAgence", idAgence);
            try {
                Map<String, Object> compteDetails = getAccountDetailsMap(numeroCompte);
                String idClient = (String) compteDetails.get("idClient");
                request.put("idClient", idClient);
            } catch (Exception e) {
                log.warn("⚠️ Impossible de récupérer idClient, utilisation valeur par défaut");
                request.put("idClient", "1"); // Valeur par défaut temporaire
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "bank-card-service");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("🔄 Envoi requête débit: compte={}, montant={}, agence={}",
                    numeroCompte, montant, idAgence);
            log.debug("📤 Payload envoyé: {}", request);

            ResponseEntity<TransactionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, TransactionResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TransactionResponse transactionResult = response.getBody();

                log.info("✅ Débit réussi: {} FCFA du compte {} - Transaction: {}",
                        montant, numeroCompte, transactionResult.getTransactionId());

                return transactionResult.isSuccess();
            }

            log.warn("⚠️ Réponse inattendue: status={}", response.getStatusCode());
            return false;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP {} lors du débit du compte {}: {}",
                    e.getStatusCode(), numeroCompte, e.getResponseBodyAsString());
            return false;
        } catch (RestClientException e) {
            log.error("❌ Erreur débit compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Récupérer les détails complets d'un compte
     */
    public Map<String, Object> getAccountDetailsMap(String numeroCompte) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/comptes/" + numeroCompte;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "bank-card-service");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> compteDetails = response.getBody();

                log.debug("✅ Détails compte récupérés: {}", compteDetails);
                return compteDetails;
            }

            throw new RuntimeException("Compte non trouvé ou réponse invalide");

        } catch (HttpClientErrorException.NotFound e) {
            log.error("❌ Compte {} n'existe pas", numeroCompte);
            throw new RuntimeException("Compte non trouvé: " + numeroCompte);
        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP {} récupération compte {}: {}",
                    e.getStatusCode(), numeroCompte, e.getResponseBodyAsString());
            throw new RuntimeException("Erreur service agence: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("❌ Erreur récupération détails compte {}: {}", numeroCompte, e.getMessage());
            throw new RuntimeException("Erreur technique récupération compte");
        }
    }

    /**
     * Débiter spécifiquement pour les frais
     */
    /**
     * Débiter spécifiquement pour les frais
     */
    public boolean debitAccountFees(String numeroCompte, BigDecimal frais, String typeFrais, String idAgence) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/transactions";

            // 1. Récupérer idClient depuis le compte
            Map<String, Object> compteDetails = getAccountDetailsMap(numeroCompte);
            String idClient = (String) compteDetails.get("idClient");

            Map<String, Object> request = new HashMap<>();
            request.put("type", "RETRAIT_PHYSIQUE");
            request.put("compteSource", numeroCompte);
            request.put("montant", frais);
            request.put("description", "Frais: " + typeFrais);

            // ✅ CORRECTION: Bon mapping des champs
            request.put("idAgence", idAgence); // PAS "agence"
            request.put("idClient", idClient); // Requis

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "bank-card-service");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("🔄 Envoi requête débit frais: compte={}, client={}, agence={}, montant={}",
                    numeroCompte, idClient, idAgence, frais);

            ResponseEntity<TransactionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, TransactionResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TransactionResponse transactionResult = response.getBody();

                log.info("✅ Débit frais réussi: {} FCFA du compte {} - Transaction: {}",
                        frais, numeroCompte, transactionResult.getTransactionId());

                return transactionResult.isSuccess();
            }

            return false;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP {} lors du débit frais: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur débit frais compte {}: {}", numeroCompte, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Créditer un compte
     */
    public boolean creditAccount(String numeroCompte, BigDecimal montant, String description) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/transactions";

            Map<String, Object> request = new HashMap<>();
            request.put("type", "CREDIT_DEPUIS_CARTE");
            request.put("compteDestination", numeroCompte);
            request.put("montant", montant);
            request.put("description", description);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "bank-card-service");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<TransactionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, TransactionResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TransactionResponse transactionResult = response.getBody();

                log.info("✅ Crédit réussi: {} FCFA vers compte {} - Transaction: {}",
                        montant, numeroCompte, transactionResult.getTransactionId());

                return transactionResult.isSuccess();
            }

            return false;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP {} lors du crédit du compte {}: {}",
                    e.getStatusCode(), numeroCompte, e.getMessage());
            return false;
        } catch (RestClientException e) {
            log.error("❌ Erreur crédit compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Récupérer les détails complets d'un compte
     */
    public Map<String, Object> getAccountDetailsMap2(String numeroCompte) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/comptes/" + numeroCompte;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Name", "bank-card-service");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> compteDetails = response.getBody();

                log.info("✅ Détails compte récupérés: {}", numeroCompte);
                return compteDetails;
            }

            throw new RuntimeException("Compte non trouvé");

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("❌ Compte {} n'existe pas", numeroCompte);
            throw new RuntimeException("Compte non trouvé");
        } catch (Exception e) {
            log.error("❌ Erreur récupération détails compte {}: {}", numeroCompte, e.getMessage());
            throw new RuntimeException("Erreur technique");
        }
    }

    /**
     * Récupérer le solde d'un compte
     */
    public BigDecimal getAccountBalance(String numeroCompte) {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/comptes/" + numeroCompte + "/solde";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<BalanceResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, BalanceResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                BalanceResponse balanceResult = response.getBody();

                log.info("✅ Solde récupéré pour compte {}: {} FCFA",
                        numeroCompte, balanceResult.getSolde());

                return balanceResult.getSolde();
            }

            return BigDecimal.ZERO;

        } catch (HttpClientErrorException e) {
            log.error("❌ Erreur HTTP {} lors de la récupération du solde {}: {}",
                    e.getStatusCode(), numeroCompte, e.getMessage());
            return BigDecimal.ZERO;
        } catch (RestClientException e) {
            log.error("❌ Erreur récupération solde {}: {}", numeroCompte, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Test de connectivité avec le service agence
     */
    public boolean testConnection() {
        try {
            String url = agenceServiceUrl + "/api/v1/agence/health";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            boolean isUp = response.getStatusCode() == HttpStatus.OK;
            log.info("🔗 Test connexion service agence: {}", isUp ? "✅ OK" : "❌ KO");

            return isUp;

        } catch (Exception e) {
            log.error("❌ Service agence inaccessible: {}", e.getMessage());
            return false;
        }
    }

    public boolean testConnection2() {
        try {
            String url = moneyServiceUrl + "/api/money/card-recharge/health";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            boolean isUp = response.getStatusCode() == HttpStatus.OK;
            log.info("🔗 Test connexion service money: {}", isUp ? "✅ OK" : "❌ KO");

            return isUp;

        } catch (Exception e) {
            log.error("❌ Service agence inaccessible: {}", e.getMessage());
            return false;
        }
    }

    // Classes internes pour les réponses API
    @Data
    public static class CompteDetails {
        private String numeroCompte;
        private String idClient;
        private String idAgence;
        private String status;
        private BigDecimal solde;

        // Getters et setters
        public String getNumeroCompte() {
            return numeroCompte;
        }

        public void setNumeroCompte(String numeroCompte) {
            this.numeroCompte = numeroCompte;
        }

        public String getIdClient() {
            return idClient;
        }

        public void setIdClient(String idClient) {
            this.idClient = idClient;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getSolde() {
            return solde;
        }

        public void setSolde(BigDecimal solde) {
            this.solde = solde;
        }
    }

    public static class TransactionResponse {
        private boolean success;
        private String transactionId;
        private String message;

        // Getters et setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class BalanceResponse {
        private String numeroCompte;
        private BigDecimal solde;
        private String devise;

        // Getters et setters
        public String getNumeroCompte() {
            return numeroCompte;
        }

        public void setNumeroCompte(String numeroCompte) {
            this.numeroCompte = numeroCompte;
        }

        public BigDecimal getSolde() {
            return solde;
        }

        public void setSolde(BigDecimal solde) {
            this.solde = solde;
        }

        public String getDevise() {
            return devise;
        }

        public void setDevise(String devise) {
            this.devise = devise;
        }
    }
}