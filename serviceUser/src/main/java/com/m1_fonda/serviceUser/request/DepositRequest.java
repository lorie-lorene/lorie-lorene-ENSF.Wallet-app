package com.m1_fonda.serviceUser.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepositRequest {
    @NotNull
    @DecimalMin(value = "100", message = "Montant minimum 100 FCFA")
    @DecimalMax(value = "10000000", message = "Montant maximum 10,000,000 FCFA")
    private BigDecimal montant;
    
    @NotBlank
    private String numeroClient;
    
    @NotNull
    private Long numeroCompte;
}
