package com.m1_fonda.serviceUser.pojo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Base64;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRegistrationDTO {
    @NotBlank(message = "CNI obligatoire")
    private String cni;
    
    @Email(message = "Email invalide")
    private String email;
    
    @NotBlank(message = "Nom obligatoire")
    private String nom;
    
    @NotBlank(message = "Prénom obligatoire")
    private String prenom;
    
    @Pattern(regexp = "^6[5-9]\\d{7}$")
    private String numero;
    
    @NotBlank(message = "Mot de passe obligatoire")
    @Size(min = 8, message = "Mot de passe minimum 8 caractères")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&].*$", 
             message = "Mot de passe doit contenir : majuscule, minuscule, chiffre, caractère spécial")
    private String password;
    
    private String idAgence;
    private String rectoCni;
    private String versoCni;

   /**
     * Selfie image for biometric verification (Base64 encoded)
     * This field works with the existing Client.selfieImage field
     */
    @NotBlank(message = "La photo selfie est obligatoire pour la vérification d'identité")
    @Size(min = 1000, message = "La photo selfie semble trop petite")
    private String selfieImage;

    // Getter and Setter
    public String getSelfieImage() {
        return selfieImage;
    }

    public void setSelfieImage(String selfieImage) {
        this.selfieImage = selfieImage;
    }
    /**
     * Validation du format du selfie
     */
    @AssertTrue(message = "Format de selfie invalide (doit être Base64)")
    public boolean isSelfieValid() {
        if (selfieImage == null || selfieImage.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Vérifier que c'est bien du Base64 valide
            String base64Data = selfieImage;
            if (base64Data.startsWith("data:image/")) {
                // Extraire les données après "base64,"
                int commaIndex = base64Data.indexOf(",");
                if (commaIndex > 0) {
                    base64Data = base64Data.substring(commaIndex + 1);
                }
            }
            
            byte[] decoded = Base64.getDecoder().decode(base64Data);
            
            // Vérifier taille minimale (20KB) et maximale (10MB)
            return decoded.length >= 20 * 1024 && decoded.length <= 10 * 1024 * 1024;
            
        } catch (Exception e) {
            return false;
        }
    }

}