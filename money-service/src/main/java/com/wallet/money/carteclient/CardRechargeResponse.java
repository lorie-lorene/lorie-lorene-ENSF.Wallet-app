package com.wallet.money.carteclient;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CardRechargeResponse {
    private String requestId;
    private String idCarte;
    private BigDecimal montant;
    private String status;
    private String message;
    private String freemoReference;
    private LocalDateTime timestamp;
}
