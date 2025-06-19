package com.wallet.bank_card_service.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CarteOperationResult {
    private boolean success;
    private String operationType;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    
    public static CarteOperationResult success(String operationType, String message) {
        CarteOperationResult result = new CarteOperationResult();
        result.success = true;
        result.operationType = operationType;
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
    
    public static CarteOperationResult failed(String operationType, String errorCode, String message) {
        CarteOperationResult result = new CarteOperationResult();
        result.success = false;
        result.operationType = operationType;
        result.errorCode = errorCode;
        result.message = message;
        result.timestamp = LocalDateTime.now();
        return result;
    }
}