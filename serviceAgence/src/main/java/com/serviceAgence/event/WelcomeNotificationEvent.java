package com.serviceAgence.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement de notification de bienvenue après création de compte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeNotificationEvent {
    private String eventId;
    private String clientId;
    private Long numeroCompte;
    private String agenceCode;
    private LocalDateTime documentValidatedAt;
    private LocalDateTime timestamp;
}