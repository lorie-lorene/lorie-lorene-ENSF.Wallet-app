package com.serviceAgence.services;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceAgence.dto.*;
import com.serviceAgence.exception.AgenceException;
import com.serviceAgence.model.Agence;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.repository.AgenceRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class AgenceService {

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private KYCService kycService;

    @Autowired
    private CompteService compteService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Traitement complet d'une demande de création de compte
     */
    public RegistrationProcessingResult processRegistrationRequest(UserRegistrationRequest request) {
        log.info("Traitement demande création compte: client={}, agence={}", 
                request.getIdClient(), request.getIdAgence());

        try {
            // 1. Validation de l'agence
            Agence agence = getAgenceOrThrow(request.getIdAgence());
            if (!agence.isActive()) {
                return RegistrationProcessingResult.rejected("AGENCE_INACTIVE", 
                    "L'agence sélectionnée n'est pas active");
            }

            // 2. Validation KYC des documents
            KYCValidationResult kycResult = kycService.validateDocuments(
                request.getIdClient(), 
                request.getCni(),
                request.getRectoCni(),
                request.getVersoCni()
            );

            if (!kycResult.isValid()) {
                log.warn("Validation KYC échouée pour {}: {}", request.getIdClient(), kycResult.getReason());
                return RegistrationProcessingResult.rejected(kycResult.getErrorCode(), kycResult.getReason());
            }

            // 3. Création du compte
            AccountCreationRequest accountRequest = new AccountCreationRequest();
            accountRequest.setIdClient(request.getIdClient());
            accountRequest.setIdAgence(request.getIdAgence());

            AccountCreationResult accountResult = compteService.createAccount(accountRequest);
            
            if (!accountResult.isSuccess()) {
                return RegistrationProcessingResult.rejected(accountResult.getErrorCode(), 
                    accountResult.getMessage());
            }

            // 4. Notification de succès
            CompteUser compte = compteService.getAccountDetails(accountResult.getNumeroCompte().toString());
            notificationService.sendAccountCreationNotification(compte);

            // 5. Mise à jour statistiques agence
            updateAgenceStatistics(agence);

            log.info("Compte créé avec succès: client={}, numéro={}", 
                    request.getIdClient(), accountResult.getNumeroCompte());

            return RegistrationProcessingResult.accepted(accountResult.getNumeroCompte(), 
                "Compte créé avec succès");

        } catch (Exception e) {
            log.error("Erreur traitement demande: {}", e.getMessage(), e);
            return RegistrationProcessingResult.rejected("ERREUR_TECHNIQUE", 
                "Erreur technique lors du traitement");
        }
    }

    /**
     * Traitement d'une transaction
     */
    public TransactionResult processTransaction(TransactionRequest request) {
        log.info("Traitement transaction: type={}, montant={}, compte={}", 
                request.getType(), request.getMontant(), request.getCompteSource());

        try {
            // Validation de l'agence
            Agence agence = getAgenceOrThrow(request.getIdAgence());
            if (!agence.isActive()) {
                throw new AgenceException("AGENCE_INACTIVE", "Agence non active");
            }

            // Traitement via TransactionService
            TransactionResult result = transactionService.processTransaction(request);

            // Mise à jour statistiques si succès
            if (result.isSuccess()) {
                updateAgenceTransactionStats(agence, request.getMontant());
            }

            return result;

        } catch (Exception e) {
            log.error("Erreur traitement transaction: {}", e.getMessage(), e);
            return TransactionResult.failed("TXN_ERROR", "Erreur technique");
        }
    }

    /**
     * Récupération des informations d'une agence
     */
    public Agence getAgenceInfo(String idAgence) {
        return getAgenceOrThrow(idAgence);
    }

    /**
     * Récupération des statistiques d'une agence
     */
    public AgenceStatistics getAgenceStatistics(String idAgence) {
        Agence agence = getAgenceOrThrow(idAgence);
        Map<String, Object> compteStats = compteService.getAccountStatistics(idAgence);

        AgenceStatistics stats = new AgenceStatistics();
        stats.setIdAgence(idAgence);
        stats.setNomAgence(agence.getNom());
        stats.setTotalComptes((Long) compteStats.get("totalComptes"));
        stats.setComptesActifs((Long) compteStats.get("comptesActifs"));
        stats.setComptesSuspendus((Long) compteStats.get("comptesSuspendus"));
        stats.setComptesBloqués((Long) compteStats.get("comptesBloqués"));
        stats.setTotalSoldes((BigDecimal) compteStats.get("totalSoldes"));
        stats.setTotalTransactions(agence.getTotalTransactions());
        stats.setTotalVolume(agence.getTotalVolume());
        stats.setCapital(agence.getCapital());
        stats.setSoldeDisponible(agence.getSoldeDisponible());
        stats.setGeneratedAt(LocalDateTime.now());

        return stats;
    }

    /**
     * Mise à jour des statistiques d'agence après création compte
     */
    private void updateAgenceStatistics(Agence agence) {
        agence.setTotalComptes(agence.getTotalComptes() + 1);
        agence.updateLastActivity();
        agenceRepository.save(agence);
    }

    /**
     * Mise à jour des statistiques d'agence après transaction
     */
    private void updateAgenceTransactionStats(Agence agence, BigDecimal montant) {
        agence.setTotalTransactions(agence.getTotalTransactions() + 1);
        agence.setTotalVolume(agence.getTotalVolume().add(montant));
        agence.updateLastActivity();
        agenceRepository.save(agence);
    }

    /**
     * Récupération sécurisée d'une agence
     */
    private Agence getAgenceOrThrow(String idAgence) {
        return agenceRepository.findById(idAgence)
                .orElseThrow(() -> new AgenceException("AGENCE_INTROUVABLE", 
                    "Agence " + idAgence + " introuvable"));
    }

    /**
     * Validation des limites d'une agence
     */
    public boolean validateAgenceLimits(String idAgence, BigDecimal montant) {
        Agence agence = getAgenceOrThrow(idAgence);
        
        // Vérifier capital disponible
        if (agence.getSoldeDisponible().compareTo(montant) < 0) {
            log.warn("Limite capital dépassée pour agence {}: requis={}, disponible={}", 
                    idAgence, montant, agence.getSoldeDisponible());
            return false;
        }

        return true;
    }

    /**
     * Récupération des comptes d'une agence
     */
    public List<CompteUser> getAgenceAccounts(String idAgence, int limit) {
        getAgenceOrThrow(idAgence); // Validation existence
        return compteService.getAgenceAccounts(idAgence, limit);
    }

    /**
     * Recherche de compte par numéro
     */
    public CompteUser findAccountByNumber(String numeroCompte) {
        return compteService.getAccountDetails(numeroCompte);
    }

    /**
     * Suspension d'un compte par l'agence
     */
    public void suspendAccount(String numeroCompte, String reason, String suspendedBy) {
        compteService.suspendAccount(numeroCompte, reason, suspendedBy);
    }

    /**
     * Activation d'un compte par l'agence
     */
    public void activateAccount(String numeroCompte, String activatedBy) {
        compteService.activateAccount(numeroCompte, activatedBy);
    }
}
