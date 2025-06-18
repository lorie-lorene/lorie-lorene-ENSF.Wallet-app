package com.serviceAgence.services;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.model.Agence;
import com.serviceAgence.repository.AgenceRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FraisService {

    @Autowired
    private AgenceRepository agenceRepository;

    // Taux par défaut si non configuré dans l'agence
    private static final Map<TransactionType, BigDecimal> DEFAULT_RATES = Map.of(
        TransactionType.DEPOT_PHYSIQUE, BigDecimal.ZERO, // Gratuit
        TransactionType.RETRAIT_PHYSIQUE, new BigDecimal("1.5"), // 1.5%
        TransactionType.RETRAIT_MOBILE_MONEY, new BigDecimal("2.5"), // 2.5%
        TransactionType.TRANSFERT_INTERNE, new BigDecimal("1.0"), // 1%
        TransactionType.TRANSFERT_EXTERNE, new BigDecimal("3.0"), // 3%
        TransactionType.FRAIS_TENUE_COMPTE, BigDecimal.ZERO // Fixe mensuel
    );

    // Frais minimums (en FCFA)
    private static final Map<TransactionType, BigDecimal> MIN_FEES = Map.of(
        TransactionType.RETRAIT_PHYSIQUE, new BigDecimal("100"),
        TransactionType.RETRAIT_MOBILE_MONEY, new BigDecimal("150"),
        TransactionType.TRANSFERT_INTERNE, new BigDecimal("50"),
        TransactionType.TRANSFERT_EXTERNE, new BigDecimal("500")
    );

    // Paliers dégressifs pour gros montants
    private static final Map<BigDecimal, BigDecimal> DISCOUNT_TIERS = Map.of(
        new BigDecimal("1000000"), new BigDecimal("0.1"), // -10% au-dessus de 1M
        new BigDecimal("5000000"), new BigDecimal("0.2"), // -20% au-dessus de 5M
        new BigDecimal("10000000"), new BigDecimal("0.3") // -30% au-dessus de 10M
    );

    private static final BigDecimal TVA_RATE = new BigDecimal("0.175"); // 17.5%

    /**
     * Calcul principal des frais
     */
    public BigDecimal calculateFrais(TransactionType type, BigDecimal montant, String idAgence) {
        log.debug("Calcul frais: type={}, montant={}, agence={}", type, montant, idAgence);

        try {
            // 1. Récupération du taux spécifique à l'agence
            BigDecimal taux = getTauxForAgence(type, idAgence);

            // 2. Calcul frais de base (pourcentage)
            BigDecimal fraisBase = montant.multiply(taux).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            // 3. Application du frais minimum
            BigDecimal fraisMinimum = MIN_FEES.getOrDefault(type, BigDecimal.ZERO);
            BigDecimal fraisSansDiscount = fraisBase.max(fraisMinimum);

            // 4. Application des paliers dégressifs
            BigDecimal discount = calculateDiscount(montant);
            BigDecimal fraisAvecDiscount = fraisSansDiscount.multiply(BigDecimal.ONE.subtract(discount));

            // 5. Application de la TVA
            BigDecimal fraisAvecTVA = fraisAvecDiscount.multiply(BigDecimal.ONE.add(TVA_RATE));

            // 6. Arrondi à 2 décimales
            BigDecimal fraisFinal = fraisAvecTVA.setScale(0, RoundingMode.HALF_UP);

            log.debug("Frais calculés: base={}, minimum={}, discount={}, TVA={}, final={}", 
                     fraisBase, fraisMinimum, discount, fraisAvecTVA, fraisFinal);

            return fraisFinal;

        } catch (Exception e) {
            log.error("Erreur calcul frais: {}", e.getMessage(), e);
            // Frais par défaut en cas d'erreur
            return MIN_FEES.getOrDefault(type, new BigDecimal("100"));
        }
    }

    /**
     * Récupération du taux spécifique à l'agence
     */
    private BigDecimal getTauxForAgence(TransactionType type, String idAgence) {
        try {
            Agence agence = agenceRepository.findById(idAgence).orElse(null);
            if (agence != null && agence.getTauxFrais() != null) {
                return agence.getTauxFrais(type);
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer taux agence {}: {}", idAgence, e.getMessage());
        }
        
        return DEFAULT_RATES.getOrDefault(type, new BigDecimal("2.0"));
    }

    /**
     * Calcul du discount selon les paliers
     */
    private BigDecimal calculateDiscount(BigDecimal montant) {
        return DISCOUNT_TIERS.entrySet().stream()
                .filter(entry -> montant.compareTo(entry.getKey()) >= 0)
                .map(Map.Entry::getValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Estimation des frais (sans les appliquer)
     */
    public Map<String, BigDecimal> estimateFrais(TransactionType type, BigDecimal montant, String idAgence) {
        BigDecimal frais = calculateFrais(type, montant, idAgence);
        BigDecimal montantNet = montant.subtract(frais);
        
        Map<String, BigDecimal> estimation = new HashMap<>();
        estimation.put("montantBrut", montant);
        estimation.put("frais", frais);
        estimation.put("montantNet", montantNet);
        estimation.put("total", montant.add(frais));
        
        return estimation;
    }

    /**
     * Frais de tenue de compte mensuel
     */
    public BigDecimal calculateMonthlyAccountFee(String idAgence) {
        // Frais fixe de 500 FCFA/mois par défaut
        return new BigDecimal("500");
    }
}
