package com.wallet.bank_card_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@Data
public class OrangeMoneyRechargeRequest {

    @NotBlank(message = "Numéro Orange Money requis")
    @Pattern(regexp = "237\\d{9}", message = "Format numéro Orange Money invalide (237xxxxxxxxx)")
    private String numeroOrangeMoney;

    @NotNull(message = "Montant requis")
    @DecimalMin(value = "50.0", message = "Montant minimum 50 FCFA")
    @DecimalMax(value = "500000.0", message = "Montant maximum 500k FCFA")
    private BigDecimal montant;

    private String description;
}
