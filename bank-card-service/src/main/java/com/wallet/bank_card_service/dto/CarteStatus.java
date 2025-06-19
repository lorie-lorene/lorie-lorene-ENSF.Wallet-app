package com.wallet.bank_card_service.dto;


public enum CarteStatus {
    PENDING_ACTIVATION("En attente d'activation"),
    ACTIVE("Active"),
    BLOCKED("Bloquée"),
    EXPIRED("Expirée"),
    CANCELLED("Annulée");
    
    private final String description;
    
    CarteStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
