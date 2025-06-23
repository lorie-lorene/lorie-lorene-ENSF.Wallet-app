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

/**
 * 🗄️ User Repository - Enhanced with Additional Methods
 * MongoDB repository for Client entity with comprehensive query methods
 */
@RepositoryRestResource
public interface UserRepository extends MongoRepository<Client, String> {

    // =====================================
    // BASIC SEARCH METHODS
    // =====================================

    /**
     * Find by email (case insensitive)
     */
    @Query("{'email': {$regex: ?0, $options: 'i'}}")
    Optional<Client> findByEmail(String email);
    
    /**
     * Find by CNI
     */
    @Query("{'cni': ?0}")
    Optional<Client> findByCni(String cni);
    
    /**
     * Find by phone number
     */
    @Query("{'numero': ?0}")
    Optional<Client> findByNumero(String numero);
    
    /**
     * Find active client by phone number (for authentication)
     */
    @Query("{'numero': ?0, 'status': 'ACTIVE'}")
    Optional<Client> findActiveClientByNumero(String numero);
    
    /**
     * Find active client by email (for authentication)
     */
    @Query("{'email': {$regex: ?0, $options: 'i'}, 'status': 'ACTIVE'}")
    Optional<Client> findActiveClientByEmail(String email);

    // =====================================
    // EXISTENCE CHECK METHODS
    // =====================================

    /**
     * Check if email exists
     */
    @Query(value = "{'email': {$regex: ?0, $options: 'i'}}", exists = true)
    boolean existsByEmail(String email);
    
    /**
     * Check if CNI exists
     */
    @Query(value = "{'cni': ?0}", exists = true)
    boolean existsByCni(String cni);
    
    /**
     * Check if phone number exists
     */
    @Query(value = "{'numero': ?0}", exists = true)
    boolean existsByNumero(String numero);

    // =====================================
    // STATUS-BASED QUERIES
    // =====================================
    
    /**
     * Find clients by status
     */
    @Query("{'status': ?0}")
    List<Client> findByStatus(ClientStatus status);
    
    /**
     * Find clients by multiple statuses
     */
    @Query("{'status': {$in: ?0}}")
    List<Client> findByStatusIn(List<ClientStatus> statuses);
    
    /**
     * Find clients by agency
     */
    @Query("{'idAgence': ?0}")
    List<Client> findByAgence(String idAgence);
    
    /**
     * Find clients by agency and status
     */
    @Query("{'idAgence': ?0, 'status': ?1}")
    List<Client> findByAgenceAndStatus(String idAgence, ClientStatus status);

    // =====================================
    // SECURITY AND LOGIN TRACKING
    // =====================================
    
    /**
     * Find clients with failed login attempts >= threshold
     */
    @Query("{'loginAttempts': {$gte: ?0}}")
    List<Client> findClientsWithFailedAttempts(int minAttempts);
    
    /**
     * Find clients locked due to failed attempts
     */
    @Query("{'loginAttempts': {$gte: ?0}, 'lastFailedLogin': {$gte: ?1}}")
    List<Client> findLockedClients(int maxAttempts, LocalDateTime lockoutThreshold);
    
    /**
     * Find clients who haven't logged in since a date
     */
    @Query("{'lastLogin': {$lt: ?0}}")
    List<Client> findInactiveClientsSince(LocalDateTime date);
    
    /**
     * Find clients who logged in today
     */
    @Query("{'lastLogin': {$gte: ?0}}")
    List<Client> findClientsLoggedInSince(LocalDateTime date);

    // =====================================
    // DATE-BASED QUERIES
    // =====================================
    
    /**
     * Find clients created between dates
     */
    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<Client> findClientsCreatedBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find clients created today
     */
    @Query("{'createdAt': {$gte: ?0}}")
    List<Client> findClientsCreatedSince(LocalDateTime date);
    
    /**
     * Find clients by creation date and status
     */
    @Query("{'createdAt': {$gte: ?0}, 'status': ?1}")
    List<Client> findClientsCreatedSinceWithStatus(LocalDateTime date, ClientStatus status);

    // =====================================
    // SEARCH AND FILTERING
    // =====================================
    
    /**
     * Search clients by name (case insensitive)
     */
    @Query("{'$or': [" +
           "{'nom': {$regex: ?0, $options: 'i'}}, " +
           "{'prenom': {$regex: ?0, $options: 'i'}}" +
           "]}")
    List<Client> findByNameContaining(String searchTerm);
    
    /**
     * Search clients by multiple criteria
     */
    @Query("{'$or': [" +
           "{'nom': {$regex: ?0, $options: 'i'}}, " +
           "{'prenom': {$regex: ?0, $options: 'i'}}, " +
           "{'email': {$regex: ?0, $options: 'i'}}, " +
           "{'numero': {$regex: ?0, $options: 'i'}}" +
           "]}")
    List<Client> searchClients(String searchTerm);
    
    /**
     * Find clients with incomplete profiles
     */
    @Query("{'$or': [" +
           "{'rectoCni': {$exists: false}}, " +
           "{'versoCni': {$exists: false}}, " +
           "{'rectoCni': null}, " +
           "{'versoCni': null}" +
           "]}")
    List<Client> findClientsWithIncompleteDocuments();

    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<Client> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

       /**
        * Count new clients created today
        */
       @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
       long countNewClientsToday(LocalDateTime startOfDay);

       /**
        * Find blocked or suspended clients
        */
       @Query("{'status': {$in: ['BLOCKED', 'SUSPENDED']}}")
       List<Client> findBlockedOrSuspendedClients();

