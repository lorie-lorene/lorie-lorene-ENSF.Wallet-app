package com.wallet.money.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.wallet.money.entity.Transaction;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    
    // Trouver par référence FreemoPay
    Optional<Transaction> findByFreemoReference(String freemoReference);
    
    // Trouver par notre ID externe
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
}


