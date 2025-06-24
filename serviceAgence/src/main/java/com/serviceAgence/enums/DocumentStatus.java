package com.serviceAgence.enums;

/**
 * Statuts des documents dans le workflow d'approbation
 */
public enum DocumentStatus {
    PENDING("En attente de réception"),
    RECEIVED("Reçu - En attente de validation"),
    UNDER_REVIEW("En cours d'examen par admin"),
    APPROVED("Approuvé par admin"),
    REJECTED("Rejeté par admin"),
    EXPIRED("Expiré");
    
    private final String description;
    
    DocumentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}