package com.m1_fonda.serviceUser.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private String transactionId;
    private String status;
    private String message;
    private BigDecimal montant;
    private LocalDateTime timestamp;
}
