package com.serviceAgence.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.serviceAgence.enums.AgenceStatus;
import com.serviceAgence.model.Agence;

@RepositoryRestResource
public interface AgenceRepository extends MongoRepository<Agence, String> {
    
    Optional<Agence> findByCodeAgence(String codeAgence);
    
    List<Agence> findByStatus(AgenceStatus status);
    
    List<Agence> findByVille(String ville);
    
    @Query("{ 'status': 'ACTIVE' }")
    List<Agence> findAllActiveAgences();
    
    boolean existsByCodeAgence(String codeAgence);
    
    boolean existsByEmail(String email);
    
    @Query("{ 'capital': { $gte: ?0 } }")
    List<Agence> findByCapitalGreaterThanEqual(java.math.BigDecimal capital);
}
