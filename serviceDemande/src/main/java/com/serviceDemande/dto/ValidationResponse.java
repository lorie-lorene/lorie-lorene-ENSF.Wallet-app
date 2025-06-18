package com.serviceDemande.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.serviceDemande.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResponse {
    private String eventId;
    private String idClient;
    private String idAgence;
    private String email;
    private String statut; // APPROVED, REJECTED, MANUAL_REVIEW
    private String probleme;
    private String message;
    
    // Limites définies si approuvé
    private BigDecimal limiteDailyWithdrawal;
    private BigDecimal limiteDailyTransfer;
    private BigDecimal limiteMonthlyOperations;
    
    // Métadonnées
    private Integer riskScore;
    private String riskLevel;
    private LocalDateTime timestamp;
    private String targetService = "ServiceAgence";
    
    public static ValidationResponse approved(String eventId, String idClient, String idAgence, 
                                           String email, TransactionLimits limits, int riskScore) {
        ValidationResponse response = new ValidationResponse();
        response.setEventId(eventId);
        response.setIdClient(idClient);
        response.setIdAgence(idAgence);
        response.setEmail(email);
        response.setStatut("APPROVED");
        response.setMessage("Demande approuvée après analyse complète");
        response.setLimiteDailyWithdrawal(limits.getDailyWithdrawal());
        response.setLimiteDailyTransfer(limits.getDailyTransfer());
        response.setLimiteMonthlyOperations(limits.getMonthlyOperations());
        response.setRiskScore(riskScore);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static ValidationResponse rejected(String eventId, String idClient, String idAgence, 
                                            String email, String errorCode, String reason) {
        ValidationResponse response = new ValidationResponse();
        response.setEventId(eventId);
        response.setIdClient(idClient);
        response.setIdAgence(idAgence);
        response.setEmail(email);
        response.setStatut("REJECTED");
        response.setProbleme(errorCode);
        response.setMessage(reason);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static ValidationResponse manualReview(String eventId, String idClient, String idAgence, 
                                                 String email, String reason) {
        ValidationResponse response = new ValidationResponse();
        response.setEventId(eventId);
        response.setIdClient(idClient);
        response.setIdAgence(idAgence);
        response.setEmail(email);
        response.setStatut("MANUAL_REVIEW");
        response.setMessage(reason);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}

