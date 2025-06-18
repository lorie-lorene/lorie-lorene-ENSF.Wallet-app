package com.serviceAgence.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceAgence.dto.TransactionRequest;
import com.serviceAgence.dto.TransactionResult;
import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.exception.TransactionException;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.model.Transaction;
import com.serviceAgence.repository.CompteRepository;
import com.serviceAgence.repository.TransactionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class TransactionService {

    @Autowired
    private CompteRepository compteRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraisService fraisService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Traitement d'une transaction
     */
    public TransactionResult processTransaction(TransactionRequest request) {
        log.info("Début traitement transaction: type={}, montant={}, compte={}",
                request.getType(), request.getMontant(), request.getCompteSource());

        String transactionId = generateTransactionId();

        try {
            // 1. Validation de base
            validateTransactionRequest(request);

            // 2. Récupération des comptes
            CompteUser compteSource = getCompteOrThrow(request.getCompteSource());
            CompteUser compteDestination = null;

            if (request.getType().requiresDestination()) {
                compteDestination = getCompteOrThrow(request.getCompteDestination());
            }

            // 3. Validation métier CRITIQUE
            validateBusinessRules(compteSource, compteDestination, request);

            // 4. Calcul des frais
            BigDecimal frais = fraisService.calculateFrais(request.getType(), request.getMontant(),
                    compteSource.getIdAgence());

            // 5. Création de la transaction
            Transaction transaction = createTransaction(transactionId, request, frais,
                    compteSource, compteDestination);

            // 6. Exécution atomique
            return executeTransaction(transaction, compteSource, compteDestination);

        } catch (TransactionException e) {
            log.warn("Transaction échouée [{}]: {}", transactionId, e.getMessage());
            return TransactionResult.failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Erreur technique transaction [{}]: {}", transactionId, e.getMessage(), e);
            return TransactionResult.failed("ERREUR_TECHNIQUE", "Erreur technique");
        }
    }

    /**
     * Validation de la demande de transaction
     */
    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("MONTANT_INVALIDE", "Le montant doit être positif");
        }

        if (request.getMontant().compareTo(new BigDecimal("50000000")) > 0) { // 50M FCFA
            throw new TransactionException("MONTANT_TROP_ELEVE", "Montant maximum dépassé");
        }

        if (request.getType().requiresDestination() && request.getCompteDestination() == null) {
            throw new TransactionException("COMPTE_DESTINATION_REQUIS",
                    "Compte de destination obligatoire pour ce type de transaction");
        }
    }

    /**
     * Validation des règles métier - CORRIGÉE
     */
    private void validateBusinessRules(CompteUser compteSource, CompteUser compteDestination,
            TransactionRequest request) {

        log.debug("=== VALIDATION BUSINESS RULES ===");
        log.debug("Type transaction: {}", request.getType());
        log.debug("Montant: {}", request.getMontant());
        log.debug("Solde compte source: {}", compteSource.getSolde());
        log.debug("Compte source actif: {}", compteSource.isActive());

        // 1. Vérification statut compte source
        if (!compteSource.isActive()) {
            throw new TransactionException("COMPTE_INACTIF",
                    "Le compte source n'est pas actif");
        }

        // 2. CRITIQUE : Pour RETRAIT et TRANSFERT, vérifier le solde suffisant
        if (request.getType() != TransactionType.DEPOT_PHYSIQUE) {

            // Calcul du montant total requis (montant + frais)
            BigDecimal frais = fraisService.calculateFrais(request.getType(), request.getMontant(),
                    compteSource.getIdAgence());
            BigDecimal montantTotal = request.getMontant().add(frais);

            log.debug("Frais calculés: {}", frais);
            log.debug("Montant total requis: {}", montantTotal);
            log.debug("Solde disponible: {}", compteSource.getSolde());

            // VÉRIFICATION CRITIQUE DU SOLDE
            if (compteSource.getSolde().compareTo(montantTotal) < 0) {
                String errorMessage = String.format("Solde insuffisant. Requis: %s FCFA, Disponible: %s FCFA",
                        montantTotal, compteSource.getSolde());
                log.warn("SOLDE INSUFFISANT: {}", errorMessage);
                throw new TransactionException("SOLDE_INSUFFISANT", errorMessage);
            }

            log.debug("✅ Solde suffisant pour la transaction");
        } else {
            log.debug("Type DEPOT_PHYSIQUE - pas de vérification de solde");
        }

        // 3. Vérification limites quotidiennes
        if (request.getType().isWithdrawal() && !compteSource.canWithdraw(request.getMontant())) {
            throw new TransactionException("LIMITE_RETRAIT_DEPASSEE",
                    "Limite de retrait quotidienne dépassée");
        }

        if (request.getType().isTransfer() && !compteSource.canTransfer(request.getMontant())) {
            throw new TransactionException("LIMITE_TRANSFERT_DEPASSEE",
                    "Limite de transfert quotidienne dépassée");
        }

        // 4. Vérification compte destination si applicable
        if (compteDestination != null) {
            if (!compteDestination.isActive()) {
                throw new TransactionException("COMPTE_DESTINATION_INACTIF",
                        "Le compte de destination n'est pas actif");
            }

            // Vérification auto-transfert
            if (compteSource.getNumeroCompte().equals(compteDestination.getNumeroCompte())) {
                throw new TransactionException("AUTO_TRANSFERT_INTERDIT",
                        "Impossible de transférer vers le même compte");
            }
        }

        log.debug("✅ Toutes les validations métier sont OK");
    }

    /**
     * Création de l'objet Transaction
     */
    private Transaction createTransaction(String transactionId, TransactionRequest request,
            BigDecimal frais, CompteUser compteSource,
            CompteUser compteDestination) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setType(request.getType());
        transaction.setMontant(request.getMontant());
        transaction.setFrais(frais);
        transaction.setMontantNet(request.getMontant().subtract(frais));
        transaction.setCompteSource(compteSource.getNumeroCompte().toString());
        transaction.setIdAgence(compteSource.getIdAgence());
        transaction.setIdClient(compteSource.getIdClient());
        transaction.setDescription(request.getDescription());
        transaction.setReferenceExterne(request.getReferenceExterne());

        if (compteDestination != null) {
            transaction.setCompteDestination(compteDestination.getNumeroCompte().toString());
        }

        // Audit des soldes avant transaction
        transaction.setSoldeAvantSource(compteSource.getSolde());
        if (compteDestination != null) {
            transaction.setSoldeAvantDestination(compteDestination.getSolde());
        }

        return transaction;
    }

    /**
     * Exécution atomique de la transaction
     */
    private TransactionResult executeTransaction(Transaction transaction, CompteUser compteSource,
            CompteUser compteDestination) {
        try {
            // Marquer comme en cours
            transaction.markAsProcessing("SYSTEM");
            transactionRepository.save(transaction);

            // Traitement selon le type de transaction
            if (transaction.getType() == TransactionType.DEPOT_PHYSIQUE) {
                // DÉPÔT : Créditer le compte (pas de débit)
                compteSource.credit(transaction.getMontant());
                transaction.setSoldeApresSource(compteSource.getSolde());

            } else {
                // RETRAIT/TRANSFERT : Débiter le compte source (montant + frais)
                BigDecimal montantTotal = transaction.getMontant().add(transaction.getFrais());
                compteSource.debit(montantTotal);
                transaction.setSoldeApresSource(compteSource.getSolde());

                // Si transfert, créditer le compte destination
                if (compteDestination != null) {
                    compteDestination.credit(transaction.getMontant());
                    transaction.setSoldeApresDestination(compteDestination.getSolde());
                }
            }

            // Sauvegarder les comptes
            compteRepository.save(compteSource);
            if (compteDestination != null) {
                compteRepository.save(compteDestination);
            }

            // Marquer la transaction comme réussie
            transaction.markAsCompleted();
            transactionRepository.save(transaction);

            // Envoyer notification
            notificationService.sendTransactionNotification(transaction);

            log.info("Transaction réussie [{}]: {} FCFA de {} vers {}",
                    transaction.getTransactionId(), transaction.getMontant(),
                    transaction.getCompteSource(), transaction.getCompteDestination());

            return TransactionResult.success(transaction.getTransactionId(),
                    transaction.getMontant(), transaction.getFrais());

        } catch (Exception e) {
            // Rollback automatique grâce à @Transactional
            transaction.markAsFailed("EXECUTION_ERROR", e.getMessage());
            transactionRepository.save(transaction);

            throw new TransactionException("EXECUTION_FAILED",
                    "Erreur lors de l'exécution de la transaction: " + e.getMessage());
        }
    }

    /**
     * Récupération sécurisée d'un compte
     */
    private CompteUser getCompteOrThrow(String numeroCompte) {
        try {
            Long numero = Long.parseLong(numeroCompte);
            return compteRepository.findByNumeroCompte(numero)
                    .orElseThrow(() -> new TransactionException("COMPTE_INTROUVABLE",
                            "Compte " + numeroCompte + " introuvable"));
        } catch (NumberFormatException e) {
            throw new TransactionException("NUMERO_COMPTE_INVALIDE",
                    "Format de numéro de compte invalide: " + numeroCompte);
        }
    }

    /**
     * Génération d'un ID de transaction unique
     */
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Récupération de l'historique des transactions d'un compte
     */
    public List<Transaction> getAccountHistory(String numeroCompte, int limit) {
        Page<Transaction> transactions = transactionRepository.findAccountHistory(
                numeroCompte,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        return transactions.getContent();
    }

    /**
     * Récupération du solde d'un compte
     */
    public BigDecimal getAccountBalance(String numeroCompte) {
        CompteUser compte = getCompteOrThrow(numeroCompte);
        return compte.getSolde();
    }
}
