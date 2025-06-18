package com.serviceAgence.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.serviceAgence.enums.AgenceStatus;
import com.serviceAgence.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "agences")
@NoArgsConstructor
@Data
@AllArgsConstructor
public class Agence {
    @Id
    private String idAgence;

    @Indexed(unique = true)
    @NotBlank(message = "Code agence obligatoire")
    @Size(min = 3, max = 10)
    private String codeAgence;

    @NotBlank(message = "Nom agence obligatoire")
    @Size(min = 2, max = 100)
    private String nom;

    @NotBlank(message = "Adresse obligatoire")
    private String adresse;

    @NotBlank(message = "Ville obligatoire")
    private String ville;

    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Téléphone obligatoire")
    private String telephone;

    // SÉCURITÉ : Credentials hashés
    @NotBlank(message = "Login obligatoire")
    private String login;

    private String passwordHash;
    private String salt;

    // Informations financières
    @NotNull
    @DecimalMin(value = "0.0", message = "Capital ne peut être négatif")
    private BigDecimal capital;

    @NotNull
    @DecimalMin(value = "0.0", message = "Solde ne peut être négatif")
    private BigDecimal soldeDisponible;

    // Configuration des frais (en pourcentage)
    private Map<TransactionType, BigDecimal> tauxFrais;

    // Configuration des limites
    private BigDecimal limiteDailyTransactions = new BigDecimal("50000000"); // 50M FCFA
    private BigDecimal limiteMonthlyTransactions = new BigDecimal("500000000"); // 500M FCFA

    // Métadonnées
    private AgenceStatus status = AgenceStatus.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private String createdBy;

    // Statistiques
    private Long totalComptes = 0L;
    private Long totalTransactions = 0L;
    private BigDecimal totalVolume = BigDecimal.ZERO;

    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AgenceStatus.ACTIVE;
        }
    }

    // Méthodes utilitaires
    public BigDecimal getTauxFrais(TransactionType type) {
        return tauxFrais != null ? tauxFrais.getOrDefault(type, BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    public boolean isActive() {
        return status == AgenceStatus.ACTIVE;
    }

    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}
