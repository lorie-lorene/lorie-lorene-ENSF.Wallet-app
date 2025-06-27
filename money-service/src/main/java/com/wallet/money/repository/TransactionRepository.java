package com.wallet.money.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wallet.money.entity.Transaction;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    // List<Transaction> findByStatus(String status);

    // // Trouver par référence FreemoPay
    // Optional<Transaction> findByFreemoReference(String freemoReference);

    @Query("{'externalId': ?0}")
    Optional<Transaction> findByExternalId(String externalId);

    // Transactions d'un client avec un statut donné
    List<Transaction> findByClientIdAndStatus(String clientId, String status);

    // Transactions en attente d'un client
    @Query("{ 'clientId': ?0, 'status': 'PENDING' }")
    List<Transaction> findPendingTransactionsByClient(String clientId);

    // Compter les transactions réussies d'un client
    @Query(value = "{ 'clientId': ?0, 'status': 'SUCCESS' }", count = true)
    long countSuccessfulTransactionsByClient(String clientId);

    // Transactions expirées (pour nettoyage)
    @Query("{ 'status': 'PENDING', 'expiredAt': { $lt: ?0 } }")
    List<Transaction> findExpiredTransactions(java.time.LocalDateTime now);

    Optional<Transaction> findByExternalIdAndClientId(String externalId, String clientId);

    List<Transaction> findByClientIdAndTypeAndIdCarteOrderByCreatedAtDesc(String clientId, String type, String idCarte);

    @Query("{'clientId': ?0, 'type': 'CARD_WITHDRAWAL', 'createdAt': {$gte: ?1}}")
    List<Transaction> findCardWithdrawalsByClientAndDate(String clientId, LocalDateTime startDate);

    @Query("{'type': 'CARD_WITHDRAWAL', 'status': 'PENDING', 'createdAt': {$lt: ?0}}")
    List<Transaction> findExpiredCardWithdrawals(LocalDateTime expiredBefore);

    /**
     * Trouve les transactions en statut PENDING plus anciennes que la date donnée
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :cutoffDate")
    List<Transaction> findPendingTransactionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Trouve les transactions par statut
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = :status")
    List<Transaction> findByStatus(@Param("status") String status);

    /**
     * /**
     * Trouve les transactions en attente par carte
     */
    @Query("SELECT t FROM Transaction t WHERE t.idCarte = :cardId AND t.status IN ('PENDING', 'INITIATED')")
    List<Transaction> findPendingTransactionsByCard(@Param("cardId") String cardId);

    /**
     * Trouver les retraits d'une carte spécifique
     */
    @Query("{'type': ?0, 'idCarte': ?1}")
    List<Transaction> findByTypeAndIdCarteOrderByCreatedAtDesc(String type, String idCarte);

    /**
     * Trouver les transactions avec callbacks échoués
     */

    /**
     * Transactions par client et type avec pagination
     */
    @Query("{'clientId': ?0, 'type': ?1}")
    List<Transaction> findByClientIdAndType(String clientId, String type);

    /**
     * Trouver les transactions en attente depuis plus de X minutes
     */
    @Query("{'status': 'PENDING', 'createdAt': {'$lt': ?0}}")
    List<Transaction> findPendingTransactionsOlderThan2(LocalDateTime cutoffTime);

    /**
     * Trouver les transactions par référence FreemoPay et type
     */
    @Query("{'freemoReference': ?0, 'type': ?1}")
    Optional<Transaction> findByFreemoReferenceAndType(String freemoReference, String type);

    /**
     * Trouver les recharges carte pour un client
     */
    @Query("{'type': 'CARD_RECHARGE', 'clientId': ?0, 'idCarte': ?1}")
    List<Transaction> findCardRechargesByClientAndCard(String clientId, String idCarte);

    /**
     * Transactions récentes (dernières 24h)
     */
    @Query("{'createdAt': {'$gte': ?0}}")
    List<Transaction> findRecentTransactions(LocalDateTime since);

    /**
     * Trouver les transactions nécessitant une notification
     */
    @Query("{'callbackUrl': {'$ne': null}, 'status': {'$ne': 'PENDING'}, 'callbackRetries': {'$lt': 3}}")
    List<Transaction> findTransactionsNeedingCallback();

    /**
     * Statistiques globales par type et statut
     */
    @Query("{'type': ?0, 'status': ?1, 'createdAt': {'$gte': ?2}}")
    List<Transaction> findByTypeAndStatusSince(String type, String status, LocalDateTime since);

    /**
     * Recherche par numéro de téléphone et type
     */
    @Query("{'phoneNumber': ?0, 'type': ?1}")
    List<Transaction> findByPhoneNumberAndType(String phoneNumber, String type);

    /**
     * Dernière transaction réussie pour une carte
     */
    @Query(value = "{'idCarte': ?0, 'status': 'SUCCESS'}", sort = "{'createdAt': -1}")
    Optional<Transaction> findLastSuccessfulTransactionByCard(String idCarte);

    /**
     * Transactions avec montant supérieur à un seuil
     */
    @Query("{'amount': {'$gte': ?0}, 'type': ?1}")
    List<Transaction> findHighValueTransactions(double minAmount, String type);

    /**
     * Vérifier l'existence d'une transaction par external ID
     */

    /**
     * Trouver les doublons potentiels (même client, même montant, même jour)
     */
    @Query("{'clientId': ?0, 'amount': ?1, 'createdAt': {'$gte': ?2, '$lt': ?3}, 'type': ?4}")
    List<Transaction> findPotentialDuplicates(String clientId, double amount,
            LocalDateTime dayStart, LocalDateTime dayEnd, String type);

    /**
     * Transactions avec callback URL spécifique
     */
    @Query("{'callbackUrl': {'$regex': ?0}}")
    List<Transaction> findByCallbackUrlPattern(String urlPattern);

    /**
     * Compter les transactions du jour pour un client
     */
    @Query(value = "{'clientId': ?0, 'createdAt': {'$gte': ?1}, 'type': ?2}", count = true)
    long countDailyTransactionsByClientAndType(String clientId, LocalDateTime dayStart, String type);

    /**
     * Trouver les transactions expirées non traitées
     */
    @Query("{'status': 'PENDING', 'createdAt': {'$lt': ?0}, 'type': {'$in': ['CARD_WITHDRAWAL', 'CARD_RECHARGE']}}")
    List<Transaction> findExpiredCardTransactions(LocalDateTime expiredBefore);

    /**
     * CORRIGÉ: Recherche par référence FreemoPay (doit être unique)
     */
    @Query("{'freemoReference': ?0}")
    Optional<Transaction> findByFreemoReference(String freemoReference);

    /**
     * NOUVEAU: Vérifier l'unicité de l'externalId
     */
    @Query(value = "{'externalId': ?0}", exists = true)
    boolean existsByExternalId(String externalId);

    /**
     * NOUVEAU: Vérifier l'unicité de la référence FreemoPay
     */
    @Query(value = "{'freemoReference': ?0}", exists = true)
    boolean existsByFreemoReference(String freemoReference);

    /**
     * CORRIGÉ: Recherche avec tri pour éviter les doublons
     */
    @Query(value = "{'status': ?0}", sort = "{'createdAt': -1}")
    List<Transaction> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * CORRIGÉ: Recherche avec limitation
     */
    @Query(value = "{'status': ?0, 'createdAt': {'$lt': ?1}}", sort = "{'createdAt': -1}")
    List<Transaction> findByStatusAndCreatedAtBefore(String status, LocalDateTime before);

    /**
     * CORRIGÉ: Recherche client/type avec tri
     */
    @Query(value = "{'clientId': ?0, 'type': ?1}", sort = "{'createdAt': -1}")
    List<Transaction> findByClientIdAndTypeOrderByCreatedAtDesc(String clientId, String type);

    // ========================================
    // NOUVELLES MÉTHODES POUR ÉVITER LES DOUBLONS
    // ========================================

    /**
     * Trouver la dernière transaction par référence FreemoPay
     */
    @Query(value = "{'freemoReference': ?0}", sort = "{'createdAt': -1}")
    Optional<Transaction> findFirstByFreemoReferenceOrderByCreatedAtDesc(String freemoReference);

    /**
     * Trouver les transactions en PENDING avec référence FreemoPay null
     */
    @Query("{'status': 'PENDING', 'freemoReference': null}")
    List<Transaction> findPendingTransactionsWithoutFreemoReference();

    /**
     * Trouver les doublons par externalId
     */
    @Query("{'externalId': ?0}")
    List<Transaction> findAllByExternalId(String externalId);

    /**
     * Trouver les doublons par référence FreemoPay
     */
    @Query("{'freemoReference': ?0}")
    List<Transaction> findAllByFreemoReference(String freemoReference);

    /**
     * Nettoyer les transactions orphelines
     */
    @Query("{'createdAt': {'$lt': ?0}, 'status': 'PENDING', 'freemoReference': null}")
    List<Transaction> findOrphanedTransactions(LocalDateTime cutoffTime);

    // ========================================
    // MÉTHODES SPÉCIFIQUES AUX RETRAITS CARTE
    // ========================================

    @Query(value = "{'type': 'CARD_WITHDRAWAL', 'idCarte': ?0}", sort = "{'createdAt': -1}")
    List<Transaction> findByTypeAndIdCarteOrderByCreatedAtDesc(String idCarte);

    @Query("{'callbackRetries': {'$gt': 0, '$lt': ?0}, 'status': {'$ne': 'PENDING'}}")
    List<Transaction> findTransactionsWithFailedCallbacks(int maxRetries);

    @Query("{'type': 'CARD_WITHDRAWAL', 'status': {'$in': ['FAILED', 'CANCELLED', 'EXPIRED']}, 'callbackUrl': {'$ne': null}}")
    List<Transaction> findFailedCardWithdrawalsNeedingRefund();

    @Query("{'type': 'CARD_WITHDRAWAL', 'idCarte': ?0, 'createdAt': {'$gte': ?1, '$lte': ?2}}")
    List<Transaction> findCardWithdrawalsBetweenDates(String idCarte, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "{'type': 'CARD_WITHDRAWAL', 'idCarte': ?0, 'status': 'SUCCESS'}", count = true)
    long countSuccessfulWithdrawalsByCard(String idCarte);

    @Query(value = "{'type': 'CARD_WITHDRAWAL', 'idCarte': ?0, 'status': {'$in': ['FAILED', 'CANCELLED']}}", count = true)
    long countFailedWithdrawalsByCard(String idCarte);

    /**
     * NOUVEAU: Recherche sécurisée avec limitation à 1 résultat
     */
    @Query(value = "{'freemoReference': ?0}", sort = "{'updatedAt': -1}")
    Optional<Transaction> findMostRecentByFreemoReference(String freemoReference);

    /**
     * NOUVEAU: Recherche par type et référence FreemoPay
     */
    @Query(value = "{'freemoReference': ?0, 'type': ?1}", sort = "{'createdAt': -1}")
    Optional<Transaction> findFirstByFreemoReferenceAndTypeOrderByCreatedAtDesc(String freemoReference, String type);

    /**
     * NOUVEAU: Statistiques de santé de la base
     */
    @Query(value = "{'externalId': {'$exists': true}}", count = true)
    long countTransactionsWithExternalId();

    @Query(value = "{'freemoReference': {'$exists': true, '$ne': null}}", count = true)
    long countTransactionsWithFreemoReference();

    /**
     * NOUVEAU: Détecter les transactions dupliquées
     */
    @Query("{'$or': [{'externalId': {'$in': ?0}}, {'freemoReference': {'$in': ?0}}]}")
    List<Transaction> findDuplicateTransactions(List<String> identifiers);
}