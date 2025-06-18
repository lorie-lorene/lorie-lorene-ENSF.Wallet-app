package com.serviceDemande.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public
class ValidationResult {
    private boolean valid;
    private String errorCode;
    private String message;
    
    public static ValidationResult valid() {
        return new ValidationResult(true, null, null);
    }
    
    public static ValidationResult invalid(String errorCode, String message) {
        return new ValidationResult(false, errorCode, message);
    }
}
