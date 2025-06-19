package com.wallet.bank_card_service.model;



import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.wallet.bank_card_service.dto.CarteStatus;
import com.wallet.bank_card_service.dto.CarteType;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Document(collection = "cartes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carte {
    
    @Id
    private String idCarte;
    
    // Informations client
    private String idClient;
    private String numeroCompte; // Compte bancaire associé
    private String idAgence;
    
    // Données carte
    private String numeroCarte; // 16 chiffres
    private String cvv; // 3 chiffres
    private LocalDateTime dateExpiration;
    private String nomPorteur;
    
    // Type et statut
    private CarteType type; // VIRTUELLE_GRATUITE, VIRTUELLE_PREMIUM, PHYSIQUE
    private CarteStatus status; // ACTIVE, BLOCKED, EXPIRED, PENDING_ACTIVATION
    
    // Solde et limites
    private BigDecimal solde = BigDecimal.ZERO;
    private BigDecimal limiteDailyPurchase = new BigDecimal("500000"); // 500k FCFA
    private BigDecimal limiteDailyWithdrawal = new BigDecimal("200000"); // 200k FCFA
    private BigDecimal limiteMonthly = new BigDecimal("2000000"); // 2M FCFA
    
    // Utilisation quotidienne/mensuelle (reset automatique)
    private BigDecimal utilisationQuotidienne = BigDecimal.ZERO;
    private BigDecimal utilisationMensuelle = BigDecimal.ZERO;
    private LocalDateTime lastDailyReset;
    private LocalDateTime lastMonthlyReset;
    
    // Sécurité
    private boolean contactless = true;
    private boolean internationalPayments = false;
    private boolean onlinePayments = true;
    private int codePin; // Encrypté
    private boolean pinBlocked = false;
    private int pinAttempts = 0;
    
    // Audit et traçabilité
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private LocalDateTime lastUsedAt;
    private String createdBy;
    private String blockedBy;
    private String blockedReason;
    private LocalDateTime blockedAt;
    
    // Historique des actions
    private List<CarteAction> actionsHistory = new ArrayList<>();
    
    // Coûts
    private BigDecimal fraisCreation = BigDecimal.ZERO;
    private BigDecimal fraisMensuels = BigDecimal.ZERO;
    private LocalDateTime nextBillingDate;
    private int failedBillingAttempts = 0; // Nombre d'échecs de facturation consécutifs
    
    /**
     * Vérifier si la carte peut effectuer un achat
     */
    public boolean canPurchase(BigDecimal montant) {
        if (status != CarteStatus.ACTIVE) return false;
        if (solde.compareTo(montant) < 0) return false;
        
        resetCountersIfNeeded();
        
        // Vérifier limites quotidiennes
        BigDecimal newDailyUsage = utilisationQuotidienne.add(montant);
        if (newDailyUsage.compareTo(limiteDailyPurchase) > 0) return false;
        
        // Vérifier limites mensuelles
        BigDecimal newMonthlyUsage = utilisationMensuelle.add(montant);
        if (newMonthlyUsage.compareTo(limiteMonthly) > 0) return false;
        
        return true;
    }
    
    /**
     * Effectuer un débit sur la carte
     */
    public void debit(BigDecimal montant) {
        if (!canPurchase(montant)) {
            throw new IllegalStateException("Transaction non autorisée");
        }
        
        resetCountersIfNeeded();
        
        this.solde = this.solde.subtract(montant);
        this.utilisationQuotidienne = this.utilisationQuotidienne.add(montant);
        this.utilisationMensuelle = this.utilisationMensuelle.add(montant);
        this.lastUsedAt = LocalDateTime.now();
        
        addAction(CarteActionType.DEBIT, montant, "Débit carte", "SYSTEM");
    }
    
    /**
     * Créditer la carte (transfert depuis compte)
     */
    public void credit(BigDecimal montant) {
        this.solde = this.solde.add(montant);
        this.lastUsedAt = LocalDateTime.now();
        
        addAction(CarteActionType.CREDIT, montant, "Crédit depuis compte", "SYSTEM");
    }
    
    /**
     * Bloquer la carte
     */
    public void block(String reason, String blockedBy) {
        this.status = CarteStatus.BLOCKED;
        this.blockedReason = reason;
        this.blockedBy = blockedBy;
        this.blockedAt = LocalDateTime.now();
        
        addAction(CarteActionType.BLOCKED, null, reason, blockedBy);
    }
    
    /**
     * Débloquer la carte
     */
    public void unblock(String unlockedBy) {
        this.status = CarteStatus.ACTIVE;
        this.blockedReason = null;
        this.blockedBy = null;
        this.blockedAt = null;
        
        addAction(CarteActionType.UNBLOCKED, null, "Carte débloquée", unlockedBy);
    }
    
    /**
     * Reset des compteurs quotidiens/mensuels
     */
    private void resetCountersIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        
        // Reset quotidien
        if (lastDailyReset == null || !lastDailyReset.toLocalDate().equals(now.toLocalDate())) {
            this.utilisationQuotidienne = BigDecimal.ZERO;
            this.lastDailyReset = now;
        }
        
        // Reset mensuel
        if (lastMonthlyReset == null || 
            lastMonthlyReset.getMonth() != now.getMonth() || 
            lastMonthlyReset.getYear() != now.getYear()) {
            this.utilisationMensuelle = BigDecimal.ZERO;
            this.lastMonthlyReset = now;
        }
    }
    
    /**
     * Ajouter action dans l'historique
     */
    public void addAction(CarteActionType type, BigDecimal montant, String description, String performedBy) {
        CarteAction action = new CarteAction();
        action.setType(type);
        action.setMontant(montant);
        action.setDescription(description);
        action.setPerformedBy(performedBy);
        action.setTimestamp(LocalDateTime.now());
        
        this.actionsHistory.add(action);
        
        // Garder seulement les 100 dernières actions
        if (this.actionsHistory.size() > 100) {
            this.actionsHistory = new ArrayList<>(this.actionsHistory.subList(
                this.actionsHistory.size() - 100, this.actionsHistory.size()));
        }
    }
    
    /**
     * Vérifier si la carte est expirée
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }
    
    /**
     * Vérifier si la carte est active
     */
    public boolean isActive() {
        return status == CarteStatus.ACTIVE && !isExpired() && !pinBlocked;
    }
    
    /**
     * Masquer le numéro de carte (affichage sécurisé)
     */
    public String getMaskedNumber() {
        if (numeroCarte == null || numeroCarte.length() != 16) {
            return "****-****-****-****";
        }
        return numeroCarte.substring(0, 4) + "-****-****-" + numeroCarte.substring(12);
    }
    
    /**
     * Calculer les frais mensuels selon le type
     */
    public BigDecimal calculateMonthlyFees() {
        return switch (type) {
            case VIRTUELLE_GRATUITE -> BigDecimal.ZERO;
            case VIRTUELLE_PREMIUM -> new BigDecimal("1000"); // 1000 FCFA/mois
            case PHYSIQUE -> new BigDecimal("2500"); // 2500 FCFA/mois
        };
    }
    
    /**
     * Mettre à jour les limites personnalisées
     */
    public void updateLimits(BigDecimal dailyPurchase, BigDecimal dailyWithdrawal, BigDecimal monthly) {
        this.limiteDailyPurchase = dailyPurchase;
        this.limiteDailyWithdrawal = dailyWithdrawal;
        this.limiteMonthly = monthly;
        
        addAction(CarteActionType.LIMITS_UPDATED, null, 
                "Limites mises à jour: " + dailyPurchase + "/" + monthly, "USER");
    }
    
    /**
     * Action interne pour l'historique
     */
    @Data
    public static class CarteAction {
        private CarteActionType type;
        private BigDecimal montant;
        private String description;
        private String performedBy;
        private LocalDateTime timestamp;
    }
    
    public enum CarteActionType {
        CREATED, ACTIVATED, BLOCKED, UNBLOCKED,
        CREDIT, DEBIT, LIMITS_UPDATED, PIN_CHANGED,
        SETTINGS_UPDATED, EXPIRED
    }
}