package com.serviceAgence.enums;

public enum AgenceStatus {
    ACTIVE("Agence active"),
    INACTIVE("Agence inactive"),
    SUSPENDED("Agence suspendue"),
    MAINTENANCE("En maintenance");

    private final String description;

    AgenceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

