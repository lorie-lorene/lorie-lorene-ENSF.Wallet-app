package com.serviceAgence.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequestEvent {
    private String eventId;
    private String idClient;
    private String cni;
    private String email;
    private String numero;
    private String nom;
    private LocalDateTime timestamp;
}