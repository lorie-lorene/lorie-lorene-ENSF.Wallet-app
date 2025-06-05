package com.m1_fonda.serviceUser.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString

/* class de configuration d'envoie pour une demande vers une agence */

public class  UserRegistrationEvent{
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
    private String rectoCni;
    private String versoCni;
    
    private String sourceService = "UserService";
    private String targetService = "AgenceService";
}
