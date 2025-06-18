package com.serviceAgence.event;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponseEvent {
    private String eventId;
    private String transactionId;
    private String statut; // SUCCESS, FAILED
    private String message;
    private BigDecimal montant;
    private BigDecimal frais;
    private String numeroCompte;
    private LocalDateTime timestamp;
    private String targetService = "UserService";
}
