package com.serviceAgence.utils;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Génération d'un salt aléatoire
     */
    public String generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash SHA-256 avec salt
     */
    public String hashWithSalt(String data, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedData = data + salt;
            byte[] hash = digest.digest(saltedData.getBytes(StandardCharsets.UTF_8));
            
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Erreur lors du hashage: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de hashage", e);
        }
    }

    /**
     * Vérification d'un hash
     */
    public boolean verifyHash(String data, String salt, String expectedHash) {
        try {
            String computedHash = hashWithSalt(data, salt);
            return computedHash.equals(expectedHash);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Chiffrement simple (pour demo - utiliser AES en production)
     */
    public String encrypt(String data) {
        // Implémentation basique - à remplacer par AES en production
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Déchiffrement simple
     */
    public String decrypt(String encryptedData) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Erreur lors du déchiffrement: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Génération d'un token aléatoire
     */
    public String generateToken(int length) {
        byte[] tokenBytes = new byte[length];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Validation de la force d'un mot de passe
     */
    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    /**
     * Masquage de données sensibles pour les logs
     */
    public String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        
        int visibleChars = Math.min(2, data.length() / 4);
        String start = data.substring(0, visibleChars);
        String end = data.substring(data.length() - visibleChars);
        
        return start + "****" + end;
    }
}