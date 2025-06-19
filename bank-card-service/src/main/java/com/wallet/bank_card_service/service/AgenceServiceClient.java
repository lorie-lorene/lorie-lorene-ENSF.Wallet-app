package com.wallet.bank_card_service.service;


import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AgenceServiceClient {

    @Autowired
    private RestTemplate restTemplate;
    
    private static final String AGENCE_SERVICE_URL = "http://service-agence/api/v1/agence";

    /**
     * Vérifier qu'un compte appartient à un client
     */
    public boolean verifyAccountOwnership(String numeroCompte, String clientId) {
        try {
            String url = AGENCE_SERVICE_URL + "/comptes/" + numeroCompte;
            
            // Supposer que l'API retourne les détails du compte
            CompteDetails compte = restTemplate.getForObject(url, CompteDetails.class);
            
            return compte != null && compte.getIdClient().equals(clientId);
            
        } catch (RestClientException e) {
            log.error("Erreur vérification propriété compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifier qu'un compte est actif
     */
    public boolean isAccountActive(String numeroCompte) {
        try {
            String url = AGENCE_SERVICE_URL + "/comptes/" + numeroCompte;
            
            CompteDetails compte = restTemplate.getForObject(url, CompteDetails.class);
            
            return compte != null && "ACTIVE".equals(compte.getStatus());
            
        } catch (RestClientException e) {
            log.error("Erreur vérification statut compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Débiter un compte pour les frais ou transferts
     */
    public boolean debitAccount(String numeroCompte, BigDecimal montant, String description) {
        try {
            String url = AGENCE_SERVICE_URL + "/transactions";
            
            TransactionRequest request = new TransactionRequest();
            request.setType("DEBIT_CARTE");
            request.setCompteSource(numeroCompte);
            request.setMontant(montant);
            request.setDescription(description);
            
            TransactionResponse response = restTemplate.postForObject(url, request, TransactionResponse.class);
            
            return response != null && response.isSuccess();
            
        } catch (RestClientException e) {
            log.error("Erreur débit compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Débiter spécifiquement pour les frais
     */
    public boolean debitAccountFees(String numeroCompte, BigDecimal frais, String typeFrais) {
        return debitAccount(numeroCompte, frais, "Frais: " + typeFrais);
    }

    /**
     * Créditer un compte
     */
    public boolean creditAccount(String numeroCompte, BigDecimal montant, String description) {
        try {
            String url = AGENCE_SERVICE_URL + "/transactions";
            
            TransactionRequest request = new TransactionRequest();
            request.setType("CREDIT_DEPUIS_CARTE");
            request.setCompteDestination(numeroCompte);
            request.setMontant(montant);
            request.setDescription(description);
            
            TransactionResponse response = restTemplate.postForObject(url, request, TransactionResponse.class);
            
            return response != null && response.isSuccess();
            
        } catch (RestClientException e) {
            log.error("Erreur crédit compte {}: {}", numeroCompte, e.getMessage());
            return false;
        }
    }

    /**
     * Récupérer le solde d'un compte
     */
    public BigDecimal getAccountBalance(String numeroCompte) {
        try {
            String url = AGENCE_SERVICE_URL + "/comptes/" + numeroCompte + "/solde";
            
            BalanceResponse response = restTemplate.getForObject(url, BalanceResponse.class);
            
            return response != null ? response.getSolde() : BigDecimal.ZERO;
            
        } catch (RestClientException e) {
            log.error("Erreur récupération solde {}: {}", numeroCompte, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // Classes internes pour les réponses API
    public static class CompteDetails {
        private String numeroCompte;
        private String idClient;
        private String status;
        private BigDecimal solde;
        
        // Getters et setters
        public String getNumeroCompte() { return numeroCompte; }
        public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }
        
        public String getIdClient() { return idClient; }
        public void setIdClient(String idClient) { this.idClient = idClient; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public BigDecimal getSolde() { return solde; }
        public void setSolde(BigDecimal solde) { this.solde = solde; }
    }
    
    public static class TransactionRequest {
        private String type;
        private String compteSource;
        private String compteDestination;
        private BigDecimal montant;
        private String description;
        
        // Getters et setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getCompteSource() { return compteSource; }
        public void setCompteSource(String compteSource) { this.compteSource = compteSource; }
        
        public String getCompteDestination() { return compteDestination; }
        public void setCompteDestination(String compteDestination) { this.compteDestination = compteDestination; }
        
        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class TransactionResponse {
        private boolean success;
        private String transactionId;
        private String message;
        
        // Getters et setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class BalanceResponse {
        private String numeroCompte;
        private BigDecimal solde;
        private String devise;
        
        // Getters et setters
        public String getNumeroCompte() { return numeroCompte; }
        public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }
        
        public BigDecimal getSolde() { return solde; }
        public void setSolde(BigDecimal solde) { this.solde = solde; }
        
        public String getDevise() { return devise; }
        public void setDevise(String devise) { this.devise = devise; }
    }
}
