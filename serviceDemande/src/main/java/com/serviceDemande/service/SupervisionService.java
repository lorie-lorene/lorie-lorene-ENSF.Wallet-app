package com.serviceDemande.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceDemande.dto.DashboardStats;
import com.serviceDemande.dto.TransactionLimits;
import com.serviceDemande.enums.DemandeStatus;
import com.serviceDemande.enums.RiskLevel;
import com.serviceDemande.model.Demande;
import com.serviceDemande.repository.DemandeRepository;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class SupervisionService {

    @Autowired
    private DemandeRepository demandeRepository;

    /**
     * Génération des statistiques pour le dashboard
     */
    public DashboardStats getDashboardStats() {
        log.info("Génération statistiques dashboard");
        
        DashboardStats stats = new DashboardStats();
        
        // Statistiques générales
        stats.setTotalDemandes(demandeRepository.count());
        stats.setDemandesEnAttente(demandeRepository.countByStatus(DemandeStatus.RECEIVED));
        stats.setDemandesEnAnalyse(demandeRepository.countByStatus(DemandeStatus.ANALYZING));
        stats.setDemandesApprouvees(demandeRepository.countByStatus(DemandeStatus.APPROVED));
        stats.setDemandesRejetees(demandeRepository.countByStatus(DemandeStatus.REJECTED));
        stats.setRevisionManuelleRequise(demandeRepository.countByStatus(DemandeStatus.MANUAL_REVIEW));
        
        // Statistiques par niveau de risque
        stats.setRisqueFaible(demandeRepository.countByRiskLevel(RiskLevel.LOW));
        stats.setRisqueMoyen(demandeRepository.countByRiskLevel(RiskLevel.MEDIUM));
        stats.setRisqueEleve(demandeRepository.countByRiskLevel(RiskLevel.HIGH));
        stats.setRisqueCritique(demandeRepository.countByRiskLevel(RiskLevel.CRITICAL));
        
        // Demandes récentes (dernières 24h)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        stats.setDemandesRecentes(demandeRepository.countByCreatedAtAfter(yesterday));
        
        // Taux d'approbation (derniers 30 jours)
        LocalDateTime lastMonth = LocalDateTime.now().minusDays(30);
        List<Demande> recentDemandes = demandeRepository.findByCreatedAtAfter(lastMonth);
        
        long recentApproved = recentDemandes.stream()
                .mapToLong(d -> d.getStatus() == DemandeStatus.APPROVED ? 1 : 0)
                .sum();
        
        double approvalRate = recentDemandes.isEmpty() ? 0.0 : 
                (double) recentApproved / recentDemandes.size() * 100;
        stats.setTauxApprobation(approvalRate);
        
        // Score de risque moyen
        double avgRiskScore = recentDemandes.stream()
                .mapToInt(Demande::getRiskScore)
                .average()
                .orElse(0.0);
        stats.setScoreRisqueMoyen(avgRiskScore);
        
        stats.setGeneratedAt(LocalDateTime.now());
        
        log.info("Statistiques générées: {} demandes total, {}% taux approbation", 
                stats.getTotalDemandes(), String.format("%.1f", approvalRate));
        
        return stats;
    }

    /**
     * Récupération des demandes en attente de révision manuelle
     */
    public List<Demande> getPendingManualReviews() {
        return demandeRepository.findPendingManualReviews();
    }

    /**
     * Recherche avancée de demandes
     */
    public Page<Demande> searchDemandes(DemandeStatus status, RiskLevel riskLevel, 
                                       String idAgence, String idClient, PageRequest pageRequest) {
        
        if (status != null) {
            return demandeRepository.findByStatus(status, pageRequest);
        } else if (riskLevel != null) {
            return demandeRepository.findByRiskLevel(riskLevel, pageRequest);
        } else if (idAgence != null) {
            return demandeRepository.findByIdAgence(idAgence, pageRequest);
        } else if (idClient != null) {
            return demandeRepository.findByIdClient(idClient, pageRequest);
        } else {
            return demandeRepository.findAll(pageRequest);
        }
    }

    /**
     * Détails complets d'une demande
     */
    public Demande getDemandeDetails(String demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande " + demandeId + " introuvable"));
    }

    /**
     * Mise à jour des limites d'un client
     */
    public void updateClientLimits(String demandeId, TransactionLimits newLimits) {
        Demande demande = getDemandeDetails(demandeId);
        
        demande.setLimiteDailyWithdrawal(newLimits.getDailyWithdrawal());
        demande.setLimiteDailyTransfer(newLimits.getDailyTransfer());
        demande.setLimiteMonthlyOperations(newLimits.getMonthlyOperations());
        
        demande.addAction(com.serviceDemande.enums.ActionType.LIMITS_UPDATED,
                String.format("Limites mises à jour: %s/%s/%s", 
                        newLimits.getDailyWithdrawal(),
                        newLimits.getDailyTransfer(),
                        newLimits.getMonthlyOperations()),
                "ADMIN_MANUAL");
        
        demandeRepository.save(demande);
        
        log.info("Limites mises à jour pour client: {}", demande.getIdClient());
    }

    /**
     * Comptes à risque nécessitant surveillance
     */
    public List<Demande> getHighRiskAccounts(int minRiskScore) {
        return demandeRepository.findByRiskScoreGreaterThanEqual(minRiskScore);
    }

    /**
     * Nettoyage des demandes expirées
     */
    @Transactional
    public void cleanupExpiredDemandes() {
        List<Demande> expired = demandeRepository.findExpiredPendingDemandes(LocalDateTime.now());
        
        for (Demande demande : expired) {
            demande.updateStatus(DemandeStatus.EXPIRED, "Demande expirée automatiquement", "SYSTEM");
            demandeRepository.save(demande);
        }
        
        log.info("Nettoyage terminé: {} demandes expirées", expired.size());
    }
}
