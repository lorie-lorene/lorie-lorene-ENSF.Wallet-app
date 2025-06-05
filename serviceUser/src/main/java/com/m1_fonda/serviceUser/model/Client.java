package com.m1_fonda.serviceUser.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.m1_fonda.serviceUser.pojo.ClientStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    private String idClient;

    @Indexed(unique = true)
    @NotBlank(message = "CNI obligatoire")
    @Pattern(regexp = "\\d{8,12}", message = "Format CNI invalide")
    private String cni;

    @Indexed(unique = true)
    @Email(message = "Email invalide")
    @NotBlank(message = "Email obligatoire")
    private String email;

    @Indexed(unique = true)
    @Pattern(regexp = "^6[5-9]\\d{7}$", message = "Numéro téléphone camerounais invalide")
    private String numero;

    @NotBlank(message = "Nom obligatoire")
    @Size(min = 2, max = 50)
    private String nom;

    @NotBlank(message = "Prénom obligatoire")
    @Size(min = 2, max = 50)
    private String prenom;

    private String idAgence;

    // SÉCURITÉ : Password hashé + métadonnées
    @JsonIgnore // Jamais exposé dans JSON
    private String passwordHash;

    @JsonIgnore
    private String salt; // Sel additionnel

    private String rectoCni;
    private String versoCni;

    // Métadonnées de sécurité
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private LocalDateTime passwordChangedAt;

    private ClientStatus status; // PENDING, ACTIVE, SUSPENDED, BLOCKED

    private int loginAttempts = 0;
    private LocalDateTime lastFailedLogin;

    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ClientStatus.PENDING;
        }
    }
}
