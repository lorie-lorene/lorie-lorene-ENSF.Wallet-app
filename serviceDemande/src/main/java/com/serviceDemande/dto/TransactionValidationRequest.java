package com.serviceDemande.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionValidationRequest {
    private String eventId;
    private String idClient;
    private String type;
    private java.math.BigDecimal montant;
    private String compteSource;
    private String compteDestination;
    private java.time.LocalDateTime timestamp;
}
