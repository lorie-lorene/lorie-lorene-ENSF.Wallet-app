package com.wallet.money.carteclient;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardRechargeRequest {
    private String idCarte;
    private BigDecimal montant;
    private String numeroOrangeMoney;
    private String callbackUrl;
    private String clientId;
}
