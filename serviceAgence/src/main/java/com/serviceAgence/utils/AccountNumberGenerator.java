package com.serviceAgence.utils;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AccountNumberGenerator {

    private static final String DATE_PATTERN = "yyyyMMdd";
    
    /**
     * Génération d'un numéro de compte unique
     * Format: AAAAAMMJJXXXXXX (Agence + Date + Hash)
     */
    public Long generateAccountNumber(String idClient, String idAgence) {
        try {
            // Préfixe agence (4 premiers caractères, paddés avec des 0)
            String agencePrefix = String.format("%4s", idAgence.substring(0, Math.min(4, idAgence.length())))
                                       .replace(' ', '0');
            
            // Date du jour (AAAAMMJJ -> MMJJ)
            String dateString = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
            String dateSuffix = dateString.substring(4); // MMJJ
            
            // Hash unique basé sur client + timestamp
            String uniqueData = idClient + System.nanoTime();
            String hash = generateHash(uniqueData);
            
            // 6 derniers caractères du hash
            String hashSuffix = hash.substring(hash.length() - 6);
            
            // Assemblage: AAAA + MMJJ + XXXXXX
            String accountNumberStr = agencePrefix + dateSuffix + hashSuffix;
            
            // Conversion en Long (on prend les 12 premiers caractères pour éviter overflow)
            accountNumberStr = accountNumberStr.substring(0, Math.min(12, accountNumberStr.length()));
            
            // Conversion hexadécimal vers décimal puis modulo pour garder dans les limites
            Long accountNumber = Long.parseLong(accountNumberStr, 16) % 999999999999L;
            
            // S'assurer que le numéro a au moins 8 chiffres
            if (accountNumber < 10000000L) {
                accountNumber += 10000000L;
            }
            
            log.info("Numéro de compte généré: {} pour client: {}, agence: {}", 
                    accountNumber, idClient, idAgence);
            
            return accountNumber;
            
        } catch (Exception e) {
            log.error("Erreur génération numéro compte: {}", e.getMessage(), e);
            // Fallback: génération basique avec timestamp
            return System.currentTimeMillis() % 999999999L + 100000000L;
        }
    }
    
    /**
     * Génération d'un hash SHA-256
     */
    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("Erreur génération hash: {}", e.getMessage(), e);
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    /**
     * Validation du format d'un numéro de compte
     */
    public boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return false;
        }
        
        try {
            Long number = Long.parseLong(accountNumber);
            return number >= 10000000L && number <= 999999999999L;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

