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

    private String compteDestination; // Optionnel selon le type

    @NotBlank
    private String idClient;

    @NotBlank
    private String idAgence;

    private String description;
    private String referenceExterne;
}
