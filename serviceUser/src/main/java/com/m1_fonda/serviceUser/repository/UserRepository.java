package com.m1_fonda.serviceUser.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientStatus;

@RepositoryRestResource
public interface UserRepository extends MongoRepository<Client, String> {

    /**
     * Recherche par email (case insensitive)
     */
    @Query("{'email': {$regex: ?0, $options: 'i'}}")
    Optional<Client> findByEmail(String email);
    
    /**
     * Recherche par CNI
     */
    @Query("{'cni': ?0}")
    Optional<Client> findByCni(String cni);
    
    /**
     * Recherche par numéro de téléphone
     */
    @Query("{'numero': ?0}")
    Optional<Client> findByNumero(String numero);
    
    /**
     * Recherche client actif par numéro (pour authentification)
     */
    @Query("{'numero': ?0, 'status': 'ACTIVE'}")
    Optional<Client> findActiveClientByNumero(String numero);
    
    /**
     * Recherche client actif par email (pour authentification)
     */
    @Query("{'email': {$regex: ?0, $options: 'i'}, 'status': 'ACTIVE'}")
    Optional<Client> findActiveClientByEmail(String email);
    
    // =====================================
    // REQUÊTES MÉTIER
    // =====================================
    
    /**
     * Clients par statut
     */
    @Query("{'status': ?0}")
    List<Client> findByStatus(ClientStatus status);
    
    /**
     * Clients par agence
     */
    @Query("{'idAgence': ?0}")
    List<Client> findByAgence(String idAgence);
    
    /**
     * Clients avec tentatives de connexion échouées
     */
    @Query("{'loginAttempts': {$gte: ?0}}")
    List<Client> findClientsWithFailedAttempts(int minAttempts);
    
    /**
     * Clients créés dans une période
     */
    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<Client> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Clients inactifs (pas de connexion depuis X jours)
     */
    @Query("{'lastLogin': {$lt: ?0}, 'status': 'ACTIVE'}")
    List<Client> findInactiveClients(LocalDateTime cutoffDate);

     
    /**
     * Compter clients par statut
     */
    @Query(value = "{'status': ?0}", count = true)
    long countByStatus(ClientStatus status);
    
    /**
     * Compter nouveaux clients du jour
     */
    @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
    long countNewClientsToday(LocalDateTime startOfDay);
    
    /**
     * Clients bloqués ou suspendus
     */
    @Query("{'status': {$in: ['BLOCKED', 'SUSPENDED']}}")
    List<Client> findBlockedOrSuspendedClients();
    
    // =====================================
    // MISES À JOUR SÉCURISÉES
    // =====================================
    
    /**
     * Mise à jour dernière connexion
     */
   // @Modifying
    @Query("{'_id': ?0}")
    @Update("{'$set': {'lastLogin': ?1, 'loginAttempts': 0}}")
    void updateLastLogin(String clientId, LocalDateTime loginTime);
    
    /**
     * Incrémenter tentatives de connexion échouées
     */
   // @Modifying
    @Query("{'_id': ?0}")
    @Update("{'$inc': {'loginAttempts': 1}, '$set': {'lastFailedLogin': ?1}}")
    void incrementFailedLoginAttempts(String clientId, LocalDateTime failedTime);
    
    /**
     * Réinitialiser tentatives de connexion
     */
   // @Modifying
    @Query("{'_id': ?0}")
    @Update("{'$set': {'loginAttempts': 0, 'lastFailedLogin': null}}")
    void resetLoginAttempts(String clientId);
    
    /**
     * Mettre à jour le statut
     */
    //@Modifying
    @Query("{'_id': ?0}")
    @Update("{'$set': {'status': ?1, 'updatedAt': ?2}}")
    void updateStatus(String clientId, ClientStatus status, LocalDateTime updatedAt);
    
    /**
     * Mettre à jour mot de passe
     */
    //@Modifying
    @Query("{'_id': ?0}")
    @Update("{'$set': {'passwordHash': ?1, 'salt': ?2, 'passwordChangedAt': ?3, 'loginAttempts': 0}}")
    void updatePassword(String clientId, String passwordHash, String salt, LocalDateTime changedAt);
    
    // =====================================
    // REQUÊTES DE SÉCURITÉ ET AUDIT
    // =====================================
    
    /**
     * Clients avec activité suspecte (trop de tentatives)
     */
    @Query("{'loginAttempts': {$gte: 3}, 'lastFailedLogin': {$gte: ?0}}")
    List<Client> findSuspiciousActivity(LocalDateTime since);
    
    /**
     * Clients nécessitant changement de mot de passe
     */
    @Query("{'passwordChangedAt': {$lt: ?0}, 'status': 'ACTIVE'}")
    List<Client> findClientsNeedingPasswordChange(LocalDateTime cutoffDate);
    
    /**
     * Recherche par critères multiples (pour admin)
     * @param page 
     */
    @Query("{'': [" +
           "{'': [{'nom': {: ?0, : 'i'}}, {'prenom': {: ?0, : 'i'}}, {'email': {: ?0, : 'i'}}]}," +
           "{'status': {: ?1}}" +
           "]}")
    List<Client> searchClients(String searchTerm, List<ClientStatus> statuses, int page);
    
    // =====================================
    // VÉRIFICATIONS D'EXISTENCE
    // =====================================
    
    /**
     * Vérifier si email existe
     */
    boolean existsByEmail(String email);
    
    /**
     * Vérifier si CNI existe
     */
    boolean existsByCni(String cni);
    
    /**
     * Vérifier si numéro existe
     */
    boolean existsByNumero(String numero);
    
    /**
     * Vérifier si client actif existe
     */
    @Query(value = "{'numero': ?0, 'status': 'ACTIVE'}", exists = true)
    boolean existsActiveClientByNumero(String numero);
}

    

