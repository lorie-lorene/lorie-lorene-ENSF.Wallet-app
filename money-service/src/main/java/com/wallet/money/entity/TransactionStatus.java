package com.wallet.money.entity;

public enum TransactionStatus {
    PENDING,           // En attente validation client
    SUCCESS,           // Client a validé ✅
    FAILED,            // Échec technique 
    CANCELLED,         // Client a annulé ❌
    EXPIRED,           // Timeout dépassé ⏰
    INSUFFICIENT_FUNDS // Solde insuffisant 💸
}
