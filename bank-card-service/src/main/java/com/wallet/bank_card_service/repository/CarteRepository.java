package com.wallet.bank_card_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.wallet.bank_card_service.dto.CarteStatus;
import com.wallet.bank_card_service.dto.CarteType;
import com.wallet.bank_card_service.model.Carte;

@Repository
public interface CarteRepository extends MongoRepository<Carte, String> {
    /**
     * Recherche des cartes par client
     */
    List<Carte> findByIdClient(String idClient);

    List<Carte> findByIdClientOrderByCreatedAtDesc(String idClient);

    /**
     * Recherche par numéro de carte
     */
    Optional<Carte> findByNumeroCarte(String numeroCarte);

    boolean existsByNumeroCarte(String numeroCarte);

    /**
     * Recherche par statut
     */
    List<Carte> findByStatus(CarteStatus status);

    List<Carte> findByIdClientAndStatus(String idClient, CarteStatus status);

    /**
     * Recherche par type
     */
    List<Carte> findByType(CarteType type);

    List<Carte> findByIdClientAndType(String idClient, CarteType type);

    /**
     * Cartes expirées
     */
    @Query("{'dateExpiration': {$lt: ?0}}")
    List<Carte> findExpiredCards(LocalDateTime now);

    /**
     * Cartes nécessitant facturation
     */
    @Query("{'nextBillingDate': {$lt: ?0}, 'status': 'ACTIVE'}")
    List<Carte> findCardsForBilling(LocalDateTime now);

    /**
     * Statistiques par client
     */
    long countByIdClient(String idClient);

    long countByIdClientAndStatus(String idClient, CarteStatus status);

    long countByIdClientAndType(String idClient, CarteType type);

    /**
     * Recherche par agence
     */
    List<Carte> findByIdAgence(String idAgence);

    /**
     * Cartes avec PIN bloqué
     */
    @Query("{'pinBlocked': true}")
    List<Carte> findCardsWithBlockedPin();

    /**
     * Recherche avancée avec pagination
     */
    Page<Carte> findByIdClientAndStatusIn(String idClient, List<CarteStatus> statuses, Pageable pageable);

    /**
     * Cartes inactives depuis X jours
     */
    @Query("{'lastUsedAt': {$lt: ?0}, 'status': 'ACTIVE'}")
    List<Carte> findInactiveCards(LocalDateTime cutoffDate);
}