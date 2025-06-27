// serviceAgence/src/main/java/com/serviceAgence/controller/CompteController.java

package com.serviceAgence.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.serviceAgence.model.CompteUser;
import com.serviceAgence.services.CompteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 🏦 Contrôleur pour la gestion des comptes
 * Fournit des endpoints pour récupérer les informations des comptes
 * Utilisé par les services externes comme bank-card-service
 */
@RestController
@RequestMapping("/api/v1/comptes")
@Slf4j
@Tag(name = "Comptes", description = "API de gestion des comptes bancaires")
public class CompteController {

    @Autowired
    private CompteService compteService;

    /**
     * Récupérer les comptes d'un client par ID
     * Endpoint utilisé par bank-card-service pour obtenir l'idAgence
     */
    @GetMapping("/client/{idClient}")
    @Operation(summary = "Récupérer les comptes d'un client", 
               description = "Retourne tous les comptes associés à un client")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comptes récupérés avec succès"),
        @ApiResponse(responseCode = "404", description = "Aucun compte trouvé pour ce client"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    public ResponseEntity<List<CompteUser>> getClientAccounts(
            @Parameter(description = "ID du client", required = true)
            @PathVariable String idClient) {
        
        try {
            log.info("🔍 Récupération des comptes pour client: {}", idClient);
            
            List<CompteUser> comptes = compteService.getClientAccounts(idClient);
            
            if (comptes.isEmpty()) {
                log.warn("⚠️ Aucun compte trouvé pour le client: {}", idClient);
                return ResponseEntity.notFound().build();
            }
            
            log.info("✅ {} compte(s) trouvé(s) pour le client: {}", comptes.size(), idClient);
            return ResponseEntity.ok(comptes);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des comptes pour client {}: {}", 
                     idClient, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer un compte spécifique par numéro
     */
    @GetMapping("/numero/{numeroCompte}")
    @Operation(summary = "Récupérer un compte par numéro", 
               description = "Retourne les détails d'un compte spécifique")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte récupéré avec succès"),
        @ApiResponse(responseCode = "404", description = "Compte non trouvé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    public ResponseEntity<CompteUser> getAccountByNumber(
            @Parameter(description = "Numéro du compte", required = true)
            @PathVariable String numeroCompte) {
        
        try {
            log.info("🔍 Récupération du compte: {}", numeroCompte);
            
            CompteUser compte = compteService.getAccountDetails(numeroCompte);
            
            log.info("✅ Compte trouvé: {} - Client: {}, Agence: {}", 
                    numeroCompte, compte.getIdClient(), compte.getIdAgence());
            return ResponseEntity.ok(compte);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du compte {}: {}", 
                     numeroCompte, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupérer le premier compte actif d'un client
     * Endpoint optimisé pour bank-card-service
     */
    @GetMapping("/client/{idClient}/primary")
    @Operation(summary = "Récupérer le compte principal d'un client", 
               description = "Retourne le premier compte actif du client (pour création de carte)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte principal récupéré"),
        @ApiResponse(responseCode = "404", description = "Aucun compte actif trouvé"),
        @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    public ResponseEntity<CompteUser> getClientPrimaryAccount(
            @Parameter(description = "ID du client", required = true)
            @PathVariable String idClient) {
        
        try {
            log.info("🔍 Récupération du compte principal pour client: {}", idClient);
            
            List<CompteUser> comptes = compteService.getClientAccounts(idClient);
            
            // Filtrer pour obtenir le premier compte actif
            CompteUser primaryAccount = comptes.stream()
                .filter(compte -> "ACTIVE".equals(compte.getStatus().toString()))
                .findFirst()
                .orElse(null);
            
            if (primaryAccount == null) {
                log.warn("⚠️ Aucun compte actif trouvé pour le client: {}", idClient);
                return ResponseEntity.notFound().build();
            }
            
            log.info("✅ Compte principal trouvé: {} - Agence: {}", 
                    primaryAccount.getNumeroCompte(), primaryAccount.getIdAgence());
            return ResponseEntity.ok(primaryAccount);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du compte principal pour client {}: {}", 
                     idClient, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer les statistiques des comptes d'une agence
     */
    @GetMapping("/agence/{idAgence}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENCE_MANAGER')")
    @Operation(summary = "Statistiques des comptes d'une agence")
    public ResponseEntity<Map<String, Object>> getAgenceAccountStats(
            @PathVariable String idAgence) {
        
        try {
            log.info("📊 Récupération des statistiques pour agence: {}", idAgence);
            
            Map<String, Object> stats = compteService.getAccountStatistics(idAgence);
            
            log.info("✅ Statistiques récupérées pour agence: {}", idAgence);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques pour agence {}: {}", 
                     idAgence, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vérifier la disponibilité d'un compte pour les opérations
     */
    @GetMapping("/numero/{numeroCompte}/availability")
    @Operation(summary = "Vérifier la disponibilité d'un compte")
    public ResponseEntity<Map<String, Object>> checkAccountAvailability(
            @PathVariable String numeroCompte) {
        
        try {
            CompteUser compte = compteService.getAccountDetails(numeroCompte);
            
            Map<String, Object> availability = Map.of(
                "available", "ACTIVE".equals(compte.getStatus().toString()),
                "status", compte.getStatus().toString(),
                "solde", compte.getSolde(),
                "idAgence", compte.getIdAgence(),
                "idClient", compte.getIdClient()
            );
            
            return ResponseEntity.ok(availability);
            
        } catch (Exception e) {
            log.error("❌ Erreur vérification disponibilité compte {}: {}", 
                     numeroCompte, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}