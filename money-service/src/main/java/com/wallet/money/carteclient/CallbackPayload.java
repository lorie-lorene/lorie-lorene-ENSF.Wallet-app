package com.wallet.money.carteclient;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CallbackPayload {
    private String requestId;
    private String idCarte;
    private String status;
    private BigDecimal montant;
    private String transactionId;
    private LocalDateTime timestamp;
}