package com.serviceAgence.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.serviceAgence.enums.TransactionStatus;
import com.serviceAgence.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "ID transaction obligatoire")
    private String transactionId;

    @NotNull(message = "Type transaction obligatoire")
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01", message = "Montant doit être positif")
    private BigDecimal montant;

    @NotNull
    @DecimalMin(value = "0.0", message = "Frais ne peuvent être négatifs")
    private BigDecimal frais = BigDecimal.ZERO;

    @NotNull
    private BigDecimal montantNet; // montant - frais

    // Comptes impliqués
    @NotBlank(message = "Compte source obligatoire")
    @Indexed
    private String compteSource;

    @Indexed
    private String compteDestination; // Null pour dépôts/retraits

    @NotBlank(message = "ID agence obligatoire")
    @Indexed
    private String idAgence;

    @NotBlank(message = "ID client obligatoire")
    @Indexed
    private String idClient;

    // Statut et métadonnées
    @NotNull
    private TransactionStatus status = TransactionStatus.PENDING;

    private String description;
    private String referenceExterne; // Pour Mobile Money, etc.

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private String processedBy;

    // Soldes avant/après pour audit
    private BigDecimal soldeAvantSource;
    private BigDecimal soldeApresSource;
    private BigDecimal soldeAvantDestination;
    private BigDecimal soldeApresDestination;

    // Géolocalisation et sécurité
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;

    // Gestion d'erreurs
    private String errorCode;
    private String errorMessage;
    private Integer retryCount = 0;
    private LocalDateTime lastRetryAt;

    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (montantNet == null) {
            montantNet = montant.subtract(frais);
        }
    }

    // Méthodes utilitaires
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    public boolean isProcessing() {
        return status == TransactionStatus.PROCESSING;
    }

    public void markAsProcessing(String processedBy) {
        this.status = TransactionStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    public void markAsCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorCode, String errorMessage) {
        this.status = TransactionStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    public boolean canRetry() {
        return retryCount < 3 && isFailed();
    }

    public boolean requiresDestination() {
        return type == TransactionType.TRANSFERT_INTERNE || 
               type == TransactionType.TRANSFERT_EXTERNE;
    }

    public long getDurationInSeconds() {
        if (createdAt != null && completedAt != null) {
            return java.time.Duration.between(createdAt, completedAt).getSeconds();
        }
        return 0;
    }
}
