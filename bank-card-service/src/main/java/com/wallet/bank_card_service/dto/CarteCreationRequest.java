package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class CarteCreationRequest {

    @NotBlank(message = "ID client requis")
    private String idClient;
    @NotBlank(message = "ID agence requis")
    private String idAgence;

    @NotBlank(message = "Numéro de compte requis")
    private String numeroCompte;

    @NotNull(message = "Type de carte requis")
    private CarteType type;

    @NotBlank(message = "Nom du porteur requis")
    @Size(min = 2, max = 50, message = "Nom entre 2 et 50 caractères")
    private String nomPorteur;

    @Min(value = 1000, message = "Code PIN doit être à 4 chiffres")
    @Max(value = 9999, message = "Code PIN doit être à 4 chiffres")
    private int codePin;

    // Paramètres optionnels
    private BigDecimal limiteDailyPurchase;
    private BigDecimal limiteDailyWithdrawal;
    private BigDecimal limiteMonthly;

    private boolean contactless = true;
    private boolean internationalPayments = false;
    private boolean onlinePayments = true;
}
