package com.serviceAgence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.model.CompteUser;

@RepositoryRestResource
public interface CompteRepository extends MongoRepository<CompteUser, String> {

    Optional<CompteUser> findByNumeroCompte(Long numeroCompte);

    List<CompteUser> findByIdClient(String idClient);

    List<CompteUser> findByIdClientOrderByCreatedAtDesc(String idClient);

    List<CompteUser> findByIdAgence(String idAgence);

    List<CompteUser> findByIdAgenceOrderByCreatedAtDesc(String idAgence, Pageable pageable);

    Optional<CompteUser> findByIdClientAndIdAgence(String idClient, String idAgence);

    boolean existsByIdClientAndIdAgence(String idClient, String idAgence);

    List<CompteUser> findByStatus(CompteStatus status);

    List<CompteUser> findByIdAgenceAndStatus(String idAgence, CompteStatus status);

    // Statistiques
    long countByIdAgence(String idAgence);

    //
    long countByIdAgenceAndStatus(String idAgence, CompteStatus status);

    // @Query("{ 'idAgence': ?0 }")
    @Query("{ 'idAgence': ?0 }")
    List<CompteUser> findComptesByIdAgence(String idAgence);

    @Query("{ 'solde': { $gte: ?0 } }")
    List<CompteUser> findBySoldeGreaterThanEqual(BigDecimal montant);

    @Query("{ 'blocked': true }")
    List<CompteUser> findAllBlockedAccounts();
}
