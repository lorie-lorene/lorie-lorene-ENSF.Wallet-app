package com.serviceAgence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreationResult {
    private boolean success;
    private String errorCode;
    private String message;
    private Long numeroCompte;
    private String idClient;

    public static AccountCreationResult success(Long numeroCompte, String idClient) {
        return new AccountCreationResult(true, null, "Compte créé avec succès", numeroCompte, idClient);
    }

    public static AccountCreationResult failed(String errorCode, String message) {
        return new AccountCreationResult(false, errorCode, message, null, null);
    }
}