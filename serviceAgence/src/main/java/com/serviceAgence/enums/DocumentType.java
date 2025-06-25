package com.serviceAgence.enums;


public enum DocumentType {
    CNI_CAMEROUNAISE("Carte Nationale d'Identité Camerounaise"),
    PASSEPORT("Passeport"),
    PERMIS_CONDUIRE("Permis de conduire"),
    SELFIE_VERIFICATION("Selfie de vérification biométrique"),
    ACTE_NAISSANCE("Acte de naissance");
    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}