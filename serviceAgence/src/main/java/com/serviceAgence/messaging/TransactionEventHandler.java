package com.serviceAgence.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.serviceAgence.dto.TransactionRequest;
import com.serviceAgence.dto.TransactionResult;
import com.serviceAgence.enums.TransactionType;
import com.serviceAgence.event.TransactionRequestEvent;
import com.serviceAgence.services.AgenceService;
import com.serviceAgence.services.CompteService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TransactionEventHandler {

    @Autowired
    private AgenceService agenceService;

    @Autowired
    private AgenceEventPublisher eventPublisher; // ✅ Maintenant disponible

    @Autowired
    private CompteService compteService; // Pour récupérer l'agence du compte

    /**
     * Réception des demandes de transaction depuis UserService
     */
    @RabbitListener(queues = "Demande-Transaction-Queue")
    public void handleTransactionRequest(TransactionRequestEvent event) {
        log.info("Réception demande transaction: type={}, montant={}, compte={}",
                event.getType(), event.getMontant(), event.getNumeroCompte());

        try {
            // Conversion vers DTO
            TransactionRequest request = convertToTransactionRequestCompte(event);

            // Traitement de la transaction
            TransactionResult result = agenceService.processTransaction(request);

            // Envoi de la réponse
            eventPublisher.sendTransactionResponse(event.getEventId(), result, event.getNumeroCompte());

            log.info("Transaction traitée: {} - Résultat: {}",
                    event.getEventId(), result.isSuccess() ? "SUCCESS" : "FAILED");

        } catch (Exception e) {
            log.error("Erreur traitement transaction: {}", e.getMessage(), e);

            // Envoi réponse d'erreur
            TransactionResult errorResult = TransactionResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du traitement");
            eventPublisher.sendTransactionResponse(event.getEventId(), errorResult, event.getNumeroCompte());
        }
    }

    /**
     * Réception des demandes de transaction depuis UserService
     */
    @RabbitListener(queues = "Demande-Retrait-Queue")
    public void handleWithdrawlRequest(TransactionRequestEvent event) {
        log.info("Réception demande transaction: type={}, montant={}, compte={}",
                event.getType(), event.getMontant(), event.getNumeroCompte());

        try {
            // Conversion vers DTO
            TransactionRequest request = convertToTransactionRequestCard(event);

            // Traitement de la transaction
            TransactionResult result = agenceService.processTransaction(request);

            // Envoi de la réponse
            eventPublisher.sendTransactionResponseCard(event.getEventId(), result, event.getNumeroCompte());

            log.info("Transaction traitée: {} - Résultat: {}",
                    event.getEventId(), result.isSuccess() ? "SUCCESS" : "FAILED");

        } catch (Exception e) {
            log.error("Erreur traitement transaction: {}", e.getMessage(), e);

            // Envoi réponse d'erreur
            TransactionResult errorResult = TransactionResult.failed("ERREUR_TECHNIQUE",
                    "Erreur technique lors du traitement");
            eventPublisher.sendTransactionResponseCard(event.getEventId(), errorResult, event.getNumeroCompte());
        }
    }

    /**
     * Conversion événement vers DTO avec récupération de l'agence pour les
     * transfert compte-compte
     */
    private TransactionRequest convertToTransactionRequestCompte(TransactionRequestEvent event) {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.TRANSFERT_INTERNE);
        request.setMontant(event.getMontant());
        request.setCompteSource(event.getNumeroCompte());
        request.setCompteDestination(event.getNumeroCompteDestination());
        request.setIdClient(event.getNumeroClient());
        request.setDescription("Transaction depuis UserService");

        // Récupération de l'agence depuis le compte
        try {
            String numeroCompte = event.getNumeroCompte();
            var compte = compteService.getAccountDetails(numeroCompte);
            request.setIdAgence(compte.getIdAgence());
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'agence pour le compte {}: {}",
                    event.getNumeroCompte(), e.getMessage());
            request.setIdAgence("DEFAULT_AGENCE");
        }

        return request;
    }

    /**
     * Conversion événement vers DTO avec récupération de l'agence pour les
     * transfere vers la carte
     */
    private TransactionRequest convertToTransactionRequestCard(TransactionRequestEvent event) {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.TRANSFERT_VERS_CARTE);
        request.setMontant(event.getMontant());
        request.setCompteSource(event.getNumeroCompte());
        request.setCompteDestination(event.getNumeroCompteDestination());
        request.setIdClient(event.getNumeroClient());
        request.setDescription("Transaction depuis UserService");

        // Récupération de l'agence depuis le compte
        try {
            String numeroCompte = event.getNumeroCompte();
            var compte = compteService.getAccountDetails(numeroCompte);
            request.setIdAgence(compte.getIdAgence());
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'agence pour le compte {}: {}",
                    event.getNumeroCompte(), e.getMessage());
            request.setIdAgence("DEFAULT_AGENCE");
        }

        return request;
    }
}
