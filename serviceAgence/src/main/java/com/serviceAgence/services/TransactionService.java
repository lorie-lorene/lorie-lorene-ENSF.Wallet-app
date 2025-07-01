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
     public BigDecimal getAccountBalanceClient(String idClient) {
        
            log.info("test entrant = {}", idClient);
        CompteUser compte = compteRepository.findByIdClient(idClient);
        
            
            log.info("test2 = {}", compte.getSolde());
        return compte.getSolde();

    }

    /**
     * Traitement d'une transaction (compte-à-compte OU compte-vers-carte)
     * 
     */
    public TransactionResult processTransaction(TransactionRequest request) {
        log.info("Début traitement transaction: type={}, montant={}, compte={}, destination={}",
                request.getType(), request.getMontant(), request.getCompteSource(),
                request.getDestination());

        String transactionId = generateTransactionId();

        try {
            // 1. Validation de base
            validateTransactionRequest(request);

            // 2. Récupération du compte source (toujours requis)
            CompteUser compteSource = getCompteOrThrow(request.getCompteSource());

            // 3. Pour transferts compte-à-compte, récupérer destination
            CompteUser compteDestination = null;
            if (request.getType().isAccountTransfer() && request.isAccountTransfer()) {
                compteDestination = getCompteOrThrow(request.getCompteDestination());
            }

            // 4. Validation métier
            validateBusinessRules(compteSource, compteDestination, request);

            // 5. Calcul des frais
            BigDecimal frais = fraisService.calculateFrais(request.getType(), request.getMontant(),
                    compteSource.getIdAgence());

            // 6. Création de la transaction
            Transaction transaction = createTransaction(transactionId, request, frais,
                    compteSource, compteDestination);

            // 7. Exécution atomique
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
     * Validation de la demande de transaction - MISE À JOUR
     */
    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("MONTANT_INVALIDE", "Le montant doit être positif");
        }

        if (request.getMontant().compareTo(new BigDecimal("50000000")) > 0) { // 50M FCFA
            throw new TransactionException("MONTANT_TROP_ELEVE", "Montant maximum dépassé");
        }

        // NOUVELLE LOGIQUE : Validation destination selon le type
        if (request.getType() == TransactionType.TRANSFERT_VERS_CARTE) {
            // Pour transfert vers carte, numeroCarteDestination requis
            if (request.getNumeroCarteDestination() == null ||
                    request.getNumeroCarteDestination().trim().isEmpty()) {
                throw new TransactionException("NUMERO_CARTE_REQUIS",
                        "Numéro de carte de destination obligatoire");
            }

            // Validation format numéro de carte (exemple basique)
            if (!request.getNumeroCarteDestination().matches("\\d{16}")) {
                throw new TransactionException("NUMERO_CARTE_INVALIDE",
                        "Format de numéro de carte invalide (16 chiffres attendus)");
            }

        } else if (request.getType().isAccountTransfer()) {
            // Pour transfert compte-à-compte, compteDestination requis
            if (request.getCompteDestination() == null ||
                    request.getCompteDestination().trim().isEmpty()) {
                throw new TransactionException("COMPTE_DESTINATION_REQUIS",
                        "Compte de destination obligatoire pour ce type de transaction");
            }
        }
    }

    /**
     * Validation des règles métier - MISE À JOUR
     */
    private void validateBusinessRules(CompteUser compteSource, CompteUser compteDestination,
            TransactionRequest request) {

        log.debug("=== VALIDATION BUSINESS RULES ===");
        log.debug("Type transaction: {}", request.getType());
        log.debug("Montant: {}", request.getMontant());
        log.debug("Solde compte source: {}", compteSource.getSolde());

        // 1. Vérification statut compte source
        if (!compteSource.isActive()) {
            throw new TransactionException("COMPTE_INACTIF",
                    "Le compte source n'est pas actif");
        }

        // 2. Pour RETRAIT, TRANSFERT et TRANSFERT_VERS_CARTE : vérifier solde
        if (request.getType() != TransactionType.DEPOT_PHYSIQUE) {
            BigDecimal frais = fraisService.calculateFrais(request.getType(), request.getMontant(),
                    compteSource.getIdAgence());
            BigDecimal montantTotal = request.getMontant().add(frais);

            if (compteSource.getSolde().compareTo(montantTotal) < 0) {
                String errorMessage = String.format("Solde insuffisant. Requis: %s FCFA, Disponible: %s FCFA",
                        montantTotal, compteSource.getSolde());
                throw new TransactionException("SOLDE_INSUFFISANT", errorMessage);
            }
        }

        // 3. Vérification limites quotidiennes
        if (request.getType().isWithdrawal() && !compteSource.canWithdraw(request.getMontant())) {
            throw new TransactionException("LIMITE_RETRAIT_DEPASSEE",
                    "Limite de retrait quotidienne dépassée");
        }

        // NOUVELLE LOGIQUE : Transfert vers carte = même limite que transfert
        if ((request.getType().isTransfer() || request.getType() == TransactionType.TRANSFERT_VERS_CARTE)
                && !compteSource.canTransfer(request.getMontant())) {
            throw new TransactionException("LIMITE_TRANSFERT_DEPASSEE",
                    "Limite de transfert quotidienne dépassée");
        }

        // 4. Validation compte destination (seulement pour transferts compte-à-compte)
        if (compteDestination != null) {
            if (!compteDestination.isActive()) {
                throw new TransactionException("COMPTE_DESTINATION_INACTIF",
                        "Le compte de destination n'est pas actif");
            }

            if (compteSource.getNumeroCompte().equals(compteDestination.getNumeroCompte())) {
                throw new TransactionException("AUTO_TRANSFERT_INTERDIT",
                        "Impossible de transférer vers le même compte");
            }
        }

        log.debug("✅ Toutes les validations métier sont OK");
    }

    /**
     * Création de l'objet Transaction - MISE À JOUR
     */
    private Transaction createTransaction(String transactionId, TransactionRequest request,
            BigDecimal frais, CompteUser compteSource, CompteUser compteDestination) {

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

        // NOUVELLE LOGIQUE : Destination selon le type
        if (request.getType() == TransactionType.TRANSFERT_VERS_CARTE) {
            // Pour transfert vers carte, stocker comme "CARTE:numero"
            transaction.setCompteDestination("CARTE:" + request.getNumeroCarteDestination());

        } else if (compteDestination != null) {
            // Pour transfert compte-à-compte
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
     * Exécution atomique de la transaction - MISE À JOUR
     */
    private TransactionResult executeTransaction(Transaction transaction, CompteUser compteSource,
            CompteUser compteDestination) {
        try {
            transaction.markAsProcessing("SYSTEM");
            transactionRepository.save(transaction);

            if (transaction.getType() == TransactionType.DEPOT_PHYSIQUE) {
                // DÉPÔT : Créditer le compte
                compteSource.credit(transaction.getMontant());
                transaction.setSoldeApresSource(compteSource.getSolde());

            } else {
                // RETRAIT/TRANSFERT/TRANSFERT_VERS_CARTE : Débiter le compte source
                BigDecimal montantTotal = transaction.getMontant().add(transaction.getFrais());
                compteSource.debit(montantTotal);
                transaction.setSoldeApresSource(compteSource.getSolde());

                // DIFFÉRENCIATION : Transfert compte vs carte
                if (transaction.getType() == TransactionType.TRANSFERT_VERS_CARTE) {
                    // TRANSFERT VERS CARTE : Juste débiter (vous gérez la suite)
                    log.info("Débit effectué pour transfert vers carte: {} FCFA vers carte {}",
                            transaction.getMontant(),
                            transaction.getCompteDestination().replace("CARTE:", ""));

                } else if (compteDestination != null) {
                    // TRANSFERT COMPTE-À-COMPTE : Créditer le compte destination
                    compteDestination.credit(transaction.getMontant());
                    transaction.setSoldeApresDestination(compteDestination.getSolde());
                }
            }

            // Sauvegarder les comptes
            compteRepository.save(compteSource);
            if (compteDestination != null) {
                compteRepository.save(compteDestination);
            }

            transaction.markAsCompleted();
            transactionRepository.save(transaction);

            notificationService.sendTransactionNotification(transaction);

            log.info("Transaction réussie [{}]: {} FCFA de {} vers {}",
                    transaction.getTransactionId(), transaction.getMontant(),
                    transaction.getCompteSource(), transaction.getCompteDestination());

            return TransactionResult.success(transaction.getTransactionId(),
                    transaction.getMontant(), transaction.getFrais());

        } catch (Exception e) {
            transaction.markAsFailed("EXECUTION_ERROR", e.getMessage());
            transactionRepository.save(transaction);
            throw new TransactionException("EXECUTION_FAILED",
                    "Erreur lors de l'exécution: " + e.getMessage());
        }
    }

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

}
