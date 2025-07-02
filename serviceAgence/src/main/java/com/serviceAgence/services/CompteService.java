package com.serviceAgence.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceAgence.dto.AccountCreationRequest;
import com.serviceAgence.dto.AccountCreationResult;
import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.enums.CompteType;
import com.serviceAgence.exception.CompteException;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.repository.CompteRepository;
import com.serviceAgence.utils.AccountNumberGenerator;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class CompteService {

    @Autowired
    private CompteRepository compteRepository;

    @Autowired
    private AccountNumberGenerator accountNumberGenerator;

    @Autowired
    private NotificationService notificationService;

    /**
     * Création d'un nouveau compte
     */
    public AccountCreationResult createAccount(AccountCreationRequest request) {
        log.info("Création compte pour client: {}, agence: {}", request.getIdClient(), request.getIdAgence());

        try {
            // 1. Vérification unicité client-agence
            if (compteRepository.existsByIdClientAndIdAgence(request.getIdClient(), request.getIdAgence())) {
                throw new CompteException("COMPTE_DEJA_EXISTANT",
                        "Un compte existe déjà pour ce client dans cette agence");
            }

            // 2. Génération numéro de compte unique
            Long numeroCompte = accountNumberGenerator.generateAccountNumber(
                    request.getIdClient(), request.getIdAgence());

            // 3. Création du compte
            CompteUser compte = new CompteUser();
            compte.setNumeroCompte(numeroCompte);
            compte.setIdClient(request.getIdClient());
            compte.setIdAgence(request.getIdAgence());
            compte.setSolde(BigDecimal.ZERO);
            compte.setStatus(CompteStatus.ACTIVE); // Directement actif après validation KYC
            compte.setType(CompteType.STANDARD);
            compte.setActivatedAt(LocalDateTime.now());
            compte.setActivatedBy("SYSTEM_AGENCE");

            // 4. Configuration des limites par défaut
            configureDefaultLimits(compte);

            // 5. Sauvegarde
            CompteUser savedCompte = compteRepository.save(compte);

            log.info("Compte créé avec succès: numéro={}, client={}",
                    savedCompte.getNumeroCompte(), savedCompte.getIdClient());

            return AccountCreationResult.success(savedCompte.getNumeroCompte(), savedCompte.getIdClient());

        } catch (CompteException e) {
            log.warn("Échec création compte: {}", e.getMessage());
            return AccountCreationResult.failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Erreur technique création compte: {}", e.getMessage(), e);
            return AccountCreationResult.failed("ERREUR_TECHNIQUE", "Erreur technique lors de la création");
        }
    }

    /**
     * Activation d'un compte
     */
    public void activateAccount(String numeroCompte, String activatedBy) {
        CompteUser compte = getCompteOrThrow(numeroCompte);

        if (compte.getStatus() == CompteStatus.ACTIVE) {
            throw new CompteException("COMPTE_DEJA_ACTIF", "Le compte est déjà actif");
        }

        compte.addStatusHistory(CompteStatus.ACTIVE, "Activation manuelle", activatedBy);
        compte.setActivatedAt(LocalDateTime.now());
        compte.setActivatedBy(activatedBy);

        compteRepository.save(compte);
        notificationService.sendAccountActivationNotification(compte);

        log.info("Compte activé: {} par {}", numeroCompte, activatedBy);
    }

    /**
     * Suspension d'un compte
     */
    public void suspendAccount(String numeroCompte, String reason, String suspendedBy) {
        CompteUser compte = getCompteOrThrow(numeroCompte);

        compte.addStatusHistory(CompteStatus.SUSPENDED, reason, suspendedBy);
        compte.setBlocked(true);
        compte.setBlockedReason(reason);
        compte.setBlockedAt(LocalDateTime.now());
        compte.setBlockedBy(suspendedBy);

        compteRepository.save(compte);
        notificationService.sendAccountSuspensionNotification(compte, reason);

        log.info("Compte suspendu: {} par {} - Raison: {}", numeroCompte, suspendedBy, reason);
    }

    /**
     * Blocage définitif d'un compte
     */
    public void blockAccount(String numeroCompte, String reason, String blockedBy) {
        CompteUser compte = getCompteOrThrow(numeroCompte);

        compte.addStatusHistory(CompteStatus.BLOCKED, reason, blockedBy);
        compte.setBlocked(true);
        compte.setBlockedReason(reason);
        compte.setBlockedAt(LocalDateTime.now());
        compte.setBlockedBy(blockedBy);

        compteRepository.save(compte);
        notificationService.sendAccountBlockNotification(compte, reason);

        log.info("Compte bloqué: {} par {} - Raison: {}", numeroCompte, blockedBy, reason);
    }

    /**
     * Récupération du solde
     */
    public BigDecimal getAccountBalance(String numeroCompte) {
        CompteUser compte = getCompteOrThrow(numeroCompte);
        return compte.getSolde();
    }

    /**
     * Récupération des informations complètes du compte
     */
    public CompteUser getAccountDetails(String numeroCompte) {
        return getCompteOrThrow(numeroCompte);
    }

    /**
     * Recherche de comptes par client
     */
    public List<CompteUser> getClientAccounts(String idClient) {
        return compteRepository.findByIdClientOrderByCreatedAtDesc(idClient);
    }

    /**
     * Recherche de comptes par agence
     */
    public List<CompteUser> getAgenceAccounts(String idAgence, int limit) {
        return compteRepository.findByIdAgenceOrderByCreatedAtDesc(idAgence, PageRequest.of(0, limit));
    }

    /**
     * Mise à jour des limites d'un compte
     */
    public void updateAccountLimits(String numeroCompte, BigDecimal dailyWithdrawal,
            BigDecimal dailyTransfer, BigDecimal monthlyOperations) {
        CompteUser compte = getCompteOrThrow(numeroCompte);

        compte.setLimiteDailyWithdrawal(dailyWithdrawal);
        compte.setLimiteDailyTransfer(dailyTransfer);
        compte.setLimiteMonthlyOperations(monthlyOperations);

        compteRepository.save(compte);

        log.info("Limites mises à jour pour compte: {}", numeroCompte);
    }

    /**
     * Configuration des limites par défaut
     */
    private void configureDefaultLimits(CompteUser compte) {
        compte.setLimiteDailyWithdrawal(new BigDecimal("1000000")); // 1M FCFA
        compte.setLimiteDailyTransfer(new BigDecimal("2000000")); // 2M FCFA
        compte.setLimiteMonthlyOperations(new BigDecimal("10000000")); // 10M FCFA
    }

    /**
     * Récupération sécurisée d'un compte
     */
    private CompteUser getCompteOrThrow(String numeroCompte) {
        try {
            Long numero = Long.parseLong(numeroCompte);
            return compteRepository.findByNumeroCompte(numero)
                    .orElseThrow(() -> new CompteException("COMPTE_INTROUVABLE",
                            "Compte " + numeroCompte + " introuvable"));
        } catch (NumberFormatException e) {
            throw new CompteException("NUMERO_COMPTE_INVALIDE",
                    "Format de numéro de compte invalide: " + numeroCompte);
        }
    }

    /**
     * Récupération des comptes d'une agence
     */
    public List<CompteUser> getAllComptes(){
        return compteRepository.findAll();
    }

    /**
     * Statistiques des comptes
     */
    public Map<String, Object> getAccountStatistics(String idAgence) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalComptes", compteRepository.countByIdAgence(idAgence));
        stats.put("comptesActifs", compteRepository.countByIdAgenceAndStatus(idAgence, CompteStatus.ACTIVE));
        stats.put("comptesSuspendus", compteRepository.countByIdAgenceAndStatus(idAgence, CompteStatus.SUSPENDED));
        stats.put("comptesBloqués", compteRepository.countByIdAgenceAndStatus(idAgence, CompteStatus.BLOCKED));

        List<CompteUser> comptes = compteRepository.findByIdAgence(idAgence);
        BigDecimal totalSoldes = comptes.stream()
                .map(CompteUser::getSolde)
                .filter(solde -> solde != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalSoldes", totalSoldes);
        return stats;
    }
}