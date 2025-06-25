package com.m1_fonda.serviceUser.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event d'enregistrement utilisateur avec selfie
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserRegistrationEvent {
    private String eventId = UUID.randomUUID().toString();
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Données utilisateur (SANS PASSWORD)
    private String idClient;
    private String idAgence;
    private String cni;
    private String email;
    private String nom;
    private String prenom;
    private String numero;
    
    // Documents KYC
    private String rectoCni;     // Base64 encoded - CNI recto
    private String versoCni;     // Base64 encoded - CNI verso
    private String selfieImage;  // Base64 encoded - User selfie ← NEW
    
    private String sourceService = "UserService";
    private String targetService = "AgenceService";

    public String getSelfieImage() {
        return selfieImage;
    }

    public void setSelfieImage(String selfieImage) {
        this.selfieImage = selfieImage;
    }
}