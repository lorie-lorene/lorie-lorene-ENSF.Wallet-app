package com.serviceAgence.services;

import com.serviceAgence.dto.document.*;
import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.exception.AuthenticationException;
import com.serviceAgence.model.DocumentKYC;
import com.serviceAgence.repository.DocumentKYCRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion du workflow d'approbation des documents
 * Permet aux admins de reviewer et approuver/rejeter les documents clients
 */
@Service
@Transactional
@Slf4j
public class DocumentApprovalService {

    @Autowired
    private DocumentKYCRepository documentRepository;

    @Autowired
    private AgenceService agenceService;

    /**
     * Récupération des documents en attente d'approbation
     */
    public Page<PendingDocumentDTO> getPendingDocuments(Pageable pageable, String agenceFilter) {
        log.info("📋 Récupération documents en attente - Page: {}, Agence: {}", 
                pageable.getPageNumber(), agenceFilter);

        Page<DocumentKYC> documents;
        
        if (agenceFilter != null && !agenceFilter.trim().isEmpty()) {
            // Filtrer par agence si spécifié
            documents = documentRepository.findByStatusAndIdAgenceOrderByUploadedAtAsc(
                    DocumentStatus.RECEIVED, agenceFilter, pageable);
        } else {
            // Tous les documents en attente
            documents = documentRepository.findByStatusOrderByUploadedAtAsc(
                    DocumentStatus.RECEIVED, pageable);
        }

        return documents.map(this::convertToPendingDocumentDTO);
    }

    /**
     * Récupération des détails complets d'un document pour review
     */
    public DocumentReviewDTO getDocumentForReview(String documentId) {
        log.info("🔍 Récupération document pour review: {}", documentId);

        DocumentKYC document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AuthenticationException("Document introuvable: " + documentId));

        if (document.getStatus() != DocumentStatus.RECEIVED) {
            throw new AuthenticationException("Document non disponible pour review. Statut: " + document.getStatus());
        }

        // Marquer comme en cours d'examen
        document.setStatus(DocumentStatus.UNDER_REVIEW);
        document.setValidatedAt(LocalDateTime.now());
        documentRepository.save(document);

