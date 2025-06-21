package com.serviceAgence.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetResponseEvent {
    private String eventId;
    private String cni;
    private String newPassword;
    private String agence;
    private String email;
    private LocalDateTime timestamp;
    private String targetService = "UserService";
}