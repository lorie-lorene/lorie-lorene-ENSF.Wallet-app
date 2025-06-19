package com.wallet.bank_card_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PinChangeRequest {
    @Min(value = 1000, message = "Code PIN actuel doit être à 4 chiffres")
    @Max(value = 9999, message = "Code PIN actuel doit être à 4 chiffres")
    private int currentPin;
    
    @Min(value = 1000, message = "Nouveau code PIN doit être à 4 chiffres")
    @Max(value = 9999, message = "Nouveau code PIN doit être à 4 chiffres")
    private int newPin;
}
