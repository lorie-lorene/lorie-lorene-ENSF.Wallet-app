package com.m1_fonda.serviceUser.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetEvent {
    private String eventId;
    private String clientId;
    private String cni;
    private String email;
    private String numero;
    private String nom;
    private LocalDateTime timestamp;
}