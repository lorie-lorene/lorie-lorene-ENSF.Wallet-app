package com.wallet.money.entity;

import lombok.Data;

@Data
// @Document(collection = "withdrawal_requests")
public class RetraitRequest {

    private String receiver;
    private double amount;
    private String callback;
    private String externalId;
    private String description;
};