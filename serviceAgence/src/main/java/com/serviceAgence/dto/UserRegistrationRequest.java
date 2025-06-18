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
}
