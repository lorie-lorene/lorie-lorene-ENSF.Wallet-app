package com.serviceAgence.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les statistiques des documents avec métriques de selfie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatisticsDTO {
    // Statistiques existantes
    private long totalDocuments;
    private long pendingDocuments;
    private long underReviewDocuments;
    private long approvedDocuments;
    private long rejectedDocuments;
    private long processedToday;
    private double approvalRate;
    
    // ← NEW: Statistiques spécifiques au selfie
    private long documentsWithSelfie;
    private long documentsWithoutSelfie;
    private double selfieComplianceRate;        // % documents avec selfie
    private double averageFacialSimilarity;     // Score moyen de similarité
    private long highSimilarityDocuments;      // Similarité > 80%
    private long lowSimilarityDocuments;       // Similarité < 50%
    private long livenessDetectedDocuments;    // Détection de vie positive
    
    private LocalDateTime generatedAt;
}