package com.serviceAgence.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.enums.CompteType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "comptes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteUser {
    @Id
    private String id;

    @Indexed(unique = true)
    @NotNull(message = "Numéro de compte obligatoire")
    private Long numeroCompte;

    @NotBlank(message = "ID client obligatoire")
    @Indexed
    private String idClient;

    @NotBlank(message = "ID agence obligatoire")
    @Indexed
    private String idAgence;

    @NotNull
    @DecimalMin(value = "0.0", message = "Solde ne peut être négatif")
    private BigDecimal solde = BigDecimal.ZERO;

    private CompteStatus status = CompteStatus.PENDING;
    private CompteType type = CompteType.STANDARD;

    // Limites transactionnelles
    private BigDecimal limiteDailyWithdrawal = new BigDecimal("1000000"); // 1M FCFA
    private BigDecimal limiteDailyTransfer = new BigDecimal("2000000"); // 2M FCFA
    private BigDecimal limiteMonthlyOperations = new BigDecimal("10000000"); // 10M FCFA

    // Tracking des opérations
    private BigDecimal totalDailyWithdrawals = BigDecimal.ZERO;
    private BigDecimal totalDailyTransfers = BigDecimal.ZERO;
    private BigDecimal totalMonthlyOperations = BigDecimal.ZERO;
    private LocalDateTime lastResetDaily;
    private LocalDateTime lastResetMonthly;

    // Métadonnées
    private LocalDateTime createdAt;
    private LocalDateTime lastTransactionAt;
    private LocalDateTime activatedAt;
    private String activatedBy;

    // Historique des statuts
    private List<CompteStatusHistory> statusHistory = new ArrayList<>();

    // Statistiques
    private Long totalTransactions = 0L;
    private BigDecimal totalVolume = BigDecimal.ZERO;
    private Integer dailyTransactionCount = 0;

    // Sécurité
    private Boolean blocked = false;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private String blockedBy;

    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = CompteStatus.PENDING;
        }
        if (type == null) {
            type = CompteType.STANDARD;
        }
        resetDailyLimitsIfNeeded();
        resetMonthlyLimitsIfNeeded();
    }

    // Méthodes utilitaires

    public void debit(BigDecimal montant) {
        if (solde.compareTo(montant) < 0) {
            throw new IllegalStateException("Solde insuffisant");
        }
        this.solde = this.solde.subtract(montant);
        updateTransactionStats(montant);
    }

    public void credit(BigDecimal montant) {
        this.solde = this.solde.add(montant);
        updateTransactionStats(montant);
    }

    private void updateTransactionStats(BigDecimal montant) {
        this.lastTransactionAt = LocalDateTime.now();
        this.totalTransactions++;
        this.totalVolume = this.totalVolume.add(montant);
        this.dailyTransactionCount++;
    }

    private void resetDailyLimitsIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (lastResetDaily == null || !lastResetDaily.toLocalDate().equals(now.toLocalDate())) {
            totalDailyWithdrawals = BigDecimal.ZERO;
            totalDailyTransfers = BigDecimal.ZERO;
            dailyTransactionCount = 0;
            lastResetDaily = now;
        }
    }

    private void resetMonthlyLimitsIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (lastResetMonthly == null ||
                lastResetMonthly.getMonth() != now.getMonth() ||
                lastResetMonthly.getYear() != now.getYear()) {
            totalMonthlyOperations = BigDecimal.ZERO;
            lastResetMonthly = now;
        }
    }

    public void addStatusHistory(CompteStatus newStatus, String reason, String changedBy) {
        statusHistory.add(new CompteStatusHistory(status, newStatus, reason, changedBy, LocalDateTime.now()));
        this.status = newStatus;
    }

    public boolean isActive() {
        return status == CompteStatus.ACTIVE &&
                (blocked == null || !blocked);
    }

    // Et assurez-vous que les méthodes canWithdraw et canTransfer existent :
    public boolean canWithdraw(BigDecimal amount) {
        if (limiteDailyWithdrawal == null)
            return true;
        BigDecimal totalWithToday = totalDailyWithdrawals != null ? totalDailyWithdrawals : BigDecimal.ZERO;
        return totalWithToday.add(amount).compareTo(limiteDailyWithdrawal) <= 0;
    }

    public boolean canTransfer(BigDecimal amount) {
        if (limiteDailyTransfer == null)
            return true;
        BigDecimal totalTransferToday = totalDailyTransfers != null ? totalDailyTransfers : BigDecimal.ZERO;
        return totalTransferToday.add(amount).compareTo(limiteDailyTransfer) <= 0;
    }

    @Data
    @AllArgsConstructor
    public static class CompteStatusHistory {
        private CompteStatus oldStatus;
        private CompteStatus newStatus;
        private String reason;
        private String changedBy;
        private LocalDateTime changedAt;
    }
}