package com.serviceAgence.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat de l'analyse du selfie utilisateur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfieAnalysisResult {
    private int qualityScore;           // Score de qualité (0-100)
    private int similarityScore;        // Score de similarité faciale (0-100)
    private boolean livenessDetected;   // Détection de vie
    private List<String> anomalies = new ArrayList<>();     // Anomalies détectées
    private String facialMatchRecommendation;  // Recommandation de correspondance
    private String overallRecommendation;      // Recommandation globale
}