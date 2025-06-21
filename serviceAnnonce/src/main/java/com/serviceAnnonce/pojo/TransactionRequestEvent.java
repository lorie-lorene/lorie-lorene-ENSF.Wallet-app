package com.serviceAnnonce.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequestEvent {
    private String eventId;
    private TransactionType type;
    private BigDecimal montant;
    private String numeroClient;
    private String numeroCompte;
    private String numeroCompteDestination; // Pour transferts
    private String sourceService;
    private LocalDateTime timestamp;
}
