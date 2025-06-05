package com.m1_fonda.serviceUser.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequest {
    @NotBlank
    private String cni;
    
    @Email
    private String email;
    
    @Pattern(regexp = "^6[5-9]\\d{7}$")
    private String numero;
    
    @NotBlank
    private String nom;
}