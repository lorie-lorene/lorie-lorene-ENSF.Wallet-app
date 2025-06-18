package com.serviceAgence.enums;

public enum DocumentStatus {
    PENDING("En attente de validation"),
    VALIDATED("Validé"),
    REJECTED("Rejeté"),
    EXPIRED("Expiré");

    private final String description;

    DocumentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
