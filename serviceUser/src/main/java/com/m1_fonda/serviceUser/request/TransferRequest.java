package com.m1_fonda.serviceUser.request;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
    @NotNull
    @DecimalMin(value = "100", message = "Montant minimum 100 FCFA")
    @DecimalMax(value = "5000000", message = "Montant maximum 5,000,000 FCFA")
    private BigDecimal montant;
    
    @NotNull
    private Long numeroCompteSend;
    
    @NotNull
    private Long numeroCompteReceive;
}