package com.serviceAgence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.serviceAgence.enums.TransactionStatus;
import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.model.Transaction;

@RepositoryRestResource
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByCompteSource(String compteSource);

    List<Transaction> findByCompteDestination(String compteDestination);

    List<Transaction> findByIdClient(String idClient);

    List<Transaction> findByIdAgence(String idAgence);

    List<Transaction> findByStatus(TransactionStatus status);

    @Query("{ 'idAgence': ?0, 'status': ?1 }")
    List<Transaction> findByIdAgenceAndStatus(String idAgence, TransactionStatus status);

    @Query("{ 'idAgence': ?0, 'status': 'COMPLETED' }")
    List<Transaction> findCompletedTransactionsByIdAgence(String idAgence);

    List<Transaction> findByType(TransactionType type);

    @Query("{ 'idAgence': ?0, 'type': ?1 }")
    List<Transaction> findByIdAgenceAndType(String idAgence, TransactionType type);

    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("{ 'idAgence': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }")
    List<Transaction> findByIdAgenceAndCreatedAtBetween(String idAgence, LocalDateTime start, LocalDateTime end);

    long countByIdAgence(String idAgence);

    long countByIdAgenceAndStatus(String idAgence, TransactionStatus status);

    long countByIdAgenceAndType(String idAgence, TransactionType type);

    long countByStatus(TransactionStatus status);

    Page<Transaction> findByIdAgenceOrderByCreatedAtDesc(String idAgence, Pageable pageable);

    Page<Transaction> findByCompteSourceOrderByCreatedAtDesc(String compteSource, Pageable pageable);

    @Query("{ 'compteSource': ?0, 'status': ?1 }")
    List<Transaction> findByCompteSourceAndStatus(String compteSource, TransactionStatus status);

    @Query("{ 'compteDestination': ?0, 'status': ?1 }")
    List<Transaction> findByCompteDestinationAndStatus(String compteDestination, TransactionStatus status);

    @Query("{ $or: [ { 'compteSource': ?0 }, { 'compteDestination': ?0 } ] }")
    Page<Transaction> findAccountHistory(String numeroCompte, Pageable pageable);
}