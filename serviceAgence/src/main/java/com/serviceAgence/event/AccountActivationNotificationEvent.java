package com.serviceAgence.event;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountActivationNotificationEvent {
    private String eventId;
    private String idClient;
    private Long numeroCompte;
    private LocalDateTime activatedAt;
    private LocalDateTime timestamp;
}