package com.serviceAnnonce.pojo;


public enum TransactionStatus {
    PENDING("En attente"),
    PROCESSING("En cours de traitement"),
    COMPLETED("Terminée avec succès"),
    FAILED("Échec"),
    CANCELLED("Annulée"),
    REFUNDED("Remboursée");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REFUNDED;
    }
}