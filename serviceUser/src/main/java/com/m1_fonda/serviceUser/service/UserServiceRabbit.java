package com.m1_fonda.serviceUser.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.m1_fonda.serviceUser.event.DepotEvent;
import com.m1_fonda.serviceUser.event.PasswordChangeNotificationEvent;
import com.m1_fonda.serviceUser.event.PasswordResetEvent;
import com.m1_fonda.serviceUser.event.RejectionNotificationEvent;
import com.m1_fonda.serviceUser.event.RetraitEvent;
import com.m1_fonda.serviceUser.event.TransactionEvent;
import com.m1_fonda.serviceUser.event.UserRegistrationEvent;
import com.m1_fonda.serviceUser.event.WelcomeNotificationEvent;
import com.m1_fonda.serviceUser.model.Client;
import com.m1_fonda.serviceUser.pojo.ClientStatus;
import com.m1_fonda.serviceUser.pojo.PasswordResetConfirmation;
import com.m1_fonda.serviceUser.repository.UserRepository;
import com.m1_fonda.serviceUser.request.DepositRequest;
import com.m1_fonda.serviceUser.request.PasswordResetRequest;
import com.m1_fonda.serviceUser.request.TransferRequest;
import com.m1_fonda.serviceUser.request.WithdrawalRequest;
import com.m1_fonda.serviceUser.response.RegistrationResponse;
import com.m1_fonda.serviceUser.response.TransactionResponse;
import com.m1_fonda.serviceUser.service.exceptions.BusinessValidationException;
import com.m1_fonda.serviceUser.service.exceptions.ServiceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Service
@Slf4j
public class UserServiceRabbit {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.rabbitmq.timeout:30000}")
    private long responseTimeout;

    /**
     * Envoie demande de création vers AgenceService
     * : N'enregistre PAS en base avant validation
     */
    public void sendRegistrationEvent(Client client) {
        try {
            // Créer event SANS password
            UserRegistrationEvent event = createRegistrationEvent(client);

            log.info("Envoi demande création vers AgenceService: {}", event.getEmail());

            // Envoyer event sans sauvegarder le client
            rabbitTemplate.convertAndSend("Client-exchange", "demande.send", event);

            log.info("Demande envoyée avec succès - ID Event: {}", event.getEventId());

        } catch (AmqpException e) {
            log.error("Erreur envoi vers AgenceService: {}", e.getMessage(), e);
            throw new ServiceException("Service AgenceService temporairement indisponible");
        } catch (Exception e) {
            log.error("Erreur technique lors de l'envoi: {}", e.getMessage(), e);
            throw new ServiceException("Erreur technique lors de l'envoi de la demande");
        }
    }

    private UserRegistrationEvent createRegistrationEvent(Client client) {
        UserRegistrationEvent event = new UserRegistrationEvent();
        event.setIdClient(client.getIdClient());
        event.setIdAgence(client.getIdAgence());
        event.setCni(client.getCni());
        event.setEmail(client.getEmail());
        event.setNom(client.getNom());
        event.setPrenom(client.getPrenom());
        event.setNumero(client.getNumero());
        event.setRectoCni(client.getRectoCni());
        event.setVersoCni(client.getVersoCni());

        return event;
    }

    // =====================================
    // GESTION DES RÉPONSES AGENCESERVICE
    // =====================================

    @RabbitListener(queues = "Response-Demande-Client-Queue")
    public void handleRegistrationResponse(RegistrationResponse response) {
        try {
            log.info("Réponse AgenceService reçue: {}", response);

            switch (response.getStatut().toUpperCase()) {
                case "ACCEPTE":
                    handleAcceptedRegistration(response);
                    break;
                case "REFUSE":
                    handleRejectedRegistration(response);
                    break;
                default:
                    log.warn("Statut inconnu reçu: {}", response.getStatut());
            }

        } catch (Exception e) {
            log.error("Erreur traitement réponse AgenceService: {}", e.getMessage(), e);
        }
    }

    private void handleAcceptedRegistration(RegistrationResponse response) {
        try {
            Client client = findClientByResponseData(response);
            if (client != null) {
                client.setStatus(ClientStatus.ACTIVE);
                repository.save(client);

                log.info("Compte activé pour client: {} - NumeroCompte: {}",
                        client.getEmail(), response.getNumeroCompte());

                sendWelcomeNotification(client, response.getNumeroCompte());
            }

        } catch (Exception e) {
            log.error("Erreur activation compte: {}", e.getMessage(), e);
        }
    }

    private void handleRejectedRegistration(RegistrationResponse response) {
        try {
            Client client = findClientByResponseData(response);
            if (client != null) {
                client.setStatus(ClientStatus.REJECTED);
                repository.save(client);

                log.info("Demande rejetée pour client: {} - Raison: {}",
                        client.getEmail(), response.getProbleme());

                sendRejectionNotification(client, response.getProbleme());
            }

        } catch (Exception e) {
            log.error("Erreur traitement rejet: {}", e.getMessage(), e);
        }
    }

    private Client findClientByResponseData(RegistrationResponse response) {
        // Rechercher par email d'abord
        if (response.getEmail() != null) {
            return repository.findByEmail(response.getEmail()).orElse(null);
        }
        // Puis par ID client si disponible
        if (response.getIdClient() != null) {
            return repository.findById(response.getIdClient()).orElse(null);
        }
        return null;
    }

    // =====================================
    // OPERATIONS FINANCIÈRES SÉCURISÉES
    // =====================================
    // aps tres utile
    public TransactionResponse sendDepot(DepositRequest request, String clientId) {
        // CORRECTION : Validation métier AVANT le try-catch
        validateFinancialOperation(clientId, request.getMontant());

        try {
            DepotEvent event = new DepotEvent();
            event.setMontant(request.getMontant());
            event.setNumeroClient(request.getNumeroClient());
            event.setNumeroCompte(request.getNumeroCompte());
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(LocalDateTime.now());

            log.info("Envoi demande dépôt: {} FCFA sur compte {}",
                    request.getMontant(), request.getNumeroCompte());

            // Envoi synchrone avec timeout
            rabbitTemplate.setReceiveTimeout(responseTimeout);
            TransactionResponse response = (TransactionResponse) rabbitTemplate
                    .convertSendAndReceive("Client-exchange", "depot.send", event);

            if (response != null) {
                log.info("Dépôt confirmé - ID Transaction: {}", response.getTransactionId());
                return response;
            } else {
                throw new ServiceException("Timeout - Service de dépôt indisponible");
            }

        } catch (AmqpTimeoutException e) {
            log.error("Timeout lors du dépôt: {}", e.getMessage());
            throw new ServiceException("Service de dépôt temporairement indisponible");
        } catch (Exception e) {
            log.error("Erreur envoi dépôt: {}", e.getMessage(), e);
            throw new ServiceException("Erreur technique lors du dépôt");
        }
    }

    /**
     * Envoi dépôt avec validation sécurisée dans la recharge de la carte du client
     * on va appeller l'api pour augmneter le solde la carte du client
     */
    public TransactionResponse sendRetrait(WithdrawalRequest request, String clientId) {
        // CORRECTION : Validation métier AVANT le try-catch
        validateFinancialOperation(clientId, request.getMontant());

        try {
            RetraitEvent event = new RetraitEvent();
            event.setMontant(request.getMontant());
            event.setNumeroClient(request.getNumeroClient());
            event.setNumeroCompte(request.getNumeroCompte());
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(LocalDateTime.now());

            log.info("Envoi demande retrait: {} FCFA du compte {}",
                    request.getMontant(), request.getNumeroCompte());

            // Envoi synchrone avec timeout
            rabbitTemplate.setReceiveTimeout(responseTimeout);
            TransactionResponse response = (TransactionResponse) rabbitTemplate
                    .convertSendAndReceive("Client-exchange", "retrait.send", event);

            if (response != null) {
                log.info("Retrait confirmé - ID Transaction: {}", response.getTransactionId());
                return response;
            } else {
                throw new ServiceException("Timeout - Service de retrait indisponible");
            }

        } catch (AmqpTimeoutException e) {
            log.error("Timeout lors du retrait: {}", e.getMessage());
            throw new ServiceException("Service de retrait temporairement indisponible");
        } catch (Exception e) {
            log.error("Erreur envoi retrait: {}", e.getMessage(), e);
            throw new ServiceException("Erreur technique lors du retrait");
        }
    }

    /**
     * Envoi transaction inter-comptes sécurisée
     * 
     */
    public TransactionResponse sendTransaction(TransferRequest request, String clientId) {
        // CORRECTION : Validations métier AVANT le try-catch
        validateFinancialOperation(clientId, request.getMontant());
        validateAccountTransfer(request);

        try {
            TransactionEvent event = new TransactionEvent();
            event.setMontant(request.getMontant());
            event.setNumeroCompteSend(request.getNumeroCompteSend());
            event.setNumeroCompteReceive(request.getNumeroCompteReceive());
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(LocalDateTime.now());
            event.setClientId(clientId);

            log.info("Envoi transaction: {} FCFA de {} vers {}",
                    request.getMontant(), request.getNumeroCompteSend(), request.getNumeroCompteReceive());

            // Envoi synchrone avec timeout
            rabbitTemplate.setReceiveTimeout(responseTimeout);
            TransactionResponse response = (TransactionResponse) rabbitTemplate
                    .convertSendAndReceive("Client-exchange", "transaction.send", event);

            if (response != null) {
                log.info("Transaction confirmée - ID: {}", response.getTransactionId());
                return response;
            } else {
                throw new ServiceException("Timeout - Service de transaction indisponible");
            }

        } catch (AmqpTimeoutException e) {
            log.error("Timeout lors de la transaction: {}", e.getMessage());
            throw new ServiceException("Service de transaction temporairement indisponible");
        } catch (Exception e) {
            log.error("Erreur envoi transaction: {}", e.getMessage(), e);
            throw new ServiceException("Erreur technique lors de la transaction");
        }
    }

    // =====================================
    // GESTION RESET PASSWORD SÉCURISÉE
    // =====================================

    /**
     * Demande reset password sécurisée
     */
    public void sendPasswordResetRequest(PasswordResetRequest request) {
        try {
            // Vérifier que le client existe
            Client client = repository.findByCni(request.getCni())
                    .orElseThrow(() -> new BusinessValidationException("Client introuvable"));

            // Vérifier données correspondantes
            if (!client.getEmail().equalsIgnoreCase(request.getEmail()) ||
                    !client.getNumero().equals(request.getNumero())) {
                throw new BusinessValidationException("Informations non correspondantes");
            }

            PasswordResetEvent event = new PasswordResetEvent();
            event.setClientId(client.getIdClient());
            event.setCni(client.getCni());
            event.setEmail(client.getEmail());
            event.setNumero(client.getNumero());
            event.setNom(client.getNom());
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(LocalDateTime.now());

            log.info("Envoi demande reset password pour: {}", client.getEmail());

            rabbitTemplate.convertAndSend("Client-exchange", "connexion.send", event);

        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur envoi reset password: {}", e.getMessage(), e);
            throw new ServiceException("Erreur technique lors de la demande");
        }
    }

    /**
     * Traitement confirmation nouveau password
     */
    @RabbitListener(queues = "Response-Connexion-Client-Queue")
    public void handlePasswordResetConfirmation(PasswordResetConfirmation confirmation) {
        try {
            log.info("Confirmation reset password reçue: {}", confirmation);

            Client client = repository.findByCni(confirmation.getCni())
                    .orElseThrow(() -> new BusinessValidationException("Client introuvable"));

            // SÉCURITÉ : Hash du nouveau password
            String newPasswordHash = passwordEncoder.encode(confirmation.getNewPassword());
            String newSalt = UUID.randomUUID().toString().substring(0, 16);

            // Mise à jour sécurisée
            repository.updatePassword(client.getIdClient(), newPasswordHash, LocalDateTime.now());

            log.info("Mot de passe mis à jour pour client: {}", client.getEmail());

            // Envoyer notification de confirmation
            sendPasswordChangeNotification(client);

        } catch (Exception e) {
            log.error("Erreur mise à jour password: {}", e.getMessage(), e);
        }
    }

    // =====================================
    // VALIDATIONS MÉTIER
    // =====================================

    private void validateFinancialOperation(String clientId, BigDecimal montant) {
        // Vérifier client actif
        Client client = repository.findById(clientId)
                .orElseThrow(() -> new BusinessValidationException("Client introuvable"));

        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessValidationException("Compte non actif");
        }

        // Vérifier montant
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Montant invalide");
        }

        if (montant.compareTo(new BigDecimal("10000000")) > 0) { // 10M FCFA max
            throw new BusinessValidationException("Montant trop élevé");
        }
    }

    private void validateAccountTransfer(TransferRequest request) {
        if (request.getNumeroCompteSend().equals(request.getNumeroCompteReceive())) {
            throw new BusinessValidationException("Impossible de transférer vers le même compte");
        }
    }

    // =====================================
    // SERVICES DE discussion avec assistance ..
    // =====================================

    private void sendWelcomeNotification(Client client, Long numeroCompte) {
        try {
            WelcomeNotificationEvent event = new WelcomeNotificationEvent();
            event.setClientId(client.getIdClient());
            event.setEmail(client.getEmail());
            event.setNom(client.getNom());
            event.setPrenom(client.getPrenom());
            event.setNumeroCompte(numeroCompte);
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend("Notification-exchange", "welcome.send", event);
            log.info("Notification bienvenue envoyée pour: {}", client.getEmail());

        } catch (Exception e) {
            log.error("Erreur envoi notification bienvenue: {}", e.getMessage(), e);
        }
    }

    private void sendRejectionNotification(Client client, String raison) {
        try {
            RejectionNotificationEvent event = new RejectionNotificationEvent();
            event.setClientId(client.getIdClient());
            event.setEmail(client.getEmail());
            event.setNom(client.getNom());
            event.setRaison(raison);
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend("Notification-exchange", "rejection.send", event);
            log.info("Notification rejet envoyée pour: {}", client.getEmail());

        } catch (Exception e) {
            log.error("Erreur envoi notification rejet: {}", e.getMessage(), e);
        }
    }

    private void sendPasswordChangeNotification(Client client) {
        try {
            PasswordChangeNotificationEvent event = new PasswordChangeNotificationEvent();
            event.setClientId(client.getIdClient());
            event.setEmail(client.getEmail());
            event.setNom(client.getNom());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend("Notification-exchange", "password.change", event);
            log.info("Notification changement password envoyée pour: {}", client.getEmail());

        } catch (Exception e) {
            log.error("Erreur envoi notification password: {}", e.getMessage(), e);
        }
    }
}