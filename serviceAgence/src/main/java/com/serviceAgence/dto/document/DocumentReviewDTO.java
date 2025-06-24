package com.serviceAgence.dto.document;

import com.serviceAgence.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour la review détaillée d'un document avec images et selfie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReviewDTO {
    private String id;
    private String idClient;
    private String idAgence;
    private String cni;
    
    // Informations extraites du document
    private String nomExtrait;
    private String prenomExtrait;
    private LocalDateTime dateNaissanceExtraite;
    private String lieuNaissanceExtrait;
    
    // Images en Base64 pour affichage admin
    private String rectoImageBase64;
    private String versoImageBase64;
    private String selfieImageBase64;  // ← NEW: Selfie utilisateur
    
    // Informations de qualité et similarité
    private Integer qualityScore;
    private Integer selfieQualityScore;     // ← NEW: Qualité du selfie
    private Integer selfieSimilarityScore;  // ← NEW: Score de similarité faciale
    private Boolean livenessDetected;      // ← NEW: Détection de vie
    
    // Anomalies détectées
    private List<String> anomaliesDetectees;
    private List<String> selfieAnomalies;  // ← NEW: Anomalies spécifiques au selfie
    
    // Métadonnées
    private LocalDateTime uploadedAt;
    private String uploadedFrom;
    private Long fileSize;
    private Long selfieFileSize;  // ← NEW
    private DocumentStatus status;
    
    // Recommandations pour l'admin
    private String facialVerificationRecommendation;  // ← NEW
}