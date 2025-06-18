package com.serviceAgence.enums;

public enum CompteType {
    STANDARD("Compte standard"),
    PREMIUM("Compte premium"),
    BUSINESS("Compte entreprise"),
    SAVING("Compte épargne");

    private final String description;

    CompteType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
