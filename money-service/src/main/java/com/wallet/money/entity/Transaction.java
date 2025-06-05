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
    private String clientId;              // Qui fait la transaction
    
    @Field("external_id")
    @Indexed(unique = true)
    private String externalId;            // Votre ID unique
    
    @Field("freemo_reference")
    @Indexed
    private String freemoReference;       // Référence FreemoPay
    
    @Field("amount")
    private BigDecimal amount;
    
    @Field("phone_number")
    private String phoneNumber;           // Numéro du payeur
    
    @Field("type")
    private String type;                  // DEPOSIT, WITHDRAWAL
    
    @Field("status")
    @Indexed
    private String status;                // PENDING, SUCCESS, FAILED, EXPIRED
    
    @Field("failure_reason")
    private String failureReason;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("expired_at")
    private LocalDateTime expiredAt;      // Expiration du lien de paiement
    
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
