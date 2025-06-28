package com.serviceAgence.repository;

import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.enums.DocumentType;
import com.serviceAgence.model.DocumentKYC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des documents KYC avec workflow d'approbation
 */
@Repository
public interface DocumentKYCRepository extends MongoRepository<DocumentKYC, String> {
    
    /**
     * Recherche par client
     */
    List<DocumentKYC> findByIdClient(String idClient);
    
    Optional<DocumentKYC> findByIdClientAndStatus(String idClient, DocumentStatus status);
    
    /**
     * Documents par statut
     */
    Page<DocumentKYC> findByStatusOrderByUploadedAtAsc(DocumentStatus status, Pageable pageable);
    
    Page<DocumentKYC> findByStatusOrderByValidatedAtDesc(DocumentStatus status, Pageable pageable);
    
    /**
     * Documents par statut et agence
     */
    Page<DocumentKYC> findByStatusAndIdAgenceOrderByUploadedAtAsc(
            DocumentStatus status, String idAgence, Pageable pageable);
    
    Page<DocumentKYC> findByStatusAndIdAgenceOrderByValidatedAtDesc(
            DocumentStatus status, String idAgence, Pageable pageable);
    
    /**
     * Documents par agence
     */
    Page<DocumentKYC> findByIdAgenceOrderByValidatedAtDesc(String idAgence, Pageable pageable);
    
    /**
     * Documents avec statuts multiples
     */
    Page<DocumentKYC> findByStatusInOrderByValidatedAtDesc(List<DocumentStatus> statuses, Pageable pageable);
    
    /**
     * Comptage par statut
     */
    long countByStatus(DocumentStatus status);
    
    long countByStatusAndIdAgence(DocumentStatus status, String idAgence);
    
    /**
     * Documents traités après une date
     */
    long countByValidatedAtAfter(LocalDateTime date);
    
    /**
     * Documents anciens (pour nettoyage)
     */
    @Query("{'uploadedAt': {$lt: ?0}, 'status': {$in: ['PENDING', 'RECEIVED']}}")
    List<DocumentKYC> findOldPendingDocuments(LocalDateTime cutoffDate);
    
    /**
     * Documents en cours de review depuis trop longtemps
     */
    @Query("{'status': 'UNDER_REVIEW', 'validatedAt': {$lt: ?0}}")
    List<DocumentKYC> findStuckInReview(LocalDateTime cutoffDate);
    
    /**
     * Recherche par nom/prénom (pour l'admin)
     */
    @Query("{'$or': [" +
           "{'nomExtrait': {$regex: ?0, $options: 'i'}}, " +
           "{'prenomExtrait': {$regex: ?0, $options: 'i'}}, " +
           "{'numeroDocument': {$regex: ?0, $options: 'i'}}" +
           "]}")
    Page<DocumentKYC> findBySearchTerm(String searchTerm, Pageable pageable);
    
    /**
     * Documents par validateur
     */
    List<DocumentKYC> findByValidatedByOrderByValidatedAtDesc(String validatedBy);
    
    /**
     * Statistiques par période
     */
    @Query("{'validatedAt': {$gte: ?0, $lt: ?1}, 'status': ?2}")
    List<DocumentKYC> findByValidatedAtBetweenAndStatus(
            LocalDateTime start, LocalDateTime end, DocumentStatus status);

    List<DocumentKYC> findByIdClientOrderByUploadedAtDesc(String idClient);

    boolean existsByNumeroDocumentAndType(String numeroDocument, DocumentType type);

    /**
     * Comptage des documents avec selfie
     */
    long countByCheminSelfieIsNotNull();

    long countByCheminSelfieIsNull();

    /**
     * Statistiques de similarité faciale
     */
    @Query("{ $group: { _id: null, avgSimilarity: { $avg: '$selfieSimilarityScore' } } }")
    Double findAverageSelfieSimilarityScore();

    /**
     * Documents par score de similarité
     */
    long countBySelfieSimilarityScoreGreaterThanEqual(Integer minScore);

    long countBySelfieSimilarityScoreLessThan(Integer maxScore);

    long countBySelfieSimilarityScoreBetween(Integer minScore, Integer maxScore);

    /**
     * Documents avec détection de vie
     */
    long countByLivenessDetectedTrue();

    long countByLivenessDetectedFalse();

    /**
     * Documents par qualité de selfie
     */
    long countBySelfieQualityScoreGreaterThanEqual(Integer minQuality);

    long countBySelfieQualityScoreLessThan(Integer maxQuality);

    /**
     * Recherche avancée avec critères de selfie
     */
    @Query("{'selfieSimilarityScore': {$gte: ?0}, 'selfieQualityScore': {$gte: ?1}, 'livenessDetected': ?2}")
    List<DocumentKYC> findBySelfieCriteria(Integer minSimilarity, Integer minQuality, Boolean liveness);

    /**
     * Documents avec problèmes de selfie pour review prioritaire
     */
    @Query("{'$or': [" +
        "{'selfieSimilarityScore': {$lt: 50}}, " +
        "{'selfieQualityScore': {$lt: 40}}, " +
        "{'livenessDetected': false}" +
        "], 'status': 'RECEIVED'}")
    List<DocumentKYC> findDocumentsWithSelfieIssues();

    /**
     * Statistiques détaillées par période
     */
    @Query("{'validatedAt': {$gte: ?0, $lt: ?1}, 'cheminSelfie': {$ne: null}}")
    List<DocumentKYC> findDocumentsWithSelfieInPeriod(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(d) FROM DocumentKYC d WHERE d.idAgence = :idAgence")
    long countByIdAgence(@Param("idAgence") String idAgence);

    
}