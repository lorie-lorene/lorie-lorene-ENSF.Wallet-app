package com.serviceAgence.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponseEvent {
    private String eventId;
    private String idClient;
    private String idAgence;
    private String email;
    private String statut; // ACCEPTE, REFUSE
    private String probleme; // Code d'erreur si refus
    private Long numeroCompte; // Si accepté
    private LocalDateTime timestamp;
    private String targetService = "UserService";
}