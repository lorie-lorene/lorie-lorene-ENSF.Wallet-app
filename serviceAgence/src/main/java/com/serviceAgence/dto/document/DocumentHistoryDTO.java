package com.serviceAgence.dto.document;

import com.serviceAgence.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * DTO pour l'historique des documents traités
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentHistoryDTO {
    private String id;
    private String idClient;
    private String idAgence;
    private String nomClient;
    private String prenomClient;
    private String cni;
    private DocumentStatus status;
    private LocalDateTime validatedAt;
    private String validatedBy;
    private String rejectionReason;
    private String validationNotes;
    private Duration processingTime;
}
