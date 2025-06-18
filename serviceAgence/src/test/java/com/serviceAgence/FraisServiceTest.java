package com.serviceAgence;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.model.Agence;
import com.serviceAgence.repository.AgenceRepository;
import com.serviceAgence.services.FraisService;

@ExtendWith(MockitoExtension.class)
class FraisServiceTest {

    @Mock
    private AgenceRepository agenceRepository;

    @InjectMocks
    private FraisService fraisService;

    private Agence testAgence;

    @BeforeEach
    void setUp() {
        testAgence = new Agence();
        testAgence.setIdAgence("AGENCE001");
        testAgence.setNom("Agence Test");
        
        // Configuration des taux personnalisés
        Map<TransactionType, BigDecimal> tauxCustom = Map.of(
            TransactionType.RETRAIT_PHYSIQUE, new BigDecimal("2.0"), // 2% au lieu de 1.5%
            TransactionType.TRANSFERT_INTERNE, new BigDecimal("0.5") // 0.5% au lieu de 1%
        );
        testAgence.setTauxFrais(tauxCustom);
    }

    @Test
    void testCalculateFrais_DepotPhysique() {
        // Given
        when(agenceRepository.findById("AGENCE001"))
            .thenReturn(Optional.of(testAgence));

        // When
        BigDecimal frais = fraisService.calculateFrais(
            TransactionType.DEPOT_PHYSIQUE, 
            new BigDecimal("10000"), 
            "AGENCE001"
        );

        // Then
        assertEquals(BigDecimal.ZERO.setScale(0), frais); // Dépôt gratuit
    }

    @Test
    void testCalculateFrais_RetraitPhysique_TauxPersonnalise() {
        // Given
        when(agenceRepository.findById("AGENCE001"))
            .thenReturn(Optional.of(testAgence));

        // When
        BigDecimal frais = fraisService.calculateFrais(
            TransactionType.RETRAIT_PHYSIQUE, 
            new BigDecimal("10000"), 
            "AGENCE001"
        );

        // Then
        // 10000 * 2% = 200, avec TVA 17.5% = 235, minimum 100 FCFA
        assertTrue(frais.compareTo(new BigDecimal("200")) >= 0);
        verify(agenceRepository).findById("AGENCE001");
    }

    @Test
    void testCalculateFrais_FraisMinimum() {
        // Given
        when(agenceRepository.findById("AGENCE001"))
            .thenReturn(Optional.of(testAgence));

        // When - Petit montant qui donnerait un frais inférieur au minimum
        BigDecimal frais = fraisService.calculateFrais(
            TransactionType.RETRAIT_PHYSIQUE, 
            new BigDecimal("1000"), // 1000 * 2% = 20 FCFA
            "AGENCE001"
        );

        // Then - Doit appliquer le minimum de 100 FCFA + TVA
        assertTrue(frais.compareTo(new BigDecimal("100")) >= 0);
    }

    @Test
    void testCalculateFrais_GrosMontant_AvecDiscount() {
        // Given
        when(agenceRepository.findById("AGENCE001"))
            .thenReturn(Optional.of(testAgence));

        // When - Montant élevé pour déclencher le discount
        BigDecimal frais = fraisService.calculateFrais(
            TransactionType.RETRAIT_PHYSIQUE, 
            new BigDecimal("2000000"), // 2M FCFA
            "AGENCE001"
        );

        // Then - Frais de base: 2M * 2% = 40000, avec discount 10% = 36000, avec TVA = 42300
        BigDecimal fraisAttendu = new BigDecimal("42300");
        assertTrue(frais.compareTo(fraisAttendu.multiply(new BigDecimal("0.9"))) >= 0); // Tolérance 10%
        assertTrue(frais.compareTo(fraisAttendu.multiply(new BigDecimal("1.1"))) <= 0);
    }

    @Test
    void testCalculateFrais_AgenceIntrouvable_TauxParDefaut() {
        // Given
        when(agenceRepository.findById("AGENCE_INEXISTANTE"))
            .thenReturn(Optional.empty());

        // When
        BigDecimal frais = fraisService.calculateFrais(
            TransactionType.RETRAIT_PHYSIQUE, 
            new BigDecimal("10000"), 
            "AGENCE_INEXISTANTE"
        );

        // Then - Doit utiliser le taux par défaut (1.5%)
        assertTrue(frais.compareTo(BigDecimal.ZERO) > 0);
        verify(agenceRepository).findById("AGENCE_INEXISTANTE");
    }

    @Test
    void testEstimateFrais() {
        // Given
        when(agenceRepository.findById("AGENCE001"))
            .thenReturn(Optional.of(testAgence));

        // When
        Map<String, BigDecimal> estimation = fraisService.estimateFrais(
            TransactionType.TRANSFERT_INTERNE, 
            new BigDecimal("5000"), 
            "AGENCE001"
        );

        // Then
        assertNotNull(estimation);
        assertTrue(estimation.containsKey("montantBrut"));
        assertTrue(estimation.containsKey("frais"));
        assertTrue(estimation.containsKey("montantNet"));
        assertTrue(estimation.containsKey("total"));
        
        assertEquals(new BigDecimal("5000"), estimation.get("montantBrut"));
        assertTrue(estimation.get("frais").compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateMonthlyAccountFee() {
        // When
        BigDecimal frais = fraisService.calculateMonthlyAccountFee("AGENCE001");

        // Then
        assertEquals(new BigDecimal("500"), frais);
    }
}