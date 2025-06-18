package com.serviceAgence.utils;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class CurrencyUtils {

    private static final String CURRENCY_CODE = "FCFA";
    /**
     * Formatage d'un montant en FCFA
     */
    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0 " + CURRENCY_CODE;
        }
        
        // Arrondir à l'unité (pas de centimes en FCFA)
        BigDecimal roundedAmount = amount.setScale(0, RoundingMode.HALF_UP);
        
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(roundedAmount) + " " + CURRENCY_CODE;
    }

    /**
     * Conversion string vers BigDecimal
     */
    public BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Nettoyer la string (enlever espaces, FCFA, etc.)
            String cleanAmount = amountStr.trim()
                                         .replace(CURRENCY_CODE, "")
                                         .replace(",", "")
                                         .replace(" ", "")
                                         .trim();
            
            return new BigDecimal(cleanAmount).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Format de montant invalide: " + amountStr);
        }
    }

    /**
     * Addition sécurisée
     */
    public BigDecimal add(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        return amount1.add(amount2);
    }

    /**
     * Soustraction sécurisée
     */
    public BigDecimal subtract(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null) amount1 = BigDecimal.ZERO;
        if (amount2 == null) amount2 = BigDecimal.ZERO;
        
        return amount1.subtract(amount2);
    }

    /**
     * Calcul de pourcentage
     */
    public BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percentage) {
        if (amount == null || percentage == null) {
            return BigDecimal.ZERO;
        }
        
        return amount.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Comparaison de montants avec tolérance
     */
    public boolean isEqual(BigDecimal amount1, BigDecimal amount2, BigDecimal tolerance) {
        if (amount1 == null || amount2 == null) {
            return amount1 == amount2;
        }
        
        BigDecimal diff = amount1.subtract(amount2).abs();
        return diff.compareTo(tolerance) <= 0;
    }

    /**
     * Arrondi à l'unité FCFA
     */
    public BigDecimal roundToFCFA(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}

