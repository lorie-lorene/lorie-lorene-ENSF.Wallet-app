package com.serviceDemande.dto;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
 public class TransactionRiskResult {
    private int riskScore;
    private boolean suspicious;
    private boolean blocked;
    private String blockReason;
    private String message;
}