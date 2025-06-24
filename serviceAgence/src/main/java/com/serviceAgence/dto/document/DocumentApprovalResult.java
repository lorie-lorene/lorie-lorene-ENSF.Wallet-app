package com.serviceAgence.dto.document;

import com.serviceAgence.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour le résultat d'une approbation/rejet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentApprovalResult {
    private String documentId;
    private String clientId;
    private DocumentStatus status;
    private String message;
    
    // Pour approbation
    private String approvedBy;
    private LocalDateTime approvedAt;
    
    // Pour rejet
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}