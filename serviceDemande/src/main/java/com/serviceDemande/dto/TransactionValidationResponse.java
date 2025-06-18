package com.serviceDemande.dto;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionValidationResponse {
    private String eventId;
    private String idClient;
    private String statut; // APPROVED, REJECTED
    private String errorCode;
    private String message;
    private Integer riskScore;
    private LocalDateTime timestamp;
}