        return convertToDocumentReviewDTO(document);
    }

    /**
     * Approbation d'un document par l'admin
     */
    public DocumentApprovalResult approveDocument(String documentId, DocumentApprovalRequest request, String approvedBy) {
        log.info("✅ Approbation document: {} par {}", documentId, approvedBy);

        DocumentKYC document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AuthenticationException("Document introuvable: " + documentId));

        if (document.getStatus() != DocumentStatus.UNDER_REVIEW) {
            throw new AuthenticationException("Document non en cours d'examen. Statut: " + document.getStatus());
        }

        // Approuver le document
        document.setStatus(DocumentStatus.APPROVED);
        document.setValidatedAt(LocalDateTime.now());
        document.setValidatedBy(approvedBy);
        document.setRejectionReason(null); // Clear any previous rejection reason

        // Ajouter notes d'approbation si fournies
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            document.setValidationNotes(request.getNotes());
        }

        documentRepository.save(document);

        // Déclencher la création du compte maintenant que le document est approuvé
        try {
            triggerAccountCreation(document);
            log.info("✅ Document approuvé et compte déclenché: {}", documentId);
            
            return DocumentApprovalResult.builder()
                    .documentId(documentId)
                    .clientId(document.getIdClient())
                    .status(DocumentStatus.APPROVED)
                    .message("Document approuvé avec succès. Création de compte initiée.")
                    .approvedBy(approvedBy)
                    .approvedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Erreur création compte après approbation: {}", e.getMessage());
            
            // Revertir le statut en cas d'échec
            document.setStatus(DocumentStatus.UNDER_REVIEW);
            documentRepository.save(document);
            
            throw new AuthenticationException("Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    /**
     * Rejet d'un document par l'admin
     */
    public DocumentApprovalResult rejectDocument(String documentId, DocumentRejectionRequest request, String rejectedBy) {
        log.info("❌ Rejet document: {} par {} - Raison: {}", documentId, rejectedBy, request.getReason());

        DocumentKYC document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AuthenticationException("Document introuvable: " + documentId));

        if (document.getStatus() != DocumentStatus.UNDER_REVIEW) {
            throw new AuthenticationException("Document non en cours d'examen. Statut: " + document.getStatus());
        }

        // Rejeter le document
        document.setStatus(DocumentStatus.REJECTED);
        document.setValidatedAt(LocalDateTime.now());
        document.setValidatedBy(rejectedBy);
        document.setRejectionReason(request.getReason());

        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            document.setValidationNotes(request.getNotes());
        }

        documentRepository.save(document);

        // Notifier le UserService du rejet
        notifyUserServiceOfRejection(document, request.getReason());

        log.info("❌ Document rejeté: {}", documentId);
        
        return DocumentApprovalResult.builder()
                .documentId(documentId)
                .clientId(document.getIdClient())
                .status(DocumentStatus.REJECTED)
                .message("Document rejeté. Client notifié.")
                .rejectionReason(request.getReason())
                .rejectedBy(rejectedBy)
                .rejectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Retour d'un document en attente (annuler review)
     */
    public void returnDocumentToPending(String documentId, String returnedBy) {
        log.info("↩️ Retour document en attente: {} par {}", documentId, returnedBy);

        DocumentKYC document = documentRepository.findById(documentId)
                .orElseThrow(() -> new AuthenticationException("Document introuvable: " + documentId));

        if (document.getStatus() != DocumentStatus.UNDER_REVIEW) {
            throw new AuthenticationException("Document non en cours d'examen");
        }

        document.setStatus(DocumentStatus.RECEIVED);
        document.setValidatedAt(null);
        document.setValidatedBy(null);
        documentRepository.save(document);

        log.info("↩️ Document remis en attente: {}", documentId);
    }

    /**
     * Historique des approbations/rejets
     */
    public Page<DocumentHistoryDTO> getDocumentHistory(Pageable pageable, DocumentStatus statusFilter, String agenceFilter) {
        log.info("📚 Récupération historique documents");

        Page<DocumentKYC> documents;

        if (statusFilter != null && agenceFilter != null) {
            documents = documentRepository.findByStatusAndIdAgenceOrderByValidatedAtDesc(
                    statusFilter, agenceFilter, pageable);
        } else if (statusFilter != null) {
            documents = documentRepository.findByStatusOrderByValidatedAtDesc(statusFilter, pageable);
        } else if (agenceFilter != null) {
            documents = documentRepository.findByIdAgenceOrderByValidatedAtDesc(agenceFilter, pageable);
        } else {
            documents = documentRepository.findByStatusInOrderByValidatedAtDesc(
                    List.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED), pageable);
        }

        return documents.map(this::convertToDocumentHistoryDTO);
    }

    /**
     * Statistiques des documents avec métriques de selfie
     */
    public DocumentStatisticsDTO getDocumentStatistics() {
        log.info("📊 Génération statistiques documents avec selfie");

        // Statistiques de base
        long totalDocuments = documentRepository.count();
        long pendingDocuments = documentRepository.countByStatus(DocumentStatus.RECEIVED);
        long underReviewDocuments = documentRepository.countByStatus(DocumentStatus.UNDER_REVIEW);
        long approvedDocuments = documentRepository.countByStatus(DocumentStatus.APPROVED);
        long rejectedDocuments = documentRepository.countByStatus(DocumentStatus.REJECTED);

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long processedToday = documentRepository.countByValidatedAtAfter(startOfDay);

        // ← NEW: Statistiques spécifiques au selfie
        long documentsWithSelfie = documentRepository.countByCheminSelfieIsNotNull();
        long documentsWithoutSelfie = totalDocuments - documentsWithSelfie;
        
        double selfieComplianceRate = totalDocuments > 0 ? 
            (double) documentsWithSelfie / totalDocuments * 100 : 0.0;
        
        // Calcul de la similarité faciale moyenne
        Double averageFacialSimilarity = documentRepository.findAverageSelfieSimilarityScore();
        if (averageFacialSimilarity == null) averageFacialSimilarity = 0.0;
        
        long highSimilarityDocuments = documentRepository.countBySelfieSimilarityScoreGreaterThanEqual(80);
        long lowSimilarityDocuments = documentRepository.countBySelfieSimilarityScoreLessThan(50);
        long livenessDetectedDocuments = documentRepository.countByLivenessDetectedTrue();

        return DocumentStatisticsDTO.builder()
                .totalDocuments(totalDocuments)
                .pendingDocuments(pendingDocuments)
                .underReviewDocuments(underReviewDocuments)
                .approvedDocuments(approvedDocuments)
                .rejectedDocuments(rejectedDocuments)
                .processedToday(processedToday)
                .approvalRate(totalDocuments > 0 ? (double) approvedDocuments / totalDocuments * 100 : 0.0)
                
                // ← NEW: Métriques selfie
                .documentsWithSelfie(documentsWithSelfie)
                .documentsWithoutSelfie(documentsWithoutSelfie)
                .selfieComplianceRate(selfieComplianceRate)
                .averageFacialSimilarity(averageFacialSimilarity)
                .highSimilarityDocuments(highSimilarityDocuments)
                .lowSimilarityDocuments(lowSimilarityDocuments)
                .livenessDetectedDocuments(livenessDetectedDocuments)
                
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // MÉTHODES PRIVÉES
    // ==========================================

    /**
     * Déclencher la création de compte après approbation
     */
    private void triggerAccountCreation(DocumentKYC document) {
        // Récupérer les informations de la demande originale depuis le document
        // et déclencher la création du compte via AgenceService
        
        try {
            // Cette méthode sera appelée pour créer le compte bancaire
            // après que les documents ont été manuellement approuvés
            agenceService.createAccountAfterDocumentApproval(
                document.getIdClient(),
                document.getIdAgence()
            );
        } catch (Exception e) {
            log.error("Erreur création compte pour client {}: {}", document.getIdClient(), e.getMessage());
            throw e;
        }
    }

    /**
     * Notifier UserService du rejet
     */
    private void notifyUserServiceOfRejection(DocumentKYC document, String reason) {
        // Envoyer notification de rejet via RabbitMQ
        try {
            // Cette méthode sera implémentée pour notifier le UserService
            agenceService.notifyUserServiceOfRejection(
                document.getIdClient(),
                document.getIdAgence(),
                reason
            );
        } catch (Exception e) {
            log.error("Erreur notification rejet pour client {}: {}", document.getIdClient(), e.getMessage());
            // Ne pas faire échouer le rejet si la notification échoue
        }
    }

    /**
     * Conversion vers DTO de document en attente
     */
    private PendingDocumentDTO convertToPendingDocumentDTO(DocumentKYC document) {
        return PendingDocumentDTO.builder()
                .id(document.getId())
                .idClient(document.getIdClient())
                .idAgence(document.getIdAgence())
                .nomClient(document.getNomExtrait())
                .prenomClient(document.getPrenomExtrait())
                .cni(document.getNumeroDocument())
                .uploadedAt(document.getUploadedAt())
                .qualityScore(document.getScoreQualite())
                .waitingTime(java.time.Duration.between(document.getUploadedAt(), LocalDateTime.now()))
                .priority(calculatePriority(document))
                .build();
    }

    /**
     * Conversion vers DTO d'historique
     */
    private DocumentHistoryDTO convertToDocumentHistoryDTO(DocumentKYC document) {
        return DocumentHistoryDTO.builder()
                .id(document.getId())
                .idClient(document.getIdClient())
                .idAgence(document.getIdAgence())
                .nomClient(document.getNomExtrait())
                .prenomClient(document.getPrenomExtrait())
                .cni(document.getNumeroDocument())
                .status(document.getStatus())
                .validatedAt(document.getValidatedAt())
                .validatedBy(document.getValidatedBy())
                .rejectionReason(document.getRejectionReason())
                .validationNotes(document.getValidationNotes())
                .processingTime(document.getValidatedAt() != null ? 
                    java.time.Duration.between(document.getUploadedAt(), document.getValidatedAt()) : null)
                .build();
    }

    /**
     * Calcul de priorité basé sur le temps d'attente et la qualité
     */
    private String calculatePriority(DocumentKYC document) {
        long hoursWaiting = java.time.Duration.between(document.getUploadedAt(), LocalDateTime.now()).toHours();
        
        if (hoursWaiting > 48) return "URGENT";
        if (hoursWaiting > 24) return "HIGH";
        if (document.getScoreQualite() != null && document.getScoreQualite() < 60) return "LOW_QUALITY";
        return "NORMAL";
    }

    /**
     * Chargement d'image en Base64 (implémentation sécurisée)
     */
    private String loadImageAsBase64(String imagePath) {
        // TODO: Implémenter le chargement sécurisé des images
        // depuis le stockage (filesystem, S3, etc.)
        try {
            // Placeholder - à remplacer par votre implémentation de stockage
            return "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD..."; 
        } catch (Exception e) {
            log.error("Erreur chargement image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Conversion vers DTO de review détaillé avec selfie
     */
    private DocumentReviewDTO convertToDocumentReviewDTO(DocumentKYC document) {
        return DocumentReviewDTO.builder()
                .id(document.getId())
                .idClient(document.getIdClient())
                .idAgence(document.getIdAgence())
                .cni(document.getNumeroDocument())
                .nomExtrait(document.getNomExtrait())
                .prenomExtrait(document.getPrenomExtrait())
                .dateNaissanceExtraite(document.getDateNaissanceExtraite())
                .lieuNaissanceExtrait(document.getLieuNaissanceExtrait())
                
                // Images pour review (Base64)
                .rectoImageBase64(loadImageAsBase64(document.getCheminRecto()))
                .versoImageBase64(loadImageAsBase64(document.getCheminVerso()))
                .selfieImageBase64(loadImageAsBase64(document.getCheminSelfie())) // ← NEW
                
                // Scores de qualité et similarité
                .qualityScore(document.getScoreQualite())
                .selfieQualityScore(document.getSelfieQualityScore())      // ← NEW
                .selfieSimilarityScore(document.getSelfieSimilarityScore()) // ← NEW
                .livenessDetected(document.getLivenessDetected())          // ← NEW
                
                // Anomalies
                .anomaliesDetectees(document.getAnomaliesDetectees())
                .selfieAnomalies(extractSelfieAnomalies(document.getAnomaliesDetectees())) // ← NEW
                
                // Métadonnées
                .uploadedAt(document.getUploadedAt())
                .uploadedFrom(document.getUploadedFrom())
                .fileSize(document.getFileSize())
                .selfieFileSize(document.getSelfieFileSize()) // ← NEW
                .status(document.getStatus())
                
                // Recommandation faciale pour l'admin
                .facialVerificationRecommendation(generateFacialRecommendationForAdmin(document)) // ← NEW
                .build();
    }

    /**
     * Extraction des anomalies spécifiques au selfie
     */
    private List<String> extractSelfieAnomalies(List<String> allAnomalies) {
        if (allAnomalies == null) return new ArrayList<>();
        
        return allAnomalies.stream()
                .filter(anomaly -> anomaly.contains("SELFIE") || 
                                anomaly.contains("LIVENESS") || 
                                anomaly.contains("SIMILARITY"))
                .collect(Collectors.toList());
    }

    /**
     * Génération de recommandation faciale pour l'admin
     */
    private String generateFacialRecommendationForAdmin(DocumentKYC document) {
        StringBuilder recommendation = new StringBuilder();
        
        // Analyse de la qualité du selfie
        Integer selfieQuality = document.getSelfieQualityScore();
        if (selfieQuality != null) {
            if (selfieQuality >= 80) {
                recommendation.append("✅ Selfie de bonne qualité (").append(selfieQuality).append("%) ");
            } else if (selfieQuality >= 60) {
                recommendation.append("⚠️ Qualité selfie modérée (").append(selfieQuality).append("%) ");
            } else {
                recommendation.append("❌ Qualité selfie insuffisante (").append(selfieQuality).append("%) ");
            }
        }
        
        // Analyse de la similarité faciale
        Integer similarity = document.getSelfieSimilarityScore();
        if (similarity != null) {
            if (similarity >= 80) {
                recommendation.append("- 🎯 Forte correspondance faciale (").append(similarity).append("%) ");
            } else if (similarity >= 60) {
                recommendation.append("- 🔍 Correspondance modérée (").append(similarity).append("%) ");
            } else if (similarity >= 40) {
                recommendation.append("- ⚠️ Correspondance faible (").append(similarity).append("%) ");
            } else {
                recommendation.append("- ❌ Très peu de correspondance (").append(similarity).append("%) ");
            }
        }
        
        // Détection de vie
        Boolean liveness = document.getLivenessDetected();
        if (liveness != null) {
            if (liveness) {
                recommendation.append("- ✅ Détection de vie positive ");
            } else {
                recommendation.append("- ❌ Possible photo d'écran ");
            }
        }
        
        // Recommandation finale
        if (selfieQuality != null && similarity != null && liveness != null) {
            if (selfieQuality >= 70 && similarity >= 70 && liveness) {
                recommendation.append("\n🟢 RECOMMANDATION: Approbation conseillée");
            } else if (selfieQuality >= 50 && similarity >= 50) {
                recommendation.append("\n🟡 RECOMMANDATION: Vérification manuelle approfondie");
            } else {
                recommendation.append("\n🔴 RECOMMANDATION: Prudence maximale ou rejet");
            }
        }
        
        return recommendation.toString();
    }
}