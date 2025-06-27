package com.wallet.money.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

@Document(collection = "transactions")
@Data
public class Transaction {

    @Id
    private String id;

    @Field("client_id")
    @Indexed
    private String clientId; // Qui fait la transaction

    @Field("external_id")
    @Indexed(unique = true)
    private String externalId; // Votre ID unique

    @Field("freemo_reference")
    @Indexed
    private String freemoReference; // Référence FreemoPay

    @Field("amount")
    private BigDecimal amount;

    @Field("phone_number")
    private String phoneNumber; // Numéro du payeur

    @Field("type")
    private String type; // DEPOSIT, WITHDRAWAL

    @Field("status")
    @Indexed
    private String status; // PENDING, SUCCESS, FAILED, EXPIRED

    @Field("failure_reason")
    private String failureReason;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("expired_at")
    private LocalDateTime expiredAt;
    @Field("id_carte")
    private String idCarte; // ID de la carte concernée

    @Field("callback_url")
    private String callbackUrl; // URL de callback pour notifier le service Carte

    @Field("callback_retries")
    private int callbackRetries = 0; // Expiration du lien de paiement
    @Field("cancellation_reason")
    private String cancellationReason; // Raison de l'annulation

    @Field("client_action")
    private String clientAction; // "VALIDATED", "CANCELLED", "TIMEOUT"

    @Field("validation_timestamp")
    private LocalDateTime validationTimestamp;

    // Constructeur pour nouvelles transactions
    public static Transaction createDeposit(String clientId, String phoneNumber, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setId(null); // MongoDB génère automatiquement
        transaction.setClientId(clientId);
        transaction.setExternalId(generateExternalId(clientId));
        transaction.setPhoneNumber(phoneNumber);
        transaction.setAmount(amount);
        transaction.setType("DEPOSIT");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15min d'expiration
        return transaction;
    }

    private static String generateExternalId(String clientId) {
        return "DEP_" + clientId + "_" + System.currentTimeMillis();
    }

}
