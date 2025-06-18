package com.serviceDemande.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.serviceDemande.model.Demande;
import com.serviceDemande.dto.FraudAnalysisResult;
import com.serviceDemande.enums.RiskLevel;
import com.serviceDemande.repository.DemandeRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AntiFraudeService {

    @Autowired
    private DemandeRepository demandeRepository;
    
    // Patterns de détection
    private static final Pattern SUSPICIOUS_EMAIL_PATTERN = 
        Pattern.compile(".*\\b(temp|test|fake|spam|trash)\\b.*", Pattern.CASE_INSENSITIVE);
    
    private static final List<String> HIGH_RISK_DOMAINS = List.of(
        "10minutemail.com", "guerrillamail.com", "tempmail.org"
    );
    
    private static final List<String> HIGH_RISK_CNI_PREFIXES = List.of(
        "000000", "111111", "123456", "999999"
    );

    public FraudAnalysisResult analyzeRequest(Demande demande) {
        log.info("Début analyse anti-fraude pour: {}", demande.getIdClient());
        
        int riskScore = 0;
        List<String> fraudFlags = new ArrayList<>();
        
        // 1. Analyse CNI
        riskScore += analyzeCNI(demande.getCni(), fraudFlags);
        
        // 2. Analyse Email
        riskScore += analyzeEmail(demande.getEmail(), fraudFlags);
        
        // 3. Analyse Historique
        riskScore += analyzeHistory(demande, fraudFlags);
        
        // 4. Analyse Géographique
        riskScore += analyzeGeography(demande.getIdAgence(), fraudFlags);
        
        // 5. Analyse Patterns Temporels
        riskScore += analyzeTemporalPatterns(demande, fraudFlags);
        
        // 6. Analyse Documents
        riskScore += analyzeDocuments(demande, fraudFlags);
        
        // Calculer le niveau de risque final
        riskScore = Math.min(100, riskScore); // Plafonner à 100
        RiskLevel riskLevel = RiskLevel.fromScore(riskScore);
        
        boolean requiresManualReview = riskScore > 50 || 
                                     fraudFlags.contains("CNI_DEJA_UTILISEE") ||
                                     fraudFlags.contains("MULTIPLE_DEMANDES_RECENTES");
        
        String recommendation = generateRecommendation(riskScore, fraudFlags);
        
        log.info("Analyse terminée - Score: {}, Niveau: {}, Flags: {}", 
                riskScore, riskLevel, fraudFlags);
        
        return new FraudAnalysisResult(
            riskScore, riskLevel, fraudFlags, requiresManualReview, 
            recommendation, LocalDateTime.now()
        );
    }
    
    private int analyzeCNI(String cni, List<String> flags) {
        int score = 0;
        
        if (cni == null || cni.trim().isEmpty()) {
            flags.add("CNI_MANQUANTE");
            return 100; // Rejet automatique
        }
        
        // Format invalide
        if (!isValidCameroonianCNI(cni)) {
            flags.add("FORMAT_CNI_INCORRECT");
            score += 60;
        }
        
        // CNI suspecte (patterns connus)
        for (String suspiciousPrefix : HIGH_RISK_CNI_PREFIXES) {
            if (cni.startsWith(suspiciousPrefix)) {
                flags.add("CNI_PATTERN_SUSPECT");
                score += 40;
                break;
            }
        }
        
        // CNI déjà utilisée
        if (demandeRepository.existsByCniAndStatusNot(cni, 
                com.serviceDemande.enums.DemandeStatus.REJECTED)) {
            flags.add("CNI_DEJA_UTILISEE");
            score += 80; // Très grave
        }
        
        return score;
    }
    
    private int analyzeEmail(String email, List<String> flags) {
        int score = 0;
        
        if (email == null || !isValidEmail(email)) {
            flags.add("EMAIL_INVALIDE");
            score += 30;
        } else {
            // Email temporaire/suspect
            if (SUSPICIOUS_EMAIL_PATTERN.matcher(email).matches()) {
                flags.add("EMAIL_SUSPECT");
                score += 25;
            }
            
            // Domaine à risque
            String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
            if (HIGH_RISK_DOMAINS.contains(domain)) {
                flags.add("DOMAINE_EMAIL_RISQUE");
                score += 35;
            }
            
            // Email déjà utilisé récemment
            long recentEmailCount = demandeRepository.countByEmailAndCreatedAtAfter(
                email, LocalDateTime.now().minusDays(30)
            );
            if (recentEmailCount > 1) {
                flags.add("EMAIL_MULTIPLE_DEMANDES");
                score += 20;
            }
        }
        
        return score;
    }
    
    private int analyzeHistory(Demande demande, List<String> flags) {
        int score = 0;
        
        // Demandes multiples récentes du même client/email
        long recentDemandesCount = demandeRepository.countByEmailAndCreatedAtAfter(
            demande.getEmail(), LocalDateTime.now().minusHours(24)
        );
        
        if (recentDemandesCount > 3) {
            flags.add("MULTIPLE_DEMANDES_RECENTES");
            score += 50;
        } else if (recentDemandesCount > 1) {
            flags.add("DEMANDES_FREQUENTES");
            score += 20;
        }
        
        return score;
    }
    
    private int analyzeGeography(String idAgence, List<String> flags) {
        // Analyse basée sur l'agence (certaines zones plus risquées)
        // À personnaliser selon la géographie camerounaise
        List<String> highRiskAgencies = List.of("AG_BORDER_001", "AG_REMOTE_002");
        
        if (highRiskAgencies.contains(idAgence)) {
            flags.add("ZONE_GEOGRAPHIQUE_RISQUE");
            return 15;
        }
        
        return 0;
    }
    
    private int analyzeTemporalPatterns(Demande demande, List<String> flags) {
        int score = 0;
        LocalDateTime now = LocalDateTime.now();
        
        // Demandes en dehors des heures ouvrables (risque plus élevé)
        if (now.getHour() < 8 || now.getHour() > 18) {
            flags.add("DEMANDE_HORS_HEURES_OUVRABLES");
            score += 10;
        }
        
        // Weekend (risque légèrement plus élevé)
        if (now.getDayOfWeek().getValue() > 5) {
            flags.add("DEMANDE_WEEKEND");
            score += 5;
        }
        
        return score;
    }
    
    private int analyzeDocuments(Demande demande, List<String> flags) {
        int score = 0;
        
        // Documents manquants
        if (demande.getRectoCniHash() == null || demande.getVersoCniHash() == null) {
            flags.add("DOCUMENTS_INCOMPLETS");
            score += 40;
        }
        
        // Qualité des documents (si disponible depuis validation Agence)
        if (demande.getAgenceValidation() != null && 
            demande.getAgenceValidation().getQualityScore() != null) {
            int qualityScore = demande.getAgenceValidation().getQualityScore();
            if (qualityScore < 50) {
                flags.add("QUALITE_DOCUMENTS_FAIBLE");
                score += 30;
            } else if (qualityScore < 70) {
                flags.add("QUALITE_DOCUMENTS_MOYENNE");
                score += 15;
            }
        }
        
        return score;
    }
    
    private String generateRecommendation(int riskScore, List<String> flags) {
        if (riskScore >= 80) {
            return "REJET RECOMMANDÉ - Risque critique détecté";
        } else if (riskScore >= 60) {
            return "RÉVISION MANUELLE OBLIGATOIRE - Plusieurs indicateurs de risque";
        } else if (riskScore >= 40) {
            return "RÉVISION MANUELLE CONSEILLÉE - Risque modéré";
        } else if (riskScore >= 20) {
            return "APPROBATION AVEC LIMITES RÉDUITES - Risque faible";
        } else {
            return "APPROBATION STANDARD - Profil de risque acceptable";
        }
    }
    
    // Validation CNI camerounaise correcte
    private boolean isValidCameroonianCNI(String cni) {
        if (cni == null || cni.trim().isEmpty()) return false;
        String cleanCni = cni.trim().replaceAll("\\s+", "");
        return cleanCni.matches("\\d{8,12}"); // 8-12 chiffres
    }
    
    // Validation email basique
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

