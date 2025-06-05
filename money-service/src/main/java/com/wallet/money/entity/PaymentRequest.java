package com.wallet.money.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String payer;
    private double amount;
    private String externalId;
    private String description;
    private String callback;
}

