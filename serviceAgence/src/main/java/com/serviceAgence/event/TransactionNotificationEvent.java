package com.serviceAgence.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.serviceAgence.enums.TransactionStatus;
import com.serviceAgence.enums.TransactionType;

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