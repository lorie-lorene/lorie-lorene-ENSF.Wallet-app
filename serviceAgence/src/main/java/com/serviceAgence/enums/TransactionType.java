package com.serviceAgence.enums;

public enum TransactionType {
    DEPOT_PHYSIQUE("Dépôt en espèces à l'agence"),
    RETRAIT_PHYSIQUE("Retrait en espèces à l'agence"),
    RETRAIT_CARTE("Retrait vers Mobile Money"),
    TRANSFERT_INTERNE("Transfert entre comptes internes"),
    TRANSFERT_EXTERNE("Transfert vers banque externe"),
    FRAIS_TENUE_COMPTE("Frais de tenue de compte"),
    TRANSFERT_VERS_CARTE("Transfert vers carte"),
    PAIEMENT_CARTE("Paiement par carte"),
    CORRECTION("Correction de transaction");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresDestination() {
        return this == TRANSFERT_INTERNE || this == TRANSFERT_EXTERNE;
    }

    public boolean isWithdrawal() {
        return this == RETRAIT_PHYSIQUE;
    }

    public boolean isTransfer() {
        return this == TRANSFERT_INTERNE || this == TRANSFERT_EXTERNE;
    }

    public boolean isCardTransaction() {
        return this == TRANSFERT_VERS_CARTE ||
                this == PAIEMENT_CARTE;
    }

    /**
     * NOUVEAU : Détermine si c'est un transfert entre comptes
     */
    public boolean isAccountTransfer() {
        return this == TRANSFERT_INTERNE || this == TRANSFERT_EXTERNE;
    }
}
