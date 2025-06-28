package com.serviceAgence.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Événement interne d'activation de compte
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountActivatedEvent {
    private String eventId;
    private String idClient;
    private String numeroCompte;
    private String idAgence;
    private LocalDateTime activatedAt;
    private String activationTrigger; // "KYC_DOCUMENT_APPROVAL", "MANUAL", etc.
    private String documentId;
    private LocalDateTime timestamp;
}