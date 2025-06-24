package com.serviceAgence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les demandes d'enregistrement utilisateur avec selfie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    private String idClient;
    private String idAgence;
    private String cni;
    private String email;
    private String nom;
    private String prenom;
    private String numero;
    
    // Documents KYC (données binaires décodées)
    private byte[] rectoCni;
    private byte[] versoCni;
    private byte[] selfieImage;  // Selfie utilisateur
    
    /**
     * Vérifier si le selfie est présent
     */
    public boolean hasSelfie() {
        return selfieImage != null && selfieImage.length > 0;
    }
    
    /**
     * Obtenir la taille du selfie en bytes
     */
    public long getSelfieSize() {
        return selfieImage != null ? selfieImage.length : 0;
    }
    
    /**
     * Vérifier si tous les documents requis sont présents
     */
    public boolean hasAllDocuments() {
        return rectoCni != null && rectoCni.length > 0 &&
               versoCni != null && versoCni.length > 0 &&
               hasSelfie();
    }
}