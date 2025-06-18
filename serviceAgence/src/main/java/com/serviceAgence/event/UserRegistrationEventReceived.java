package com.serviceAgence.event;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String rectoCni; // Base64 encoded
    private String versoCni; // Base64 encoded
    private String sourceService;
    private LocalDateTime timestamp;
}
