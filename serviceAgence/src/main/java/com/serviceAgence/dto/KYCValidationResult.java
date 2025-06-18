package com.serviceAgence.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KYCValidationResult {
    private boolean valid;
    private String errorCode;
    private String reason;

    public static KYCValidationResult accepted(String code, String reason) {
        return new KYCValidationResult(true, code, reason);
    }

    public static KYCValidationResult rejected(String errorCode, String reason) {
        return new KYCValidationResult(false, errorCode, reason);
    }
}
