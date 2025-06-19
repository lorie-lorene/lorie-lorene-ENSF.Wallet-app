package com.wallet.bank_card_service.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CarteSettingsRequest {
    private BigDecimal limiteDailyPurchase;
    private BigDecimal limiteDailyWithdrawal;
    private BigDecimal limiteMonthly;
    private Boolean contactless;
    private Boolean internationalPayments;
    private Boolean onlinePayments;
}
