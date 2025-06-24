package com.serviceAgence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Résultat du traitement d'une demande d'enregistrement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationProcessingResult {
    private boolean accepted;
    private String status;      // Status code for tracking
    private String errorCode;
    private String message;
    private String clientId;
    private Long numeroCompte;  // Si compte créé
    private LocalDateTime timestamp;
    
    /**
     * Résultat d'acceptation avec création de compte
     */
    public static RegistrationProcessingResult accepted(Long numeroCompte, String message) {
        return RegistrationProcessingResult.builder()
                .accepted(true)
                .status("ACCEPTED")
                .message(message)
                .numeroCompte(numeroCompte)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Résultat de rejet
     */
    public static RegistrationProcessingResult rejected(String errorCode, String message) {
        return RegistrationProcessingResult.builder()
                .accepted(false)
                .status("REJECTED")
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Résultat en attente d'approbation manuelle
     */
    public static RegistrationProcessingResult pendingManualApproval(String clientId, String message) {
        return RegistrationProcessingResult.builder()
                .accepted(false)
                .status("PENDING_MANUAL_APPROVAL")
                .message(message)
                .clientId(clientId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Vérifier si la demande est acceptée
     */
    public boolean isAccepted() {
        return accepted;
    }
    
    /**
     * Vérifier si en attente d'approbation
     */
    public boolean isPendingApproval() {
        return "PENDING_MANUAL_APPROVAL".equals(status);
    }
    
    /**
     * Vérifier si rejetée
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}