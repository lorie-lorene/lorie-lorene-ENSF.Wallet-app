package com.wallet.money.entity;


import lombok.Data;

@Data
public class CardWithdrawalRequest {
    private String idCarte;
    private String receiver;          // Numéro Orange/MTN
    private double amount;
    private String provider;          // ORANGE ou MTN
    private String description;
    private String callbackUrl;       // URL callback vers service Carte
    private String clientId;
}