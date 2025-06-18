package com.serviceDemande.service;


import org.springframework.stereotype.Service;
import com.serviceDemande.dto.TransactionLimits;
import com.serviceDemande.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Service
@Slf4j
public class LimitesService {

    public TransactionLimits calculateLimits(int riskScore, RiskLevel riskLevel, String idAgence) {
        log.info("Calcul limites - Score: {}, Niveau: {}, Agence: {}", riskScore, riskLevel, idAgence);
        
        // Limites de base
        TransactionLimits baseLimits = TransactionLimits.getDefault();
        
        // Multiplicateur selon le risque
        double riskMultiplier = getRiskMultiplier(riskLevel);
        
        // Ajustement selon l'agence (certaines agences peuvent avoir des limites différentes)
        double agenceMultiplier = getAgenceMultiplier(idAgence);
        
        // Application des multiplicateurs
        double finalMultiplier = riskMultiplier * agenceMultiplier;
        TransactionLimits finalLimits = baseLimits.applyRiskMultiplier(finalMultiplier);
        
        // Limites minimales absolues (sécurité)
        finalLimits = enforceMinimumLimits(finalLimits);
        
        log.info("Limites calculées - Daily Withdrawal: {}, Daily Transfer: {}, Monthly: {}", 
                finalLimits.getDailyWithdrawal(), 
                finalLimits.getDailyTransfer(),
                finalLimits.getMonthlyOperations());
        
        return finalLimits;
    }
    
    private double getRiskMultiplier(RiskLevel riskLevel) {
        switch (riskLevel) {
            case LOW:
                return 1.0; // Limites normales
            case MEDIUM:
                return 0.7; // Réduction 30%
            case HIGH:
                return 0.4; // Réduction 60%
            case CRITICAL:
                return 0.2; // Réduction 80%
            default:
                return 0.5; // Sécurité
        }
    }
    
    private double getAgenceMultiplier(String idAgence) {
        // Logique spécifique par agence
        // Certaines agences peuvent avoir des politiques différentes
        if (idAgence != null && idAgence.startsWith("AG_PREMIUM_")) {
            return 1.2; // Agences premium +20%
        } else if (idAgence != null && idAgence.startsWith("AG_RURAL_")) {
            return 0.8; // Agences rurales -20%
        }
        return 1.0; // Standard
    }
    
    private TransactionLimits enforceMinimumLimits(TransactionLimits limits) {
        // Limites minimales absolues (même pour les profils très risqués)
        BigDecimal minDailyWithdrawal = new BigDecimal("50000");   // 50k FCFA minimum
        BigDecimal minDailyTransfer = new BigDecimal("100000");    // 100k FCFA minimum
        BigDecimal minMonthlyOps = new BigDecimal("500000");       // 500k FCFA minimum
        
        return new TransactionLimits(
            limits.getDailyWithdrawal().max(minDailyWithdrawal),
            limits.getDailyTransfer().max(minDailyTransfer),
            limits.getMonthlyOperations().max(minMonthlyOps)
        );
    }
}
