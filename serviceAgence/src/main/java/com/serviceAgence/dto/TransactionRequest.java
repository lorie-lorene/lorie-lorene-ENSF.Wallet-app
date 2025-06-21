package com.serviceAgence.dto;

import java.math.BigDecimal;
import com.serviceAgence.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.01", message = "Montant doit être positif")
    private BigDecimal montant;

    @NotBlank
    private String compteSource;

    // Pour transferts compte-à-compte
    private String compteDestination;

    // NOUVEAU : Pour transactions vers carte
    private String numeroCarteDestination;

    @NotBlank
    private String idClient;

    @NotBlank
    private String idAgence;

    private String description;
    private String referenceExterne;

    /**
     * Détermine si la transaction nécessite une destination
     */
    public boolean requiresDestination() {
        return type.requiresDestination();
    }

    /**
     * Détermine si c'est une transaction vers carte
     */
    public boolean isCardTransaction() {
        return numeroCarteDestination != null && !numeroCarteDestination.trim().isEmpty();
    }

    /**
     * Détermine si c'est un transfert entre comptes
     */
    public boolean isAccountTransfer() {
        return compteDestination != null && !compteDestination.trim().isEmpty();
    }

    /**
     * Récupère la destination (compte ou carte)
     */
    public String getDestination() {
        if (isCardTransaction()) {
            return "CARTE:" + numeroCarteDestination;
        } else if (isAccountTransfer()) {
            return "COMPTE:" + compteDestination;
        }
        return null;
    }
}