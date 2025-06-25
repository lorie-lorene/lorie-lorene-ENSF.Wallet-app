package com.serviceAgence.services;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceAgence.dto.*;
import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.exception.AgenceException;
import com.serviceAgence.messaging.AgenceEventPublisher;
import com.serviceAgence.model.Agence;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.model.DocumentKYC;
import com.serviceAgence.repository.AgenceRepository;
import com.serviceAgence.repository.DocumentKYCRepository;

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

    @Autowired
    private AgenceEventPublisher eventPublisher;

    @Autowired
    private FacialVerificationService facialVerificationService;

    @Autowired
    private DocumentKYCRepository documentRepository;


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
            KYCValidationResult kycResult = kycService.validateDocumentsWithSelfie(
                request.getIdClient(), 
                request.getCni(),
                request.getRectoCni(),
                request.getVersoCni(),
                request.getSelfieImage()
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

    /**
     * Création de compte après approbation manuelle des documents
     */
    public void createAccountAfterDocumentApproval(String idClient, String idAgence) {
        log.info("🏦 Création compte après approbation manuelle: client={}, agence={}", idClient, idAgence);

        try {
            // Créer le compte bancaire
            AccountCreationRequest accountRequest = new AccountCreationRequest();
            accountRequest.setIdClient(idClient);
            accountRequest.setIdAgence(idAgence);

            AccountCreationResult accountResult = compteService.createAccount(accountRequest);
            
            if (!accountResult.isSuccess()) {
                throw new AgenceException(accountResult.getErrorCode(), accountResult.getMessage());
            }

            // Récupérer le compte créé
            CompteUser compte = compteService.getAccountDetails(accountResult.getNumeroCompte().toString());
            
            // Envoyer notification de succès au UserService
            notificationService.sendAccountCreationNotification(compte);
            
            // Envoyer réponse d'acceptation au UserService via RabbitMQ
            eventPublisher.sendRegistrationResponse(idClient, idAgence, 
                null, // email sera récupéré depuis le document
                RegistrationProcessingResult.accepted(accountResult.getNumeroCompte(), 
                    "Compte créé avec succès après approbation manuelle"));

            log.info("✅ Compte créé avec succès après approbation: client={}, compte={}", 
                    idClient, accountResult.getNumeroCompte());

        } catch (Exception e) {
            log.error("❌ Erreur création compte après approbation: {}", e.getMessage(), e);
            throw new AgenceException("CREATION_COMPTE_FAILED", 
                "Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    /**
     * Notification du UserService en cas de rejet
     */
    public void notifyUserServiceOfRejection(String idClient, String idAgence, String reason) {
        log.info("📢 Notification rejet vers UserService: client={}, raison={}", idClient, reason);

        try {
            // Envoyer réponse de rejet au UserService via RabbitMQ
            eventPublisher.sendRegistrationResponse(idClient, idAgence, 
                null, // email sera récupéré
                RegistrationProcessingResult.rejected("DOCUMENTS_REJECTED", 
                    "Documents rejetés: " + reason));

            log.info("📤 Notification rejet envoyée vers UserService: client={}", idClient);

        } catch (Exception e) {
            log.error("❌ Erreur notification rejet: {}", e.getMessage(), e);
            // Ne pas faire échouer le rejet si la notification échoue
        }
    }

    /**
     * Stockage sécurisé des images
     */
    private String storeImageSecurely(byte[] imageData, String type, String clientId) {
        try {
            // TODO: Implémenter stockage sécurisé (filesystem, S3, etc.)
            // Pour l'instant, retourner un chemin fictif
            String fileName = String.format("%s_%s_%s_%d.jpg", 
                clientId, type, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), 
                System.currentTimeMillis());
            
            String storagePath = "/secure/documents/" + fileName;
            
            // Ici vous devriez implémenter le stockage réel
            // Files.write(Paths.get(storagePath), imageData);
            
            log.debug("📁 Image stockée: {}", storagePath);
            return storagePath;
            
        } catch (Exception e) {
            log.error("❌ Erreur stockage image: {}", e.getMessage());
            throw new AgenceException("IMAGE_STORAGE_FAILED", "Erreur stockage image");
        }
    }
    /**
     * Traitement avec selfie et approbation manuelle
     */
    public RegistrationProcessingResult processRegistrationRequestWithManualApproval(UserRegistrationRequest request) {
        log.info("🔄 Traitement demande avec selfie: client={}, agence={}", 
                request.getIdClient(), request.getIdAgence());

        try {
            // 1. Validation de l'agence
            Agence agence = getAgenceOrThrow(request.getIdAgence());
            if (!agence.isActive()) {
                return RegistrationProcessingResult.rejected("AGENCE_INACTIVE", 
                    "L'agence sélectionnée n'est pas active");
            }

            // 2. Validation de base des documents (format, taille, etc.)
            KYCValidationResult basicValidation = kycService.validateDocumentsBasic(
                request.getIdClient(), 
                request.getCni(),
                request.getRectoCni(),
                request.getVersoCni()
            );

            if (!basicValidation.isValid()) {
                log.warn("Validation de base échouée pour {}: {}", request.getIdClient(), basicValidation.getReason());
                return RegistrationProcessingResult.rejected(basicValidation.getErrorCode(), basicValidation.getReason());
            }

            // 3. Validation et analyse du selfie
            if (!request.hasSelfie()) {
                log.warn("Selfie manquant pour client: {}", request.getIdClient());
                return RegistrationProcessingResult.rejected("SELFIE_REQUIRED", 
                    "Selfie utilisateur obligatoire pour la vérification d'identité");
            }

            // 4. Analyse faciale du selfie
            SelfieAnalysisResult selfieAnalysis = facialVerificationService.analyzeSelfie(
                request.getSelfieImage(), 
                request.getRectoCni()
            );

            log.info("📸 Analyse selfie - Qualité: {}, Similarité: {}, Vie: {}", 
                    selfieAnalysis.getQualityScore(), 
                    selfieAnalysis.getSimilarityScore(), 
                    selfieAnalysis.isLivenessDetected());

            // 5. Créer document avec toutes les informations (CNI + Selfie)
            DocumentKYC document = createDocumentWithSelfie(request, basicValidation, selfieAnalysis);
            
            documentRepository.save(document);

            log.info("📄 Document avec selfie créé en attente d'approbation: client={}", request.getIdClient());

            // 6. Retourner résultat "en attente d'approbation manuelle"
            return RegistrationProcessingResult.pendingManualApproval(request.getIdClient(), 
                "Documents et selfie reçus. En attente d'approbation manuelle par l'agence.");

        } catch (Exception e) {
            log.error("Erreur traitement demande avec selfie: {}", e.getMessage(), e);
            return RegistrationProcessingResult.rejected("ERREUR_TECHNIQUE", 
                "Erreur technique lors du traitement avec selfie");
        }
    }

    /**
     * Création du document KYC avec selfie
     */
    private DocumentKYC createDocumentWithSelfie(UserRegistrationRequest request, 
                                            KYCValidationResult basicValidation,
                                            SelfieAnalysisResult selfieAnalysis) {
        DocumentKYC document = new DocumentKYC();
        
        // Informations de base
        document.setIdClient(request.getIdClient());
        document.setIdAgence(request.getIdAgence());
        document.setNumeroDocument(request.getCni());
        document.setStatus(DocumentStatus.RECEIVED);
        document.setUploadedAt(LocalDateTime.now());
        
        // Informations extraites (placeholder - à implémenter avec OCR)
        document.setNomExtrait(request.getNom());
        document.setPrenomExtrait(request.getPrenom());
        
        // Stockage sécurisé des images
        document.setCheminRecto(storeImageSecurely(request.getRectoCni(), "recto", request.getIdClient()));
        document.setCheminVerso(storeImageSecurely(request.getVersoCni(), "verso", request.getIdClient()));
        document.setCheminSelfie(storeImageSecurely(request.getSelfieImage(), "selfie", request.getIdClient()));
        
        // Scores de qualité
        document.setScoreQualite(basicValidation.getQualityScore());
        document.setSelfieQualityScore(selfieAnalysis.getQualityScore());
        document.setSelfieSimilarityScore(selfieAnalysis.getSimilarityScore());
        document.setLivenessDetected(selfieAnalysis.isLivenessDetected());
        
        // Métadonnées des fichiers
        document.setFileSize((long) request.getRectoCni().length);
        document.setSelfieFileSize(request.getSelfieSize());
        
        // Hash pour intégrité
        document.setHashRecto(calculateHash(request.getRectoCni()));
        document.setHashVerso(calculateHash(request.getVersoCni()));
        document.setHashSelfie(calculateHash(request.getSelfieImage()));
        
        // Anomalies détectées
        List<String> allAnomalies = new ArrayList<>();
        if (basicValidation.getAnomalies() != null) {
            allAnomalies.addAll(basicValidation.getAnomalies());
        }
        if (selfieAnalysis.getAnomalies() != null) {
            allAnomalies.addAll(selfieAnalysis.getAnomalies());
        }
        document.setAnomaliesDetectees(allAnomalies);
        
        return document;
    }

    /**
     * Calcul de hash pour intégrité des images
     */
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Erreur calcul hash: {}", e.getMessage());
            return null;
        }
    }
}
