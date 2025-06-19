package com.wallet.bank_card_service.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RechargeResult {
    private boolean success;
    private String requestId;
    private BigDecimal montant;
    private String status;
    private String message;
    private LocalDateTime timestamp;
    
    public static RechargeResult success(String requestId, BigDecimal montant, String message) {
        RechargeResult result = new RechargeResult();
        result.success = true;
        result.requestId = requestId;
        result.montant = montant;
        result.status = "PENDING";
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
    
    public static RechargeResult failed(String message) {
        RechargeResult result = new RechargeResult();
        result.success = false;
        result.status = "FAILED";
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
}