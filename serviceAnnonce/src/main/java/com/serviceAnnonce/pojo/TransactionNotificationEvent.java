package com.serviceAnnonce.pojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionNotificationEvent {
    private String eventId;
    private String transactionId;
    private String idClient;
    private String email;
    private TransactionType type;
    private BigDecimal montant;
    private BigDecimal frais;
    private String compteSource;
    private String compteDestination;
    private TransactionStatus status;
    private LocalDateTime timestamp;
}