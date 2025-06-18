package com.serviceAgence.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.serviceAgence.enums.DocumentStatus;
import com.serviceAgence.enums.DocumentType;
import com.serviceAgence.model.DocumentKYC;

@Repository
public interface DocumentKYCRepository extends MongoRepository<DocumentKYC, String> {
    
    List<DocumentKYC> findByIdClient(String idClient);
    
    List<DocumentKYC> findByIdClientOrderByUploadedAtDesc(String idClient);
    
    Optional<DocumentKYC> findByIdClientAndType(String idClient, DocumentType type);
    
    boolean existsByNumeroDocumentAndType(String numeroDocument, DocumentType type);
    
    List<DocumentKYC> findByStatus(DocumentStatus status);
    
    List<DocumentKYC> findByFraudDetected(boolean fraudDetected);
    
    long countByIdClientAndStatus(String idClient, DocumentStatus status);
}