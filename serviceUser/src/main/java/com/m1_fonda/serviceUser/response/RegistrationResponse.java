package com.m1_fonda.serviceUser.response;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponse {
    private String probleme;
    private String statut;
    private String email;
    private Long numeroCompte;
    private String idClient;
    private String idAgence;
    private LocalDateTime timestamp;
}