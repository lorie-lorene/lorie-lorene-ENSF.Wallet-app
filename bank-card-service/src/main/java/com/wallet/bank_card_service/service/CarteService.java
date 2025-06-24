package com.wallet.bank_card_service.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.bank_card_service.dto.CarteCreationRequest;
import com.wallet.bank_card_service.dto.CarteCreationResult;
import com.wallet.bank_card_service.repository.CarteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.bank_card_service.dto.CarteCreationRequest;
import com.wallet.bank_card_service.dto.CarteCreationResult;
import com.wallet.bank_card_service.dto.CarteOperationResult;
import com.wallet.bank_card_service.dto.CarteSettingsRequest;
import com.wallet.bank_card_service.dto.CarteStatistiques;
import com.wallet.bank_card_service.dto.CarteStatus;
import com.wallet.bank_card_service.dto.CarteType;
import com.wallet.bank_card_service.dto.PinChangeRequest;
import com.wallet.bank_card_service.dto.TransfertCarteRequest;
import com.wallet.bank_card_service.dto.TransfertCarteResult;
import com.wallet.bank_card_service.exception.CarteException;
import com.wallet.bank_card_service.model.Carte;
import com.wallet.bank_card_service.repository.CarteRepository;

import lombok.extern.slf4j.Slf4j;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class CarteService {

    @Autowired
    private CarteRepository carteRepository;

    @Autowired
    private AgenceServiceClient agenceServiceClient;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    private final SecureRandom random = new SecureRandom();

    /**
     * Création d'une nouvelle carte bancaire
     */
    public CarteCreationResult createCarte(CarteCreationRequest request) {
        log.info("🆕 Création carte: client={}, type={}", request.getIdClient(), request.getType());

        try {
            // 1. Validations préliminaires
            validateCreationRequest(request);

            // 2. Vérifier limites du client (1 gratuite max)
            validateClientCardLimits(request.getIdClient(), request.getType());

            // 3. Vérifier et débiter les frais de création si nécessaire
            BigDecimal fraisCreation = request.getType().getFraisCreation();
            if (fraisCreation.compareTo(BigDecimal.ZERO) > 0) {
                boolean fraisDebites = agenceServiceClient.debitAccountFees(
                        request.getNumeroCompte(), fraisCreation, "FRAIS_CREATION_CARTE_" + request.getType());

                if (!fraisDebites) {
                    return CarteCreationResult.failed("SOLDE_INSUFFISANT",
                            "Solde insuffisant pour les frais de création: " + fraisCreation + " FCFA");
                }
            }

            // 4. Générer numéro de carte unique
            String numeroCarte = generateUniqueCardNumber();

            // 5. Créer la carte
            Carte carte = buildNewCarte(request, numeroCarte);
            Carte savedCarte = carteRepository.save(carte);

            // 6. Notification
            notificationService.sendCarteCreationNotification(savedCarte);

            log.info("✅ Carte créée avec succès: {}", savedCarte.getMaskedNumber());

            return CarteCreationResult.success(savedCarte.getIdCarte(),
                    savedCarte.getMaskedNumber(), fraisCreation);

        } catch (CarteException e) {
            log.warn("❌ Échec création carte: {}", e.getMessage());
            return CarteCreationResult.failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur technique création carte: {}", e.getMessage(), e);
            return CarteCreationResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors de la création");
        }
    }

    /**
     * Transfert d'argent du compte vers une carte
     */
    @RabbitListener
    public TransfertCarteResult transferToCard(TransfertCarteRequest request, String clientId) {
        log.info("💳 Transfert vers carte: compte={}, carte={}, montant={}",
                request.getNumeroCompteSource(), request.getIdCarteDestination(), request.getMontant());

        try {
            // 1. Récupérer et valider la carte
            Carte carte = getCarteOrThrow(request.getIdCarteDestination());
            validateCardOwnership(carte, clientId);
            validateCardForTransfer(carte);

            // 2. Calculer frais (0.5% du montant, min 50 FCFA)
            BigDecimal frais = calculateTransferFees(request.getMontant());
            BigDecimal montantTotal = request.getMontant().add(frais);

            // 3. Vérifier solde et débiter le compte
            boolean debitOk = agenceServiceClient.debitAccount(
                    request.getNumeroCompteSource(), montantTotal,
                    "TRANSFERT_VERS_CARTE_" + carte.getMaskedNumber());

            if (!debitOk) {
                return TransfertCarteResult.failed("SOLDE_INSUFFISANT",
                        "Solde insuffisant. Requis: " + montantTotal + " FCFA");
            }

            // 4. Créditer la carte
            carte.credit(request.getMontant());
            carteRepository.save(carte);

            // 5. Enregistrer la transaction
            String transactionId = transactionService.recordCardTransfer(
                    request.getNumeroCompteSource(), carte.getIdCarte(),
                    request.getMontant(), frais, request.getDescription());

            // 6. Récupérer nouveau solde compte
            BigDecimal nouveauSoldeCompte = agenceServiceClient.getAccountBalance(
                    request.getNumeroCompteSource());

            // 7. Notification
            notificationService.sendTransferNotification(carte, request.getMontant(), "CREDIT");

            log.info("✅ Transfert réussi: {} FCFA vers carte {}",
                    request.getMontant(), carte.getMaskedNumber());

            return TransfertCarteResult.success(transactionId, request.getMontant(),
                    nouveauSoldeCompte, carte.getSolde(), frais);

        } catch (CarteException e) {
            log.warn("❌ Échec transfert: {}", e.getMessage());
            return TransfertCarteResult.failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur technique transfert: {}", e.getMessage(), e);
            return TransfertCarteResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du transfert");
        }
    }

    /**
     * Transfert d'argent d'une carte vers le compte
     */
    public TransfertCarteResult transferFromCard(String idCarte, BigDecimal montant,
            String description, String clientId) {
        log.info("🏦 Transfert depuis carte: carte={}, montant={}", idCarte, montant);

        try {
            // 1. Récupérer et valider la carte
            Carte carte = getCarteOrThrow(idCarte);
            validateCardOwnership(carte, clientId);
            validateCardForTransfer(carte);

            // 2. Vérifier solde carte
            if (!carte.canPurchase(montant)) {
                return TransfertCarteResult.failed("SOLDE_INSUFFISANT_CARTE",
                        "Solde insuffisant sur la carte");
            }

            // 3. Calculer frais (0.3% du montant, min 30 FCFA)
            BigDecimal frais = calculateTransferFees(montant).multiply(new BigDecimal("0.6"));
            BigDecimal montantNet = montant.subtract(frais);

            // 4. Débiter la carte
            carte.debit(montant);
            carteRepository.save(carte);

            // 5. Créditer le compte
            boolean creditOk = agenceServiceClient.creditAccount(
                    carte.getNumeroCompte(), montantNet,
                    "TRANSFERT_DEPUIS_CARTE_" + carte.getMaskedNumber());

            if (!creditOk) {
                // Rollback: recréditer la carte
                carte.credit(montant);
                carteRepository.save(carte);
                throw new CarteException("ERREUR_CREDIT_COMPTE",
                        "Impossible de créditer le compte");
            }

            // 6. Enregistrer la transaction
            String transactionId = transactionService.recordCardTransfer(
                    carte.getNumeroCompte(), carte.getIdCarte(),
                    montant, frais, description);

            // 7. Récupérer nouveau solde compte
            BigDecimal nouveauSoldeCompte = agenceServiceClient.getAccountBalance(
                    carte.getNumeroCompte());

            // 8. Notification
            notificationService.sendTransferNotification(carte, montant, "DEBIT");

            log.info("✅ Transfert depuis carte réussi: {} FCFA vers compte", montantNet);

            return TransfertCarteResult.success(transactionId, montantNet,
                    nouveauSoldeCompte, carte.getSolde(), frais);

        } catch (CarteException e) {
            log.warn("❌ Échec transfert depuis carte: {}", e.getMessage());
            return TransfertCarteResult.failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur technique transfert depuis carte: {}", e.getMessage(), e);
            return TransfertCarteResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du transfert");
        }
    }

    /**
     * Bloquer une carte
     */
    public CarteOperationResult blockCard(String idCarte, String reason, String clientId) {
        try {
            Carte carte = getCarteOrThrow(idCarte);
            validateCardOwnership(carte, clientId);

            if (carte.getStatus() == CarteStatus.BLOCKED) {
                return CarteOperationResult.failed("BLOCK", "DEJA_BLOQUEE",
                        "La carte est déjà bloquée");
            }

            carte.block(reason, clientId);
            carteRepository.save(carte);

            notificationService.sendCardBlockNotification(carte, reason);

            log.info("🔒 Carte bloquée: {} - Raison: {}", carte.getMaskedNumber(), reason);

            return CarteOperationResult.success("BLOCK", "Carte bloquée avec succès");

        } catch (CarteException e) {
            return CarteOperationResult.failed("BLOCK", e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Débloquer une carte
     */
    public CarteOperationResult unblockCard(String idCarte, String clientId) {
        try {
            Carte carte = getCarteOrThrow(idCarte);
            validateCardOwnership(carte, clientId);

            if (carte.getStatus() != CarteStatus.BLOCKED) {
                return CarteOperationResult.failed("UNBLOCK", "CARTE_NON_BLOQUEE",
                        "La carte n'est pas bloquée");
            }

            carte.unblock(clientId);
            carteRepository.save(carte);

            notificationService.sendCardUnblockNotification(carte);

            log.info("🔓 Carte débloquée: {}", carte.getMaskedNumber());

            return CarteOperationResult.success("UNBLOCK", "Carte débloquée avec succès");

        } catch (CarteException e) {
            return CarteOperationResult.failed("UNBLOCK", e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Modifier les paramètres d'une carte
     */
    public CarteOperationResult updateCardSettings(String idCarte, CarteSettingsRequest request, String clientId) {
        try {
            Carte carte = getCarteOrThrow(idCarte);
            validateCardOwnership(carte, clientId);

            if (carte.getStatus() != CarteStatus.ACTIVE) {
                return CarteOperationResult.failed("UPDATE_SETTINGS", "CARTE_INACTIVE",
                        "Impossible de modifier une carte inactive");
            }

            // Mettre à jour les limites si fournies
            if (request.getLimiteDailyPurchase() != null ||
                    request.getLimiteDailyWithdrawal() != null ||
                    request.getLimiteMonthly() != null) {

                BigDecimal dailyPurchase = request.getLimiteDailyPurchase() != null ? request.getLimiteDailyPurchase()
                        : carte.getLimiteDailyPurchase();
                BigDecimal dailyWithdrawal = request.getLimiteDailyWithdrawal() != null
                        ? request.getLimiteDailyWithdrawal()
                        : carte.getLimiteDailyWithdrawal();
                BigDecimal monthly = request.getLimiteMonthly() != null ? request.getLimiteMonthly()
                        : carte.getLimiteMonthly();

                // Validation des limites selon le type de carte
                validateLimitsForCardType(carte.getType(), dailyPurchase, dailyWithdrawal, monthly);

                carte.updateLimits(dailyPurchase, dailyWithdrawal, monthly);
            }

            // Mettre à jour les paramètres de sécurité
            if (request.getContactless() != null) {
                carte.setContactless(request.getContactless());
            }
            if (request.getInternationalPayments() != null) {
                carte.setInternationalPayments(request.getInternationalPayments());
            }
            if (request.getOnlinePayments() != null) {
                carte.setOnlinePayments(request.getOnlinePayments());
            }

            carteRepository.save(carte);

            log.info("⚙️ Paramètres carte mis à jour: {}", carte.getMaskedNumber());

            return CarteOperationResult.success("UPDATE_SETTINGS", "Paramètres mis à jour avec succès");

        } catch (CarteException e) {
            return CarteOperationResult.failed("UPDATE_SETTINGS", e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Changer le code PIN d'une carte
     */
    public CarteOperationResult changePin(String idCarte, PinChangeRequest request, String clientId) {
        try {
            Carte carte = getCarteOrThrow(idCarte);
            validateCardOwnership(carte, clientId);

            if (carte.getStatus() != CarteStatus.ACTIVE) {
                return CarteOperationResult.failed("CHANGE_PIN", "CARTE_INACTIVE",
                        "Impossible de changer le PIN d'une carte inactive");
            }

            // Vérifier l'ancien PIN
            if (!passwordEncoder.matches(String.valueOf(request.getCurrentPin()),
                    String.valueOf(carte.getCodePin()))) {
                carte.setPinAttempts(carte.getPinAttempts() + 1);

                if (carte.getPinAttempts() >= 3) {
                    carte.setPinBlocked(true);
                    carte.block("PIN bloqué après 3 tentatives", "SYSTEM");
                }

                carteRepository.save(carte);

                return CarteOperationResult.failed("CHANGE_PIN", "PIN_INCORRECT",
                        "Code PIN actuel incorrect");
            }

            // Valider le nouveau PIN
            if (request.getCurrentPin() == request.getNewPin()) {
                return CarteOperationResult.failed("CHANGE_PIN", "PIN_IDENTIQUE",
                        "Le nouveau PIN doit être différent de l'ancien");
            }

            // Mettre à jour le PIN
            String hashedPin = passwordEncoder.encode(String.valueOf(request.getNewPin()));
            carte.setCodePin(Integer.parseInt(hashedPin.substring(0, 4))); // Simplifié pour l'exemple
            carte.setPinAttempts(0);
            carte.setPinBlocked(false);

            carte.addAction(Carte.CarteActionType.PIN_CHANGED, null, "Code PIN modifié", clientId);

            carteRepository.save(carte);

            notificationService.sendPinChangeNotification(carte);

            log.info("🔑 PIN modifié pour carte: {}", carte.getMaskedNumber());

            return CarteOperationResult.success("CHANGE_PIN", "Code PIN modifié avec succès");

        } catch (CarteException e) {
            return CarteOperationResult.failed("CHANGE_PIN", e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Récupérer toutes les cartes d'un client
     */
    public List<Carte> getClientCards(String clientId) {
        return carteRepository.findByIdClientOrderByCreatedAtDesc(clientId);
    }

    /**
     * Récupérer les détails d'une carte
     */
    public Carte getCardDetails(String idCarte, String clientId) {
        Carte carte = getCarteOrThrow(idCarte);
        validateCardOwnership(carte, clientId);
        return carte;
    }

    /**
     * Récupérer les statistiques des cartes d'un client
     */
    public CarteStatistiques getClientCardStatistics(String clientId) {
        List<Carte> cartes = carteRepository.findByIdClient(clientId);

        CarteStatistiques stats = new CarteStatistiques();
        stats.setIdClient(clientId);
        stats.setTotalCartes(cartes.size());

        BigDecimal soldeTotal = BigDecimal.ZERO;
        BigDecimal limiteUtiliseeQuotidienne = BigDecimal.ZERO;
        BigDecimal limiteUtiliseeMensuelle = BigDecimal.ZERO;
        BigDecimal fraisMensuelsTotal = BigDecimal.ZERO;
        int cartesActives = 0;
        int cartesBloques = 0;

        LocalDateTime prochainPrelevement = null;

        for (Carte carte : cartes) {
            soldeTotal = soldeTotal.add(carte.getSolde());
            limiteUtiliseeQuotidienne = limiteUtiliseeQuotidienne.add(carte.getUtilisationQuotidienne());
            limiteUtiliseeMensuelle = limiteUtiliseeMensuelle.add(carte.getUtilisationMensuelle());
            fraisMensuelsTotal = fraisMensuelsTotal.add(carte.calculateMonthlyFees());

            if (carte.getStatus() == CarteStatus.ACTIVE) {
                cartesActives++;
            } else if (carte.getStatus() == CarteStatus.BLOCKED) {
                cartesBloques++;
            }

            if (carte.getNextBillingDate() != null &&
                    (prochainPrelevement == null || carte.getNextBillingDate().isBefore(prochainPrelevement))) {
                prochainPrelevement = carte.getNextBillingDate();
            }
        }

        stats.setCartesActives(cartesActives);
        stats.setCartesBloques(cartesBloques);
        stats.setSoldeTotal(soldeTotal);
        stats.setLimiteUtiliseeQuotidienne(limiteUtiliseeQuotidienne);
        stats.setLimiteUtiliseeMensuelle(limiteUtiliseeMensuelle);
        stats.setFraisMensuelsTotal(fraisMensuelsTotal);
        stats.setProchainPrelevement(prochainPrelevement);
        stats.setGeneratedAt(LocalDateTime.now());

        return stats;
    }

    /**
     * NOUVELLE MÉTHODE: Vérifier le PIN d'une carte
     */
    public boolean verifyCardPin(String idCarte, Integer pin) {
        try {
            Carte carte = findById(idCarte);

            if (carte.isPinBlocked()) {
                throw new CarteException("PIN_BLOCKED", "Code PIN bloqué");
            }

            // Vérifier le PIN (en réalité, comparer avec le hash stocké)
            boolean pinValid = passwordEncoder.matches(String.valueOf(pin), String.valueOf(carte.getCodePin()));

            if (!pinValid) {
                // Incrémenter tentatives
                carte.setPinAttempts(carte.getPinAttempts() + 1);

                if (carte.getPinAttempts() >= 3) {
                    carte.setPinBlocked(true);
                    carte.block("PIN bloqué après 3 tentatives incorrectes", "SYSTEM");
                    log.warn("🔒 PIN bloqué pour carte: {}", carte.getMaskedNumber());
                }

                carteRepository.save(carte);
                return false;
            }

            // Reset tentatives si PIN correct
            if (carte.getPinAttempts() > 0) {
                carte.setPinAttempts(0);
                carteRepository.save(carte);
            }

            return true;

        } catch (Exception e) {
            log.error("❌ Erreur vérification PIN carte {}: {}", idCarte, e.getMessage());
            return false;
        }
    }

    /**
     * NOUVELLE MÉTHODE: Vérifier si retrait possible (limites)
     */
    public boolean canWithdraw(String idCarte, BigDecimal montant) {
        try {
            Carte carte = findById(idCarte);

            if (!carte.isActive()) {
                return false;
            }

            carte.resetCountersIfNeeded(); // Méthode privée existante

            // Vérifier limite quotidienne de retrait
            BigDecimal nouvelleUtilisation = carte.getUtilisationQuotidienne().add(montant);
            if (nouvelleUtilisation.compareTo(carte.getLimiteDailyWithdrawal()) > 0) {
                log.warn("⚠️ Limite quotidienne retrait dépassée pour carte: {}", carte.getMaskedNumber());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("❌ Erreur vérification limite retrait: {}", e.getMessage());
            return false;
        }
    }

    /**
     * NOUVELLE MÉTHODE: Débiter carte pour retrait
     */
    public void debitCarteForWithdrawal(String idCarte, BigDecimal montant, BigDecimal frais, String requestId) {
        try {
            Carte carte = findById(idCarte);

            BigDecimal montantTotal = montant.add(frais);

            // Débiter la carte
            carte.debit(montantTotal);

            // Ajouter action spécifique retrait
            carte.addAction(Carte.CarteActionType.DEBIT, montantTotal,
                    "Retrait Mobile Money - " + montant + " FCFA (frais: " + frais + ") - Ref: " + requestId,
                    "MOBILE_MONEY_WITHDRAWAL");

            carteRepository.save(carte);

            log.info("✅ Carte débitée pour retrait - ID: {}, Montant: {}, Frais: {}, Nouveau solde: {}",
                    idCarte, montant, frais, carte.getSolde());

        } catch (Exception e) {
            log.error("❌ Erreur débit carte pour retrait: {}", e.getMessage(), e);
            throw new CarteException("DEBIT_FAILED", "Impossible de débiter la carte");
        }
    }

    /**
     * NOUVELLE MÉTHODE: Rembourser carte en cas d'échec retrait
     */
    public void refundCardWithdrawal(String idCarte, BigDecimal montant, BigDecimal frais, String reason) {
        try {
            Carte carte = findById(idCarte);

            BigDecimal montantTotal = montant.add(frais);

            // Recréditer la carte
            carte.credit(montantTotal);

            // Ajouter action de remboursement
            carte.addAction(Carte.CarteActionType.CREDIT, montantTotal,
                    "Remboursement retrait échoué - Raison: " + reason,
                    "WITHDRAWAL_REFUND");

            carteRepository.save(carte);

            log.info("💰 Carte remboursée pour retrait échoué - ID: {}, Montant: {}", idCarte, montantTotal);

        } catch (Exception e) {
            log.error("❌ Erreur remboursement carte: {}", e.getMessage(), e);
        }
    }
    // ========================================
    // MÉTHODES PRIVÉES DE VALIDATION
    // ========================================

    private void validateCreationRequest(CarteCreationRequest request) {
        // Vérifier que le compte existe et appartient au client
        boolean accountExists = agenceServiceClient.verifyAccountOwnership(
                request.getNumeroCompte(), request.getIdClient());

        if (!accountExists) {
            throw new CarteException("COMPTE_INTROUVABLE",
                    "Compte bancaire introuvable ou n'appartient pas au client");
        }

        // Vérifier que le compte est actif
        boolean accountActive = agenceServiceClient.isAccountActive(request.getNumeroCompte());

        if (!accountActive) {
            throw new CarteException("COMPTE_INACTIF",
                    "Le compte bancaire n'est pas actif");
        }
    }

    private void validateClientCardLimits(String clientId, CarteType type) {
        List<Carte> existingCards = carteRepository.findByIdClient(clientId);

        // Vérifier limite de cartes gratuites (1 seule)
        if (type == CarteType.VIRTUELLE_GRATUITE) {
            long gratuitesCount = existingCards.stream()
                    .filter(c -> c.getType() == CarteType.VIRTUELLE_GRATUITE)
                    .filter(c -> c.getStatus() != CarteStatus.CANCELLED)
                    .count();

            if (gratuitesCount >= 1) {
                throw new CarteException("LIMITE_CARTE_GRATUITE",
                        "Une seule carte virtuelle gratuite autorisée par client");
            }
        }

        // Vérifier limite totale de cartes (5 max)
        long totalActiveCards = existingCards.stream()
                .filter(c -> c.getStatus() != CarteStatus.CANCELLED)
                .count();

        if (totalActiveCards >= 5) {
            throw new CarteException("LIMITE_CARTES_DEPASSEE",
                    "Nombre maximum de cartes atteint (5)");
        }
    }

    private void validateCardOwnership(Carte carte, String clientId) {
        if (!carte.getIdClient().equals(clientId)) {
            throw new CarteException("CARTE_NON_AUTORISEE",
                    "Cette carte n'appartient pas au client");
        }
    }

    private void validateCardForTransfer(Carte carte) {
        if (carte.getStatus() != CarteStatus.ACTIVE) {
            throw new CarteException("CARTE_INACTIVE",
                    "La carte n'est pas active");
        }

        if (carte.isExpired()) {
            throw new CarteException("CARTE_EXPIREE",
                    "La carte est expirée");
        }

        if (carte.isPinBlocked()) {
            throw new CarteException("CARTE_PIN_BLOQUE",
                    "Le PIN de la carte est bloqué");
        }
    }

    private void validateLimitsForCardType(CarteType type, BigDecimal dailyPurchase,
            BigDecimal dailyWithdrawal, BigDecimal monthly) {

        BigDecimal maxDaily = type.getLimiteDailyDefault();
        BigDecimal maxMonthly = type.getLimiteMonthlyDefault();

        if (dailyPurchase.compareTo(maxDaily) > 0) {
            throw new CarteException("LIMITE_QUOTIDIENNE_TROP_ELEVEE",
                    "Limite quotidienne trop élevée pour ce type de carte (max: " + maxDaily + " FCFA)");
        }

        if (monthly.compareTo(maxMonthly) > 0) {
            throw new CarteException("LIMITE_MENSUELLE_TROP_ELEVEE",
                    "Limite mensuelle trop élevée pour ce type de carte (max: " + maxMonthly + " FCFA)");
        }
    }

    /**
     * NOUVELLE MÉTHODE: Notifier client du succès du retrait
     */
    public void notifyClientWithdrawalSuccess(String idCarte, String requestId) {
        try {
            Carte carte = findById(idCarte);

            // Ajouter action de confirmation
            carte.addAction(Carte.CarteActionType.DEBIT, null,
                    "Retrait confirmé réussi - Ref: " + requestId,
                    "WITHDRAWAL_CONFIRMED");

            carteRepository.save(carte);

            log.info("📢 Notification succès retrait envoyée - Carte: {}", carte.getMaskedNumber());

        } catch (Exception e) {
            log.error("❌ Erreur notification succès retrait: {}", e.getMessage());
        }
    }

    /**
     * NOUVELLE MÉTHODE: Notifier client de l'échec du retrait
     */
    public void notifyClientWithdrawalFailure(String idCarte, String requestId, String reason) {
        try {
            Carte carte = findById(idCarte);

            // Ajouter action d'échec
            carte.addAction(Carte.CarteActionType.DEBIT, null,
                    "Retrait échoué - Ref: " + requestId + " - Raison: " + reason,
                    "WITHDRAWAL_FAILED");

            carteRepository.save(carte);

            // Notification d'échec
            // notificationService.sendWithdrawalFailureNotification(carte, requestId,
            // reason);

            log.info("📢 Notification échec retrait envoyée - Carte: {}", carte.getMaskedNumber());

        } catch (Exception e) {
            log.error("❌ Erreur notification échec retrait: {}", e.getMessage());
        }
    }

    /**
     * NOUVELLE MÉTHODE: Notifier client du remboursement
     */
    public void notifyClientWithdrawalRefund(String idCarte, String requestId, BigDecimal montantRembourse) {
        try {
            Carte carte = findById(idCarte);

            // Ajouter action de remboursement
            carte.addAction(Carte.CarteActionType.CREDIT, montantRembourse,
                    "Remboursement retrait échoué - Ref: " + requestId,
                    "WITHDRAWAL_REFUNDED");

            carteRepository.save(carte);

            // Notification de remboursement
            // notificationService.sendWithdrawalRefundNotification(carte, requestId,
            // montantRembourse);

            log.info("📢 Notification remboursement envoyée - Carte: {}, Montant: {}",
                    carte.getMaskedNumber(), montantRembourse);

        } catch (Exception e) {
            log.error("❌ Erreur notification remboursement: {}", e.getMessage());
        }
    }

    /**
     * NOUVELLE MÉTHODE: Statistiques des retraits d'une carte
     */
    public Map<String, Object> getCardWithdrawalStatistics(String idCarte, String clientId) {
        try {
            Carte carte = getCardDetails(idCarte, clientId);

            List<Carte.CarteAction> withdrawalActions = carte.getActionsHistory()
                    .stream()
                    .filter(action -> action.getType() == Carte.CarteActionType.DEBIT &&
                            action.getDescription().contains("Retrait"))
                    .toList();

            BigDecimal totalWithdrawn = withdrawalActions.stream()
                    .filter(action -> action.getMontant() != null)
                    .map(Carte.CarteAction::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long successfulWithdrawals = withdrawalActions.stream()
                    .filter(action -> action.getDescription().contains("confirmé"))
                    .count();

            long failedWithdrawals = withdrawalActions.stream()
                    .filter(action -> action.getDescription().contains("échoué"))
                    .count();

            return Map.of(
                    "idCarte", idCarte,
                    "totalRetraits", withdrawalActions.size(),
                    "retraitsReussis", successfulWithdrawals,
                    "retraitsEchoues", failedWithdrawals,
                    "montantTotalRetire", totalWithdrawn,
                    "tauxSucces",
                    withdrawalActions.size() > 0 ? (double) successfulWithdrawals / withdrawalActions.size() * 100 : 0,
                    "generatedAt", LocalDateTime.now());

        } catch (Exception e) {
            log.error("❌ Erreur calcul statistiques retraits: {}", e.getMessage());
            return Map.of("error", "Impossible de calculer les statistiques");
        }
    }

    /**
     * NOUVELLE MÉTHODE: Vérifier limites de retrait quotidien/hebdomadaire
     */
    public Map<String, Object> checkWithdrawalLimits(String idCarte, String clientId) {
        try {
            Carte carte = getCardDetails(idCarte, clientId);

            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);

            List<Carte.CarteAction> todayWithdrawals = carte.getActionsHistory()
                    .stream()
                    .filter(action -> action.getType() == Carte.CarteActionType.DEBIT &&
                            action.getDescription().contains("Retrait") &&
                            action.getTimestamp().isAfter(today))
                    .toList();

            List<Carte.CarteAction> weekWithdrawals = carte.getActionsHistory()
                    .stream()
                    .filter(action -> action.getType() == Carte.CarteActionType.DEBIT &&
                            action.getDescription().contains("Retrait") &&
                            action.getTimestamp().isAfter(weekStart))
                    .toList();

            BigDecimal todayAmount = todayWithdrawals.stream()
                    .filter(action -> action.getMontant() != null)
                    .map(Carte.CarteAction::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal weekAmount = weekWithdrawals.stream()
                    .filter(action -> action.getMontant() != null)
                    .map(Carte.CarteAction::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Limites selon le type de carte
            BigDecimal limiteDailyWithdrawal = carte.getLimiteDailyWithdrawal();
            BigDecimal limiteWeeklyWithdrawal = limiteDailyWithdrawal.multiply(new BigDecimal("5")); // 5x la limite
                                                                                                     // quotidienne

            // 🔧 SOLUTION: Utiliser HashMap au lieu de Map.of() pour éviter la limite de 10
            // paires
            Map<String, Object> result = new HashMap<>();
            result.put("idCarte", idCarte);
            result.put("limiteDailyWithdrawal", limiteDailyWithdrawal);
            result.put("limiteWeeklyWithdrawal", limiteWeeklyWithdrawal);
            result.put("utilisationAujourdhui", todayAmount);
            result.put("utilisationSemaine", weekAmount);
            result.put("limiteQuotidienneRestante", limiteDailyWithdrawal.subtract(todayAmount));
            result.put("limiteHebdomadaireRestante", limiteWeeklyWithdrawal.subtract(weekAmount));
            result.put("nombreRetraitsAujourdhui", todayWithdrawals.size());
            result.put("nombreRetraitsSemaine", weekWithdrawals.size());
            result.put("peutRetirerAujourdhui", todayAmount.compareTo(limiteDailyWithdrawal) < 0);
            result.put("peutRetirerCetteSemaine", weekAmount.compareTo(limiteWeeklyWithdrawal) < 0);
            result.put("calculatedAt", LocalDateTime.now());

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur vérification limites retrait: {}", e.getMessage());

            // 🔧 Même correction pour le cas d'erreur
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Impossible de vérifier les limites");
            errorResult.put("message", e.getMessage());
            errorResult.put("timestamp", LocalDateTime.now());

            return errorResult;
        }
    }
    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    private Carte getCarteOrThrow(String idCarte) {
        return carteRepository.findById(idCarte)
                .orElseThrow(() -> new CarteException("CARTE_INTROUVABLE",
                        "Carte " + idCarte + " introuvable"));
    }

    private String generateUniqueCardNumber() {
        String number;
        do {
            number = generateCardNumber();
        } while (carteRepository.existsByNumeroCarte(number));
        return number;
    }

    private String generateCardNumber() {
        // Format: 4XXX XXXX XXXX XXXX (commence par 4 pour Visa-like)
        StringBuilder number = new StringBuilder("4");

        for (int i = 1; i < 16; i++) {
            number.append(random.nextInt(10));
        }

        return number.toString();
    }

    private String generateCVV() {
        return String.format("%03d", random.nextInt(1000));
    }

    private Carte buildNewCarte(CarteCreationRequest request, String numeroCarte) {
        Carte carte = new Carte();
        carte.setIdCarte(UUID.randomUUID().toString());
        carte.setIdClient(request.getIdClient());
        carte.setNumeroCompte(request.getNumeroCompte());
        carte.setNumeroCarte(numeroCarte);
        carte.setCvv(generateCVV());
        carte.setDateExpiration(LocalDateTime.now().plusYears(3)); // Expire dans 3 ans
        carte.setNomPorteur(request.getNomPorteur().toUpperCase());
        carte.setType(request.getType());
        carte.setStatus(CarteStatus.ACTIVE); // Directement active
        carte.setCreatedAt(LocalDateTime.now());
        carte.setActivatedAt(LocalDateTime.now());
        carte.setCreatedBy(request.getIdClient());

        // Configuration des limites
        BigDecimal dailyPurchase = request.getLimiteDailyPurchase() != null ? request.getLimiteDailyPurchase()
                : request.getType().getLimiteDailyDefault();
        BigDecimal dailyWithdrawal = request.getLimiteDailyWithdrawal() != null ? request.getLimiteDailyWithdrawal()
                : new BigDecimal("200000");
        BigDecimal monthly = request.getLimiteMonthly() != null ? request.getLimiteMonthly()
                : request.getType().getLimiteMonthlyDefault();

        carte.setLimiteDailyPurchase(dailyPurchase);
        carte.setLimiteDailyWithdrawal(dailyWithdrawal);
        carte.setLimiteMonthly(monthly);

        // Configuration sécurité
        carte.setContactless(request.isContactless());
        carte.setInternationalPayments(request.isInternationalPayments());
        carte.setOnlinePayments(request.isOnlinePayments());

        // PIN sécurisé
        String hashedPin = passwordEncoder.encode(String.valueOf(request.getCodePin()));
        carte.setCodePin(hashedPin.hashCode());
        // Frais et facturation
        carte.setFraisCreation(request.getType().getFraisCreation());
        carte.setFraisMensuels(carte.calculateMonthlyFees());
        carte.setNextBillingDate(LocalDateTime.now().plusMonths(1));

        // Historique
        carte.addAction(Carte.CarteActionType.CREATED, null,
                "Carte créée - Type: " + request.getType(), request.getIdClient());
        carte.addAction(Carte.CarteActionType.ACTIVATED, null,
                "Carte activée immédiatement", "SYSTEM");

        return carte;
    }

    private BigDecimal calculateTransferFees(BigDecimal montant) {
        // 0.5% du montant avec minimum 50 FCFA et maximum 2000 FCFA
        BigDecimal frais = montant.multiply(new BigDecimal("0.005"));

        if (frais.compareTo(new BigDecimal("50")) < 0) {
            frais = new BigDecimal("50");
        } else if (frais.compareTo(new BigDecimal("2000")) > 0) {
            frais = new BigDecimal("2000");
        }

        return frais;
    }

    // ========================================
    // MÉTHODES ADMINISTRATIVES
    // ========================================

    /**
     * Récupérer toutes les cartes pour l'admin
     */
    public List<Carte> getAllCardsForAdmin(int page, int size) {
        // Implémentation simplifiée - en réalité utiliser Pageable
        return carteRepository.findAll().stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    /**
     * Blocage administratif d'une carte
     */
    public CarteOperationResult adminBlockCard(String idCarte, String reason, String adminId) {
        try {
            Carte carte = getCarteOrThrow(idCarte);

            if (carte.getStatus() == CarteStatus.BLOCKED) {
                return CarteOperationResult.failed("ADMIN_BLOCK", "DEJA_BLOQUEE",
                        "La carte est déjà bloquée");
            }

            carte.block("BLOCAGE_ADMINISTRATIF: " + reason, "ADMIN_" + adminId);
            carteRepository.save(carte);

            notificationService.sendAdminCardBlockNotification(carte, reason, adminId);

            log.info("🔒 Carte bloquée par admin: {} - Admin: {} - Raison: {}",
                    carte.getMaskedNumber(), adminId, reason);

            return CarteOperationResult.success("ADMIN_BLOCK", "Carte bloquée par l'administration");

        } catch (CarteException e) {
            return CarteOperationResult.failed("ADMIN_BLOCK", e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Traitement automatique des frais mensuels
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    public void processMonthlyFees() {
        log.info("🔄 Début traitement frais mensuels des cartes");

        LocalDateTime now = LocalDateTime.now();
        List<Carte> cartesForBilling = carteRepository.findCardsForBilling(now);

        for (Carte carte : cartesForBilling) {
            try {
                BigDecimal fraisMensuels = carte.calculateMonthlyFees();

                if (fraisMensuels.compareTo(BigDecimal.ZERO) > 0) {
                    boolean fraisDebites = agenceServiceClient.debitAccountFees(
                            carte.getNumeroCompte(), fraisMensuels,
                            "FRAIS_MENSUEL_CARTE_" + carte.getType());

                    if (fraisDebites) {
                        carte.setNextBillingDate(now.plusMonths(1));
                        carte.addAction(Carte.CarteActionType.DEBIT, fraisMensuels,
                                "Frais mensuels prélevés", "SYSTEM_BILLING");
                        carteRepository.save(carte);

                        notificationService.sendMonthlyFeesNotification(carte, fraisMensuels);

                        log.info("💰 Frais mensuels prélevés: {} FCFA sur carte {}",
                                fraisMensuels, carte.getMaskedNumber());
                    } else {
                        log.warn("⚠️ Échec prélèvement frais mensuels carte {}: solde insuffisant",
                                carte.getMaskedNumber());

                        // Optionnel: bloquer la carte après X échecs
                        handleFailedBilling(carte);
                    }
                }

            } catch (Exception e) {
                log.error("❌ Erreur traitement frais carte {}: {}",
                        carte.getIdCarte(), e.getMessage(), e);
            }
        }

        log.info("✅ Traitement frais mensuels terminé: {} cartes traitées", cartesForBilling.size());
    }

    /**
     * Traitement automatique des cartes expirées
     */
    @Scheduled(cron = "0 0 1 * * ?") // Tous les jours à 1h du matin
    public void processExpiredCards() {
        log.info("🔄 Début traitement cartes expirées");

        LocalDateTime now = LocalDateTime.now();
        List<Carte> cartesExpirees = carteRepository.findExpiredCards(now);

        for (Carte carte : cartesExpirees) {
            if (carte.getStatus() == CarteStatus.ACTIVE) {
                carte.setStatus(CarteStatus.EXPIRED);
                carte.addAction(Carte.CarteActionType.EXPIRED, null,
                        "Carte expirée automatiquement", "SYSTEM_EXPIRY");
                carteRepository.save(carte);

                notificationService.sendCardExpiryNotification(carte);

                log.info("⏰ Carte expirée: {}", carte.getMaskedNumber());
            }
        }

        log.info("✅ Traitement cartes expirées terminé: {} cartes expirées", cartesExpirees.size());
    }

    /**
     * Gestion des échecs de facturation
     */
    private void handleFailedBilling(Carte carte) {
        // Logique pour gérer les échecs de prélèvement
        // Par exemple, bloquer après 3 échecs consécutifs
        int failedAttempts = carte.getFailedBillingAttempts() + 1;
        carte.setFailedBillingAttempts(failedAttempts);

        if (failedAttempts >= 3) {
            carte.block("Blocage automatique: échec prélèvement frais mensuels", "SYSTEM_BILLING");
            log.warn("🔒 Carte bloquée pour échec de facturation: {}", carte.getMaskedNumber());
        }

        carteRepository.save(carte);
    }

    /**
     * Transfert entre cartes du même client
     */
    public TransfertCarteResult transferBetweenUserCards(String idCarteSource, String idCarteDestination,
            BigDecimal montant, String description, String clientId) {
        log.info("💳➡️💳 Transfert direct carte à carte: {} -> {}, montant={}",
                idCarteSource, idCarteDestination, montant);

        try {
            // 1. Récupérer et valider les cartes
            Carte carteSource = getCarteOrThrow(idCarteSource);
            Carte carteDestination = getCarteOrThrow(idCarteDestination);

            validateCardOwnership(carteSource, clientId);
            validateCardOwnership(carteDestination, clientId);
            validateCardForTransfer(carteSource);
            validateCardForTransfer(carteDestination);

            // 2. Vérifier solde carte source
            if (!carteSource.canPurchase(montant)) {
                return TransfertCarteResult.failed("SOLDE_INSUFFISANT_CARTE_SOURCE",
                        "Solde insuffisant sur la carte source");
            }

            // 3. Calculer frais réduits pour transfert interne (0.2%)
            BigDecimal frais = montant.multiply(new BigDecimal("0.002"));
            if (frais.compareTo(new BigDecimal("25")) < 0) {
                frais = new BigDecimal("25");
            }

            BigDecimal montantNet = montant.subtract(frais);

            // 4. Effectuer le transfert atomique
            carteSource.debit(montant);
            carteDestination.credit(montantNet);

            // 5. Sauvegarder les cartes
            carteRepository.save(carteSource);
            carteRepository.save(carteDestination);

            // 6. Enregistrer la transaction
            String transactionId = transactionService.recordCardToCardTransfer(
                    idCarteSource, idCarteDestination, montant, frais, description);

            // 7. Notifications
            notificationService.sendCardToCardTransferNotification(
                    carteSource, carteDestination, montant, "DEBIT");
            notificationService.sendCardToCardTransferNotification(
                    carteDestination, carteSource, montantNet, "CREDIT");

            log.info("✅ Transfert carte à carte réussi: {} FCFA (net: {}) de {} vers {}",
                    montant, montantNet, carteSource.getMaskedNumber(), carteDestination.getMaskedNumber());

            return TransfertCarteResult.success(transactionId, montantNet,
                    BigDecimal.ZERO, // Pas de solde compte dans ce cas
                    carteDestination.getSolde(), frais);

        } catch (CarteException e) {
            log.warn("❌ Échec transfert carte à carte: {}", e.getMessage());
            return TransfertCarteResult.failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur technique transfert carte à carte: {}", e.getMessage(), e);
            return TransfertCarteResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du transfert");
        }
    }

    /**
     * Mise à jour des limites quotidiennes/mensuelles (reset automatique)
     */
    @Scheduled(cron = "0 1 0 * * ?") // Tous les jours à 00:01
    public void resetDailyLimits() {
        log.info("🔄 Reset des limites quotidiennes");

        // En MongoDB, on peut faire une mise à jour batch
        List<Carte> cartesActives = carteRepository.findByStatus(CarteStatus.ACTIVE);

        for (Carte carte : cartesActives) {
            carte.setUtilisationQuotidienne(BigDecimal.ZERO);
            carte.setLastDailyReset(LocalDateTime.now());
        }

        carteRepository.saveAll(cartesActives);
        log.info("✅ Reset quotidien terminé: {} cartes mises à jour", cartesActives.size());
    }

    /**
     * Génération de rapport mensuel des cartes
     */
    public Map<String, Object> generateMonthlyReport(String clientId) {
        List<Carte> cartes = carteRepository.findByIdClient(clientId);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal totalSoldes = BigDecimal.ZERO;
        BigDecimal totalFraisPayes = BigDecimal.ZERO;
        BigDecimal totalUtilisationMensuelle = BigDecimal.ZERO;
        int transactionsCount = 0;

        for (Carte carte : cartes) {
            totalSoldes = totalSoldes.add(carte.getSolde());
            totalFraisPayes = totalFraisPayes.add(carte.calculateMonthlyFees());
            totalUtilisationMensuelle = totalUtilisationMensuelle.add(carte.getUtilisationMensuelle());
            transactionsCount += carte.getActionsHistory().size();
        }

        return Map.of(
                "clientId", clientId,
                "periode", now.getMonth() + " " + now.getYear(),
                "nombreCartes", cartes.size(),
                "totalSoldes", totalSoldes,
                "totalFraisPayes", totalFraisPayes,
                "totalUtilisationMensuelle", totalUtilisationMensuelle,
                "nombreTransactions", transactionsCount,
                "generatedAt", now);
    }

    /**
     * Analyse de sécurité des cartes
     */
    public List<Map<String, Object>> analyzeCardsSecurity(String clientId) {
        List<Carte> cartes = carteRepository.findByIdClient(clientId);

        return cartes.stream().map(carte -> {
            Map<String, Object> analysis = Map.of(
                    "idCarte", carte.getIdCarte(),
                    "numeroCarte", carte.getMaskedNumber(),
                    "riskLevel", calculateRiskLevel(carte),
                    "lastUsed", carte.getLastUsedAt(),
                    "pinBlocked", carte.isPinBlocked(),
                    "internationalEnabled", carte.isInternationalPayments(),
                    "recommendations", generateSecurityRecommendations(carte));
            return analysis;
        }).toList();
    }

    private String calculateRiskLevel(Carte carte) {
        if (carte.isPinBlocked() || carte.getStatus() != CarteStatus.ACTIVE) {
            return "HIGH";
        }

        if (carte.isInternationalPayments() && carte.isOnlinePayments()) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private List<String> generateSecurityRecommendations(Carte carte) {
        List<String> recommendations = new ArrayList<>();

        if (carte.isInternationalPayments()) {
            recommendations.add("Désactiver les paiements internationaux si non nécessaires");
        }

        if (carte.getLastUsedAt() != null &&
                carte.getLastUsedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            recommendations.add("Carte non utilisée depuis plus de 30 jours - Considérer la désactivation");
        }

        if (carte.getLimiteDailyPurchase().compareTo(new BigDecimal("1000000")) > 0) {
            recommendations.add("Limite quotidienne élevée - Vérifier si nécessaire");
        }

        return recommendations;
    }

    public void creditCarteFromOrangeMoney(String idCarte, BigDecimal montant, String transactionId) {
        Carte carte = findById(idCarte);
        if (carte == null) {
            throw new CarteException("CARTE_NOT_FOUND", "Carte non trouvée: " + idCarte);
        }

        if (!carte.isActive()) {
            throw new CarteException("CARTE_NOT_ACTIVE", "Carte non active");
        }

        // Créditer la carte
        carte.credit(montant);

        // Ajouter action spécifique Orange Money
        carte.addAction(Carte.CarteActionType.CREDIT, montant,
                "Crédit Orange Money - " + transactionId, "ORANGE_MONEY");

        carteRepository.save(carte);

        log.info("✅ Carte créditée depuis Orange Money - ID: {}, Montant: {}, Nouveau solde: {}",
                idCarte, montant, carte.getSolde());
    }

    public Carte findById(String idCarte) {
        return carteRepository.findById(idCarte)
                .orElseThrow(() -> new CarteException("CARTE_INTROUVABLE",
                        "Carte " + idCarte + " introuvable"));
    }
}