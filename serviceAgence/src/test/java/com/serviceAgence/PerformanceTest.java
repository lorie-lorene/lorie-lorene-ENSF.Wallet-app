package com.serviceAgence;


import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import com.serviceAgence.dto.TransactionRequest;
import com.serviceAgence.dto.TransactionResult;
import com.serviceAgence.enums.CompteStatus;
import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.model.CompteUser;
import com.serviceAgence.repository.CompteRepository;
import com.serviceAgence.services.TransactionService;

@SpringBootTest(classes = ServiceAgenceApplication.class)
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/agence_performance_test",
    "logging.level.com.serviceAgence=WARN"
})
class PerformanceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CompteRepository compteRepository;

    private CompteUser testCompte;

    @BeforeEach
    void setUp() {
        // Nettoyer et créer un compte de test avec beaucoup de solde
        compteRepository.deleteAll();
        
        testCompte = new CompteUser();
        testCompte.setNumeroCompte(999888777L);
        testCompte.setIdClient("PERF_CLIENT");
        testCompte.setIdAgence("PERF_AGENCE");
        testCompte.setSolde(new BigDecimal("10000000")); // 10M FCFA
        testCompte.setStatus(CompteStatus.ACTIVE);
        testCompte.setLimiteDailyWithdrawal(new BigDecimal("50000000"));
        testCompte.setLimiteDailyTransfer(new BigDecimal("50000000"));
        
        compteRepository.save(testCompte);
    }

    @Test
    void testTransactionPerformance_Sequential() {
        // Given
        int numberOfTransactions = 100;
        List<TransactionRequest> requests = createTransactionRequests(numberOfTransactions);
        
        // When
        LocalDateTime start = LocalDateTime.now();
        
        List<TransactionResult> results = new ArrayList<>();
        for (TransactionRequest request : requests) {
            try {
                TransactionResult result = transactionService.processTransaction(request);
                results.add(result);
            } catch (Exception e) {
                // Ignorer les erreurs pour ce test de performance
            }
        }
        
        LocalDateTime end = LocalDateTime.now();
        Duration duration = Duration.between(start, end);

        // Then
        long executionTimeMs = duration.toMillis();
        double avgTimePerTransaction = (double) executionTimeMs / numberOfTransactions;
        
        System.out.println("=== PERFORMANCE TEST RESULTS ===");
        System.out.println("Transactions: " + numberOfTransactions);
        System.out.println("Total time: " + executionTimeMs + " ms");
        System.out.println("Average time per transaction: " + avgTimePerTransaction + " ms");
        System.out.println("Successful transactions: " + results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum());
        
        // Assertions
        assertTrue(executionTimeMs < 30000, "Les transactions doivent prendre moins de 30 secondes au total");
        assertTrue(avgTimePerTransaction < 500, "Chaque transaction doit prendre moins de 500ms en moyenne");
    }

    @Test
    void testTransactionPerformance_Concurrent() throws Exception {
        // Given
        int numberOfTransactions = 50;
        int numberOfThreads = 10;
        List<TransactionRequest> requests = createTransactionRequests(numberOfTransactions);
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        // When
        LocalDateTime start = LocalDateTime.now();
        
        List<CompletableFuture<TransactionResult>> futures = new ArrayList<>();
        for (TransactionRequest request : requests) {
            CompletableFuture<TransactionResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return transactionService.processTransaction(request);
                } catch (Exception e) {
                    return TransactionResult.failed("ERROR", e.getMessage());
                }
            }, executor);
            futures.add(future);
        }
        
        // Attendre que toutes les transactions se terminent
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        LocalDateTime end = LocalDateTime.now();
        Duration duration = Duration.between(start, end);
        
        // Then
        long executionTimeMs = duration.toMillis();
        long successfulTransactions = futures.stream()
                .mapToLong(f -> f.join().isSuccess() ? 1 : 0)
                .sum();
        
        System.out.println("=== CONCURRENT PERFORMANCE TEST RESULTS ===");
        System.out.println("Transactions: " + numberOfTransactions);
        System.out.println("Threads: " + numberOfThreads);
        System.out.println("Total time: " + executionTimeMs + " ms");
        System.out.println("Successful transactions: " + successfulTransactions);
        
        // Assertions
        assertTrue(executionTimeMs < 15000, "Les transactions concurrentes doivent prendre moins de 15 secondes");
        assertTrue(successfulTransactions > numberOfTransactions * 0.8, "Au moins 80% des transactions doivent réussir");
        
        executor.shutdown();
    }

    private List<TransactionRequest> createTransactionRequests(int count) {
        List<TransactionRequest> requests = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TransactionRequest request = new TransactionRequest();
            request.setType(TransactionType.DEPOT_PHYSIQUE);
            request.setMontant(new BigDecimal("1000"));
            request.setCompteSource(testCompte.getNumeroCompte().toString());
            request.setIdClient("PERF_CLIENT");
            request.setIdAgence("PERF_AGENCE");
            request.setDescription("Performance test transaction " + i);
            
            requests.add(request);
        }
        
        return requests;
    }
}