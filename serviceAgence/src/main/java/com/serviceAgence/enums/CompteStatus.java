package com.serviceAgence.enums;

public enum CompteStatus {
    PENDING("En attente de validation"),
    ACTIVE("Compte actif"),
    SUSPENDED("Compte suspendu"),
    BLOCKED("Compte bloqué"),
    CLOSED("Compte fermé");

    private final String description;

    CompteStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

