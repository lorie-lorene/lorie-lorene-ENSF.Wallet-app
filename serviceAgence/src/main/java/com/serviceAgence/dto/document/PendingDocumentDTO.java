package com.serviceAgence.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * DTO pour les documents en attente d'approbation (vue liste)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDocumentDTO {
    private String id;
    private String idClient;
    private String idAgence;
    private String nomClient;
    private String prenomClient;
    private String cni;
    private LocalDateTime uploadedAt;
    private Integer qualityScore;
    private Duration waitingTime;
    private String priority; // URGENT, HIGH, NORMAL, LOW_QUALITY
}