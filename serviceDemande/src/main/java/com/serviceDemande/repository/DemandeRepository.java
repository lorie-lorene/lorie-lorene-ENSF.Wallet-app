package com.serviceDemande.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import com.serviceDemande.model.Demande;
import com.serviceDemande.enums.DemandeStatus;
import com.serviceDemande.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface DemandeRepository extends MongoRepository<Demande, String> {

    Optional<Demande> findByEventId(String eventId);

    List<Demande> findByIdClient(String idClient);

    List<Demande> findByIdAgence(String idAgence);

    List<Demande> findByStatus(DemandeStatus status);

    List<Demande> findByRiskLevel(RiskLevel riskLevel);

    boolean existsByCni(String cni);

    boolean existsByCniAndStatusNot(String cni, DemandeStatus status);

    boolean existsByEmail(String email);

    // Statistiques et détection de patterns
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    long countByIdAgenceAndCreatedAtAfter(String idAgence, LocalDateTime after);

    @Query("{'requiresManualReview': true, 'status': 'MANUAL_REVIEW'}")
    List<Demande> findPendingManualReviews();

    @Query("{'riskScore': {$gte: ?0}}")
    List<Demande> findByRiskScoreGreaterThanEqual(int minRiskScore);

    @Query("{'status': 'APPROVED', 'approvedAt': {$gte: ?0, $lte: ?1}}")
    List<Demande> findApprovedBetween(LocalDateTime start, LocalDateTime end);

    @Query("{'fraudFlags': {$in: ?0}}")
    List<Demande> findByFraudFlagsContaining(List<String> flags);

    // Supervision
    @Query("{'expiresAt': {$lt: ?0}, 'status': {$in: ['RECEIVED', 'ANALYZING', 'MANUAL_REVIEW']}}")
    List<Demande> findExpiredPendingDemandes(LocalDateTime now);

    // Dashboard stats
    @Query(value = "{'status': ?0}", count = true)
    long countByStatus(DemandeStatus status);

    @Query(value = "{'riskLevel': ?0}", count = true)
    long countByRiskLevel(RiskLevel riskLevel);

    // ✅ POUR RECHERCHES AVANCÉES
    Page<Demande> findByStatus(DemandeStatus status, Pageable pageable);

    Page<Demande> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    Page<Demande> findByIdAgence(String idAgence, Pageable pageable);

    Page<Demande> findByIdClient(String idClient, Pageable pageable);

    // ✅ POUR ANTI-FRAUDE
    // ✅ MAINTENANT DISPONIBLES
    long countByCreatedAtAfter(LocalDateTime after);

    List<Demande> findByCreatedAtAfter(LocalDateTime after);
}