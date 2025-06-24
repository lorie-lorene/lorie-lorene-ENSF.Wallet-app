package com.wallet.bank_card_service.dto;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@Data
public class CarteWithdrawalRequest {

    @NotBlank(message = "Numéro de téléphone requis")
    @Pattern(regexp = "(237\\d{9}|\\+237\\d{9})", message = "Format numéro invalide (237xxxxxxxxx)")
    private String numeroTelephone;

    @NotNull(message = "Montant requis")
    @DecimalMin(value = "100.0", message = "Montant minimum 100 FCFA")
    @DecimalMax(value = "200000.0", message = "Montant maximum 200k FCFA par retrait")
    private BigDecimal montant;

    @NotBlank(message = "Provider requis")
    @Pattern(regexp = "(ORANGE|MTN)", message = "Provider doit être ORANGE ou MTN")
    private String provider; // ORANGE ou MTN

    private String description;

    @NotNull(message = "Code PIN requis")
    @Min(value = 1000, message = "Code PIN doit être à 4 chiffres")
    @Max(value = 9999, message = "Code PIN doit être à 4 chiffres")
    private Integer codePin;
}