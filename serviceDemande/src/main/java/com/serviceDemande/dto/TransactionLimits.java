package com.serviceDemande.dto;
import java.math.BigDecimal;

import com.serviceDemande.enums.RiskLevel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionLimits {
    private BigDecimal dailyWithdrawal;
    private BigDecimal dailyTransfer;
    private BigDecimal monthlyOperations;
    
    public static TransactionLimits getDefault() {
        return new TransactionLimits(
            new BigDecimal("1000000"),  // 1M FCFA
            new BigDecimal("2000000"),  // 2M FCFA
            new BigDecimal("10000000")  // 10M FCFA
        );
    }
    
    public TransactionLimits applyRiskMultiplier(double multiplier) {
        return new TransactionLimits(
            dailyWithdrawal.multiply(BigDecimal.valueOf(multiplier)),
            dailyTransfer.multiply(BigDecimal.valueOf(multiplier)),
            monthlyOperations.multiply(BigDecimal.valueOf(multiplier))
        );
    }
}