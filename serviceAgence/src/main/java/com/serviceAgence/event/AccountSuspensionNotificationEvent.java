package com.serviceAgence.event;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountSuspensionNotificationEvent {
    private String eventId;
    private String idClient;
    private Long numeroCompte;
    private String reason;
    private LocalDateTime suspendedAt;
    private LocalDateTime timestamp;
}
