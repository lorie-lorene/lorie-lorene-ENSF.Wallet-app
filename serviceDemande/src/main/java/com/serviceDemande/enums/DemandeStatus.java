package com.serviceDemande.enums;


import lombok.Getter;

@Getter
public enum DemandeStatus {
    RECEIVED("Demande reçue"),
    ANALYZING("En cours d'analyse"),
    MANUAL_REVIEW("Révision manuelle requise"),
    APPROVED("Approuvée"),
    REJECTED("Rejetée"),
    EXPIRED("Expirée");

    private final String description;

    DemandeStatus(String description) {
        this.description = description;
    }
}