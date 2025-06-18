package com.serviceAgence.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudAlertNotificationEvent {
    private String eventId;
    private String idClient;
    private String alertType;
    private String details;
    private LocalDateTime timestamp;
}
