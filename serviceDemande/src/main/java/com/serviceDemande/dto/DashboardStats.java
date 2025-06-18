package com.serviceDemande.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStats {
    // Statistiques générales
    private long totalDemandes;
    private long demandesEnAttente;
    private long demandesEnAnalyse;
    private long demandesApprouvees;
    private long demandesRejetees;
    private long revisionManuelleRequise;
    
    // Statistiques par risque
    private long risqueFaible;
    private long risqueMoyen;
    private long risqueEleve;
    private long risqueCritique;
    
    // Métriques temporelles
    private long demandesRecentes; // 24h
    private double tauxApprobation; // 30 jours
    private double scoreRisqueMoyen;
    
    private LocalDateTime generatedAt;
    
    // Méthodes calculées
    public double getPourcentageApprobation() {
        long total = demandesApprouvees + demandesRejetees;
        return total == 0 ? 0.0 : (double) demandesApprouvees / total * 100;
    }
    
    public double getPourcentageRisqueEleve() {
        long totalRisk = risqueFaible + risqueMoyen + risqueEleve + risqueCritique;
        return totalRisk == 0 ? 0.0 : (double) (risqueEleve + risqueCritique) / totalRisk * 100;
    }
}
