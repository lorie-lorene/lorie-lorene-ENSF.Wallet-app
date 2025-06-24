package com.serviceAgence.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event de réception des demandes d'enregistrement avec selfie
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationEventReceived {
    private String eventId;
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
    
    private String sourceService;
    private LocalDateTime timestamp;
}