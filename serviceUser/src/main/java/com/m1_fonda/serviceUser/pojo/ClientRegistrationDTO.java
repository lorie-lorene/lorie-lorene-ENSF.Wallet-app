package com.m1_fonda.serviceUser.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}