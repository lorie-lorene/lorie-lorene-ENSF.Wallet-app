package com.serviceAgence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat de validation KYC avec informations détaillées
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCValidationResult {
    private boolean valid;
    private String errorCode;
    private String reason;
    private Integer qualityScore;  // Score de qualité (0-100)
    private List<String> anomalies; // Anomalies détectées
    private List<String> documentsValidated; // Documents validés avec succès
    
    /**
     * Résultat de validation acceptée
     */
    public static KYCValidationResult accepted(String code, String message) {
        return KYCValidationResult.builder()
                .valid(true)
                .errorCode(code)
                .reason(message)
                .qualityScore(85) // Score par défaut
                .anomalies(new ArrayList<>())
                .documentsValidated(List.of("CNI_RECTO", "CNI_VERSO"))
                .build();
    }
    
    /**
     * Résultat de validation rejetée
     */
    public static KYCValidationResult rejected(String code, String message) {
        return KYCValidationResult.builder()
                .valid(false)
                .errorCode(code)
                .reason(message)
                .qualityScore(0)
                .anomalies(new ArrayList<>())
                .documentsValidated(new ArrayList<>())
                .build();
    }
    
    /**
     * Ajouter une anomalie
     */
    public void addAnomaly(String anomaly) {
        if (this.anomalies == null) {
            this.anomalies = new ArrayList<>();
        }
        this.anomalies.add(anomaly);
    }
    
    /**
     * Vérifier si des anomalies existent
     */
    public boolean hasAnomalies() {
        return anomalies != null && !anomalies.isEmpty();
    }
}
