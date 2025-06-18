package com.serviceAgence.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationProcessingResult {
    private boolean accepted;
    private String errorCode;
    private String message;
    private Long numeroCompte;

    public static RegistrationProcessingResult accepted(Long numeroCompte, String message) {
        return new RegistrationProcessingResult(true, "COMPTE_CREE", message, numeroCompte);
    }

    public static RegistrationProcessingResult rejected(String errorCode, String message) {
        return new RegistrationProcessingResult(false, errorCode, message, null);
    }
}