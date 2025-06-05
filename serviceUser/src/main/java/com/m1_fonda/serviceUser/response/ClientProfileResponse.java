package com.m1_fonda.serviceUser.response;
import java.time.LocalDateTime;

import com.m1_fonda.serviceUser.pojo.ClientStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProfileResponse {
    private String idClient;
    private String nom;
    private String prenom;
    private String email;
    private String numero;
    private ClientStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
