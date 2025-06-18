package com.serviceDemande.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.serviceDemande.enums.ActionType;
import com.serviceDemande.enums.DemandeStatus;
import com.serviceDemande.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "demandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Demande {
    @Id
    private String id;
    
    // Identifiants
    @Indexed
    private String idClient;
    @Indexed
    private String idAgence;
    private String eventId; // ID de la demande originale
    
    // Données client
    @Indexed(unique = true)
    private String cni;
    @Indexed
    private String email;
    private String nom;
    private String prenom;
    private String numero;
    
    // Documents sécurisés
    private String rectoCniHash;
    private String versoCniHash;
    private String documentsEncryptionKey;
    
    // Statut et timing
    private DemandeStatus status = DemandeStatus.RECEIVED;
    private LocalDateTime createdAt;
    private LocalDateTime analyzedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime expiresAt;
    
    // Analyse de risque
    private Integer riskScore = 0;
    private RiskLevel riskLevel = RiskLevel.LOW;
    private List<String> fraudFlags = new ArrayList<>();
    private String rejectionReason;
    
    // Limites définies par Demande
    private BigDecimal limiteDailyWithdrawal;
    private BigDecimal limiteDailyTransfer;
    private BigDecimal limiteMonthlyOperations;
    
    // Supervision
    private Boolean requiresManualReview = false;
    private String assignedReviewer;
    private String reviewerNotes;
    
    // Audit trail
    private List<DemandeAction> actionHistory = new ArrayList<>();
    
    // Validation initiale Agence
    private ValidationDetails agenceValidation;
    
    public void addAction(ActionType actionType, String description, String performedBy) {
        DemandeAction action = new DemandeAction(
            actionType, description, performedBy, LocalDateTime.now()
        );
        this.actionHistory.add(action);
    }
    
    public void updateStatus(DemandeStatus newStatus, String reason, String updatedBy) {
        this.status = newStatus;
        addAction(ActionType.valueOf(newStatus.name()), reason, updatedBy);
        
        if (newStatus == DemandeStatus.APPROVED) {
            this.approvedAt = LocalDateTime.now();
        } else if (newStatus == DemandeStatus.ANALYZING) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
