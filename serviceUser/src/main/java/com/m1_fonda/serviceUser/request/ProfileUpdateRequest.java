package com.m1_fonda.serviceUser.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileUpdateRequest {
    @Email
    private String email;

    @Pattern(regexp = "^6[5-9]\\d{7}$")
    private String numero;

    @Size(min = 2, max = 50)
    private String nom;

    @Size(min = 2, max = 50)
    private String prenom;
}
