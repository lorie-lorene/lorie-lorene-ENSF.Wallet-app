package com.serviceAgence.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationRequest {
    @NotBlank
    private String idClient;

    @NotBlank
    private String idAgence;

    @NotBlank
    private String cni;

    @Email
    private String email;

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotBlank
    private String numero;

    private byte[] rectoCni;
    private byte[] versoCni;

    /**
     * Image selfie de l'utilisateur (décodée depuis Base64)
     */
    private byte[] selfieImage;

    /**
     * Vérifier si le selfie est présent
     */
    public boolean hasSelfie() {
        return selfieImage != null && selfieImage.length > 0;
    }

    /**
     * Obtenir la taille du selfie en bytes
     */
    public long getSelfieSize() {
        return selfieImage != null ? selfieImage.length : 0;
    }
}