       /**
        * Check if active client exists by phone number
        */
       @Query(value = "{'numero': ?0, 'status': 'ACTIVE'}", exists = true)
       boolean existsActiveClientByNumero(String numero);

       /**
        * Find clients with suspicious activity (multiple failed attempts recently)
        */
       @Query("{'loginAttempts': {$gte: 3}, 'lastFailedLogin': {$gte: ?0}}")
       List<Client> findSuspiciousActivity(LocalDateTime since);

       /**
        * Find clients needing password change (old passwords)
        */
       @Query("{'passwordChangedAt': {$lt: ?0}}")
       List<Client> findClientsNeedingPasswordChange(LocalDateTime cutoff);

       /**
        * Increment failed login attempts
        */
       @Query("{'_id': ?0}")
       @Update("{'$inc': {'loginAttempts': 1}, '$set': {'lastFailedLogin': ?1}}")
       void incrementFailedLoginAttempts(String clientId, LocalDateTime failedTime);
    // =====================================
    // AGGREGATION AND STATISTICS
    // =====================================
    
    /**
     * Count clients by status
     */
    @Query(value = "{'status': ?0}", count = true)
    long countByStatus(ClientStatus status);
    
    /**
     * Count clients by agency
     */
    @Query(value = "{'idAgence': ?0}", count = true)
    long countByAgence(String idAgence);
    
    /**
     * Count clients created since date
     */
    @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
    long countClientsCreatedSince(LocalDateTime date);
    
    /**
     * Count active clients who logged in since date
     */
    @Query(value = "{'status': 'ACTIVE', 'lastLogin': {$gte: ?0}}", count = true)
    long countActiveClientsLoggedInSince(LocalDateTime date);
    

    // =====================================
    // UPDATE OPERATIONS
    // =====================================
    
    /**
     * Update last login time
     */
    @Query("{'_id': ?0}")
    @Update("{'$set': {'lastLogin': ?1, 'loginAttempts': 0, 'lastFailedLogin': null}}")
    void updateLastLogin(String clientId, LocalDateTime lastLogin);
    
    /**
     * Increment login attempts
     */
    @Query("{'_id': ?0}")
    @Update("{'$inc': {'loginAttempts': 1}, '$set': {'lastFailedLogin': ?1}}")
    void incrementLoginAttempts(String clientId, LocalDateTime lastFailedLogin);
    
    /**
     * Reset login attempts
     */
    @Query("{'_id': ?0}")
    @Update("{'$set': {'loginAttempts': 0, 'lastFailedLogin': null}}")
    void resetLoginAttempts(String clientId);
    
    /**
     * Update client status
     */
    @Query("{'_id': ?0}")
    @Update("{'$set': {'status': ?1}}")
    void updateClientStatus(String clientId, ClientStatus status);
    
    /**
     * Update password
     */
    @Query("{'_id': ?0}")
    @Update("{'$set': {'passwordHash': ?1, 'passwordChangedAt': ?2}}")
    void updatePassword(String clientId, String passwordHash, String salt, LocalDateTime passwordChangedAt);

    // =====================================
    // BULK OPERATIONS
    // =====================================
    
    /**
     * Find clients for bulk status update
     */
    @Query("{'status': ?0, 'createdAt': {$lt: ?1}}")
    List<Client> findClientsForBulkStatusUpdate(ClientStatus currentStatus, LocalDateTime beforeDate);
    
    /**
     * Find clients for cleanup (inactive for long time)
     */
    @Query("{'status': {$in: ['REJECTED', 'BLOCKED']}, 'createdAt': {$lt: ?0}}")
    List<Client> findClientsForCleanup(LocalDateTime beforeDate);

    // =====================================
    // COMPLEX QUERIES
    // =====================================
    
    /**
     * Find clients needing KYC reminder
     */
    @Query("{'status': 'PENDING', 'createdAt': {$lt: ?0, $gte: ?1}}")
    List<Client> findClientsNeedingKycReminder(LocalDateTime reminderDate, LocalDateTime cutoffDate);
    
    /**
     * Find suspicious clients (multiple failed attempts recently)
     */
    @Query("{'loginAttempts': {$gte: ?0}, 'lastFailedLogin': {$gte: ?1}}")
    List<Client> findSuspiciousClients(int minFailedAttempts, LocalDateTime recentDate);
    
    /**
     * Find clients eligible for reactivation
     */
    @Query("{'status': 'SUSPENDED', '$or': [" +
           "{'lastFailedLogin': {$lt: ?0}}, " +
           "{'lastFailedLogin': null}" +
           "]}")
    List<Client> findClientsEligibleForReactivation(LocalDateTime reactivationDate);

    // =====================================
    // REPORTING QUERIES
    // =====================================
    
    /**
     * Get client registration stats by date range
     */
    @Query(value = "{'createdAt': {$gte: ?0, $lte: ?1}}", 
           fields = "{'createdAt': 1, 'status': 1, 'idAgence': 1}")
    List<Client> getRegistrationStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get login activity stats
     */
    @Query(value = "{'lastLogin': {$gte: ?0}}", 
           fields = "{'lastLogin': 1, 'status': 1, 'idAgence': 1}")
    List<Client> getLoginActivitySince(LocalDateTime date);
    
    /**
     * Find duplicate potential clients (same CNI or phone)
     */
    @Query("{'$or': [{'cni': ?0}, {'numero': ?1}], '_id': {$ne: ?2}}")
    List<Client> findPotentialDuplicates(String cni, String numero, String excludeClientId);
}