package com.serviceAgence.utils;


import java.math.BigDecimal;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ValidationUtils {

    private static final Pattern CNI_PATTERN = Pattern.compile("\\d{8,12}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^6[5-9]\\d{7}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Validation CNI camerounaise
     */
    public boolean isValidCameroonianCNI(String cni) {
        if (cni == null || cni.trim().isEmpty()) {
            return false;
        }
        String cleanCni = cni.trim().replaceAll("\\s+", "");
        return CNI_PATTERN.matcher(cleanCni).matches() && 
               cleanCni.length() >= 8 && cleanCni.length() <= 12;
    }

    /**
     * Validation numéro téléphone camerounais
     */
    public boolean isValidCameroonianPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validation email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validation montant
     */
    public boolean isValidAmount(BigDecimal montant) {
        return montant != null && montant.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validation montant dans une fourchette
     */
    public boolean isAmountInRange(BigDecimal montant, BigDecimal min, BigDecimal max) {
        if (!isValidAmount(montant)) {
            return false;
        }
        return montant.compareTo(min) >= 0 && montant.compareTo(max) <= 0;
    }

    /**
     * Validation format fichier image
     */
    public boolean isValidImageFormat(byte[] imageData) {
        if (imageData == null || imageData.length < 1000) {
            return false;
        }
        
        // Vérification headers JPEG/PNG
        boolean isJPEG = imageData.length >= 2 && 
                        imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8;
        boolean isPNG = imageData.length >= 4 && 
                       imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && 
                       imageData[2] == 0x4E && imageData[3] == 0x47;
        
        return isJPEG || isPNG;
    }

    /**
     * Validation taille fichier
     */
    public boolean isValidFileSize(byte[] fileData, long maxSizeBytes) {
        return fileData != null && fileData.length > 0 && fileData.length <= maxSizeBytes;
    }
}
