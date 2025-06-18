package com.serviceAgence.model;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.enums.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "documents_kyc")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKYC {
    @Id
    private String id;

    @NotBlank(message = "ID client obligatoire")
    @Indexed
    private String idClient;

    @NotNull(message = "Type document obligatoire")
    private DocumentType type;

    @NotBlank(message = "Numéro document obligatoire")
    private String numeroDocument;

    // Stockage sécurisé des fichiers (chiffrés)
    private String cheminRecto;
    private String cheminVerso;
    private String hashRecto;
    private String hashVerso;

    // Informations extraites du document
    private String nomExtrait;
    private String prenomExtrait;
    private LocalDateTime dateNaissanceExtraite;
    private String lieuNaissanceExtrait;

    // Validation
    @NotNull
    private DocumentStatus status = DocumentStatus.PENDING;
    
    private Integer scoreQualite; // 0-100
    private Boolean fraudDetected = false;
    private List<String> anomaliesDetectees;

    // Métadonnées
    private LocalDateTime uploadedAt;
    private LocalDateTime validatedAt;
    private String validatedBy;
    private String rejectionReason;

    // Audit
    private String uploadedFrom; // IP, User-Agent
    private Long fileSize;
    private String originalFileName;

    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DocumentStatus.PENDING;
        }
        if (fraudDetected == null) {
            fraudDetected = false;
        }
    }

    // Méthodes utilitaires
    public boolean isValid() {
        return status == DocumentStatus.VALIDATED && !fraudDetected;
    }

    public boolean isPending() {
        return status == DocumentStatus.PENDING;
    }

    public boolean isRejected() {
        return status == DocumentStatus.REJECTED;
    }

    public void markAsValidated(String validatedBy) {
        this.status = DocumentStatus.VALIDATED;
        this.validatedAt = LocalDateTime.now();
        this.validatedBy = validatedBy;
    }

    public void markAsRejected(String reason, String rejectedBy) {
        this.status = DocumentStatus.REJECTED;
        this.rejectionReason = reason;
        this.validatedBy = rejectedBy;
        this.validatedAt = LocalDateTime.now();
    }

    public void markAsFraudulent(List<String> anomalies) {
        this.fraudDetected = true;
        this.anomaliesDetectees = anomalies;
        this.status = DocumentStatus.REJECTED;
        this.rejectionReason = "Fraude détectée";
    }

    public boolean hasGoodQuality() {
        return scoreQualite != null && scoreQualite >= 70;
    }
}
