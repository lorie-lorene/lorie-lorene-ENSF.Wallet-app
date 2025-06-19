package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data 
public class TransfertCarteRequest {
    
    @NotBlank(message = "Numéro de compte source requis")
    private String numeroCompteSource;
    
    @NotBlank(message = "ID carte destination requis")
    private String idCarteDestination;
    
    @NotNull(message = "Montant requis")
    @DecimalMin(value = "100.0", message = "Montant minimum 100 FCFA")
    @DecimalMax(value = "10000000.0", message = "Montant maximum 10M FCFA")
    private BigDecimal montant;
    
    private String description;
}
