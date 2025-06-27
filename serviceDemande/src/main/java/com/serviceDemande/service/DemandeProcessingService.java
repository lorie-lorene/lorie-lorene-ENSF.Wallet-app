package com.serviceDemande.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.serviceDemande.config.MessagingConfig;
import com.serviceDemande.dto.*;
import com.serviceDemande.enums.ActionType;
import com.serviceDemande.enums.DemandeStatus;
import com.serviceDemande.enums.RiskLevel;
import com.serviceDemande.model.Demande;
import com.serviceDemande.model.ValidationDetails;
import com.serviceDemande.repository.DemandeRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class DemandeProcessingService {

    @Autowired
    private DemandeRepository demandeRepository;

    @Autowired
    private AntiFraudeService antiFraudeService;

    @Autowired
    private LimitesService limitesService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationService notificationService; 

    /**
     * Réception et traitement des demandes de validation depuis ServiceAgence
     */
    @RabbitListener(queues = "Demande-Queue2")
    public void processValidationRequest(ValidationRequest request) {
        log.info("🔍 Réception demande validation: client={}, agence={}",
                request.getIdClient(), request.getIdAgence());

        try {
            // 1. Créer ou récupérer la demande
            Demande demande = createOrUpdateDemande(request);

            // 2. Validation format basique
            ValidationResult formatValidation = validateFormat(request);
            if (!formatValidation.isValid()) {
                rejectDemande(demande, formatValidation.getErrorCode(), formatValidation.getMessage());
                sendRejectionResponse(request, formatValidation.getErrorCode(), formatValidation.getMessage());
                return;
            }

            // 3. Analyse anti-fraude
            demande.updateStatus(DemandeStatus.ANALYZING, "Début analyse anti-fraude", "SYSTEM");
            FraudAnalysisResult fraudAnalysis = antiFraudeService.analyzeRequest(demande);

            // 4. Mise à jour des données de risque
            updateRiskData(demande, fraudAnalysis);

            // 5. Prise de décision
            processDecision(demande, fraudAnalysis, request);

        } catch (Exception e) {
            log.error("❌ Erreur traitement demande {}: {}", request.getIdClient(), e.getMessage(), e);
            sendRejectionResponse(request, "ERREUR_TECHNIQUE", "Erreur technique lors de l'analyse");
        }
    }

    /**
     * Traitement des demandes de transaction (validation en temps réel)
     */
    @RabbitListener(queues = MessagingConfig.TRANSACTION_VALIDATION_QUEUE)
    public void processTransactionValidation(TransactionValidationRequest request) {
        log.info("💳 Validation transaction: client={}, montant={}, type={}",
                request.getIdClient(), request.getMontant(), request.getType());

        try {
            // Récupérer la demande approuvée du client
            Optional<Demande> demandeOpt = demandeRepository.findByIdClient(request.getIdClient())
                    .stream()
                    .filter(d -> d.getStatus() == DemandeStatus.APPROVED)
                    .findFirst();

            if (demandeOpt.isEmpty()) {
                sendTransactionRejection(request, "CLIENT_NON_APPROVE", "Client non approuvé");
                return;
            }

            Demande demande = demandeOpt.get();

            // Analyse de risque en temps réel pour la transaction
            TransactionRiskResult riskResult = analyzeTransactionRisk(request, demande);

            if (riskResult.isBlocked()) {
                sendTransactionRejection(request, riskResult.getBlockReason(), riskResult.getMessage());

                // Alerter si transaction suspecte
                if (riskResult.isSuspicious()) {
                    notificationService.sendFraudAlert(request.getIdClient(),
                            "TRANSACTION_SUSPECTE", riskResult.getMessage());
                }
            } else {
                sendTransactionApproval(request, riskResult);
            }

        } catch (Exception e) {
            log.error("❌ Erreur validation transaction {}: {}", request.getIdClient(), e.getMessage(), e);
            sendTransactionRejection(request, "ERREUR_TECHNIQUE", "Erreur technique");
        }
    }

    /**
     * Traitement manuel des demandes nécessitant une révision
     */
    public void processManualReview(String demandeId, boolean approved, String reviewerNotes, String reviewerId) {
        log.info("👨‍💼 Révision manuelle: demande={}, approuvé={}, reviewer={}",
                demandeId, approved, reviewerId);

        Optional<Demande> demandeOpt = demandeRepository.findById(demandeId);
        if (demandeOpt.isEmpty()) {
            log.warn("Demande {} introuvable pour révision manuelle", demandeId);
            return;
        }

        Demande demande = demandeOpt.get();
        demande.setAssignedReviewer(reviewerId);
        demande.setReviewerNotes(reviewerNotes);

        if (approved) {
            // Approbation manuelle
            TransactionLimits limits = limitesService.calculateLimits(
                    demande.getRiskScore(), demande.getRiskLevel(), demande.getIdAgence());

            approveDemande(demande, limits, "Approuvé manuellement par " + reviewerId);

            ValidationResponse response = ValidationResponse.approved(
                    demande.getEventId(), demande.getIdClient(), demande.getIdAgence(),
                    demande.getEmail(), limits, demande.getRiskScore());

            sendValidationResponse(response);

        } else {
            // Rejet manuel
            rejectDemande(demande, "REJET_MANUEL", "Rejeté manuellement: " + reviewerNotes);

            ValidationResponse response = ValidationResponse.rejected(
                    demande.getEventId(), demande.getIdClient(), demande.getIdAgence(),
                    demande.getEmail(), "REJET_MANUEL", reviewerNotes);

            sendValidationResponse(response);
        }

        demandeRepository.save(demande);
    }

    private Demande createOrUpdateDemande(ValidationRequest request) {
        Optional<Demande> existingOpt = demandeRepository.findByEventId(request.getEventId());

        if (existingOpt.isPresent()) {
            log.info("Mise à jour demande existante: {}", request.getEventId());
            return existingOpt.get();
        }

        log.info("Création nouvelle demande: {}", request.getEventId());
        Demande demande = new Demande();
        demande.setEventId(request.getEventId());
        demande.setIdClient(request.getIdClient());
        demande.setIdAgence(request.getIdAgence());
        demande.setCni(request.getCni());
        demande.setEmail(request.getEmail());
        demande.setNom(request.getNom());
        demande.setPrenom(request.getPrenom());
        demande.setNumero(request.getNumero());
        demande.setRectoCniHash(request.getRectoCniHash());
        demande.setVersoCniHash(request.getVersoCniHash());
        demande.setAgenceValidation(request.getAgenceValidation());
        demande.setCreatedAt(LocalDateTime.now());
        demande.setExpiresAt(LocalDateTime.now().plusDays(7)); // Expire dans 7 jours

        demande.addAction(ActionType.VALIDATION_REQUESTED,
                "Demande reçue de " + request.getSourceService(), "SYSTEM");

        return demandeRepository.save(demande);
    }

    private ValidationResult validateFormat(ValidationRequest request) {
        // Validation CNI camerounaise
        if (!isValidCameroonianCNI(request.getCni())) {
            return ValidationResult.invalid("FORMAT_CNI_INCORRECT",
                    "Format CNI camerounaise invalide (8-12 chiffres requis)");
        }

        // Validation numéro de téléphone camerounais
        if (!isValidCameroonianPhone(request.getNumero())) {
            return ValidationResult.invalid("FORMAT_NUMERO_INCORRECT",
                    "Format numéro camerounais invalide (6XXXXXXXX)");
        }

        // Validation email
        if (!isValidEmail(request.getEmail())) {
            return ValidationResult.invalid("FORMAT_EMAIL_INCORRECT",
                    "Format email invalide");
        }

        // Validation nom/prénom
        if (request.getNom() == null || request.getNom().trim().length() < 2) {
            return ValidationResult.invalid("NOM_INVALIDE", "Nom requis (minimum 2 caractères)");
        }

        if (request.getPrenom() == null || request.getPrenom().trim().length() < 2) {
            return ValidationResult.invalid("PRENOM_INVALIDE", "Prénom requis (minimum 2 caractères)");
        }

        return ValidationResult.valid();
    }

    private void updateRiskData(Demande demande, FraudAnalysisResult fraudAnalysis) {
        demande.setRiskScore(fraudAnalysis.getRiskScore());
        demande.setRiskLevel(fraudAnalysis.getRiskLevel());
        demande.setFraudFlags(fraudAnalysis.getFraudFlags());
        demande.setRequiresManualReview(fraudAnalysis.isRequiresManualReview());

        demande.addAction(ActionType.FRAUD_ANALYSIS_COMPLETED,
                String.format("Analyse terminée - Score: %d, Niveau: %s",
                        fraudAnalysis.getRiskScore(), fraudAnalysis.getRiskLevel()),
                "ANTIFRAUD_SYSTEM");

        demandeRepository.save(demande);
    }

    private void processDecision(Demande demande, FraudAnalysisResult fraudAnalysis, ValidationRequest request) {
        int riskScore = fraudAnalysis.getRiskScore();

        if (riskScore >= 80 || fraudAnalysis.getFraudFlags().contains("CNI_DEJA_UTILISEE")) {
            // Rejet automatique pour risque critique
            rejectDemande(demande, "RISQUE_CRITIQUE",
                    "Score de risque trop élevé: " + riskScore + " - " + fraudAnalysis.getRecommendation());

            sendRejectionResponse(request, "RISQUE_CRITIQUE", fraudAnalysis.getRecommendation());

        } else if (fraudAnalysis.isRequiresManualReview()) {
            // Révision manuelle requise
            demande.updateStatus(DemandeStatus.MANUAL_REVIEW,
                    "Révision manuelle requise - " + fraudAnalysis.getRecommendation(), "SYSTEM");

            demande.addAction(ActionType.MANUAL_REVIEW_ASSIGNED,
                    "Demande assignée pour révision manuelle", "SYSTEM");

            demandeRepository.save(demande);

            // Notifier les superviseurs
            notificationService.sendManualReviewNotification(demande);

            sendManualReviewResponse(request, fraudAnalysis.getRecommendation());

        } else {
            // Approbation automatique
            TransactionLimits limits = limitesService.calculateLimits(
                    riskScore, fraudAnalysis.getRiskLevel(), demande.getIdAgence());

            approveDemande(demande, limits, "Approuvé automatiquement - " + fraudAnalysis.getRecommendation());

            ValidationResponse response = ValidationResponse.approved(
                    request.getEventId(), request.getIdClient(), request.getIdAgence(),
                    request.getEmail(), limits, riskScore);

            sendValidationResponse(response);
        }
    }

    private void approveDemande(Demande demande, TransactionLimits limits, String reason) {
        demande.updateStatus(DemandeStatus.APPROVED, reason, "SYSTEM");
        demande.setLimiteDailyWithdrawal(limits.getDailyWithdrawal());
        demande.setLimiteDailyTransfer(limits.getDailyTransfer());
        demande.setLimiteMonthlyOperations(limits.getMonthlyOperations());

        demandeRepository.save(demande);

        log.info("✅ Demande approuvée: client={}, limites={}/{}/{}",
                demande.getIdClient(), limits.getDailyWithdrawal(),
                limits.getDailyTransfer(), limits.getMonthlyOperations());
    }

    private void rejectDemande(Demande demande, String errorCode, String reason) {
        demande.updateStatus(DemandeStatus.REJECTED, reason, "SYSTEM");
        demande.setRejectionReason(errorCode + ": " + reason);

        demandeRepository.save(demande);

        log.info("❌ Demande rejetée: client={}, raison={}", demande.getIdClient(), reason);
    }

    // ========================================
    // ENVOI DE RÉPONSES
    // ========================================

    private void sendValidationResponse(ValidationResponse response) {
        try {
            rabbitTemplate.convertAndSend(
                    MessagingConfig.DEMANDE_EXCHANGE,
                    MessagingConfig.VALIDATION_RESPONSE_KEY,
                    response);
            log.info("📤 Réponse validation envoyée: client={}, statut={}",
                    response.getIdClient(), response.getStatut());
        } catch (Exception e) {
            log.error("❌ Erreur envoi réponse validation: {}", e.getMessage(), e);
        }
    }

    private void sendRejectionResponse(ValidationRequest request, String errorCode, String message) {
        ValidationResponse response = ValidationResponse.rejected(
                request.getEventId(), request.getIdClient(), request.getIdAgence(),
                request.getEmail(), errorCode, message);
        sendValidationResponse(response);
    }

    private void sendManualReviewResponse(ValidationRequest request, String reason) {
        ValidationResponse response = ValidationResponse.manualReview(
                request.getEventId(), request.getIdClient(), request.getIdAgence(),
                request.getEmail(), reason);
        sendValidationResponse(response);
    }

    private void sendTransactionApproval(TransactionValidationRequest request, TransactionRiskResult risk) {
        TransactionValidationResponse response = new TransactionValidationResponse();
        response.setEventId(request.getEventId());
        response.setIdClient(request.getIdClient());
        response.setStatut("APPROVED");
        response.setMessage("Transaction autorisée");
        response.setRiskScore(risk.getRiskScore());
        response.setTimestamp(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                MessagingConfig.DEMANDE_EXCHANGE,
                "transaction.validation.response",
                response);
    }

    private void sendTransactionRejection(TransactionValidationRequest request, String errorCode, String message) {
        TransactionValidationResponse response = new TransactionValidationResponse();
        response.setEventId(request.getEventId());
        response.setIdClient(request.getIdClient());
        response.setStatut("REJECTED");
        response.setErrorCode(errorCode);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                MessagingConfig.DEMANDE_EXCHANGE,
                "transaction.validation.response",
                response);
    }

    // ========================================
    // VALIDATION UTILITAIRES
    // ========================================

    private boolean isValidCameroonianCNI(String cni) {
        if (cni == null || cni.trim().isEmpty())
            return false;
        String cleanCni = cni.trim().replaceAll("\\s+", "");
        return cleanCni.matches("\\d{8,12}");
    }

    private boolean isValidCameroonianPhone(String phone) {
        if (phone == null)
            return false;
        return phone.matches("^6[5-9]\\d{7}$");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // ========================================
    // ANALYSE DE RISQUE TRANSACTIONNEL
    // ========================================

    private TransactionRiskResult analyzeTransactionRisk(TransactionValidationRequest request, Demande demande) {
        int riskScore = demande.getRiskScore(); // Score de base du profil
        boolean suspicious = false;
        boolean blocked = false;
        String blockReason = null;
        String message = "Transaction autorisée";

        // Analyse du montant par rapport au profil
        if (request.getMontant().compareTo(demande.getLimiteDailyWithdrawal()) > 0) {
            blocked = true;
            blockReason = "LIMITE_QUOTIDIENNE_DEPASSEE";
            message = "Montant dépasse la limite quotidienne autorisée";
        }

        // Détection de patterns suspects
        if (!blocked && isUnusualTransactionPattern(request, demande)) {
            suspicious = true;
            riskScore += 20;
            message = "Pattern transactionnel inhabituel détecté";
        }

        // Vérification horaires suspects
        if (!blocked && isUnusualTime()) {
            suspicious = true;
            riskScore += 10;
        }

        return new TransactionRiskResult(riskScore, suspicious, blocked, blockReason, message);
    }

    private boolean isUnusualTransactionPattern(TransactionValidationRequest request, Demande demande) {
        // Logique de détection de patterns suspects
        // (montants inhabituels, fréquence élevée, etc.)
        return false; // Simplifié pour l'exemple
    }

    private boolean isUnusualTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour() < 6 || now.getHour() > 22; // Entre 22h et 6h
    }

}
