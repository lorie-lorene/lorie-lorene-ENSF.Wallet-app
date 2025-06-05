package com.m1_fonda.serviceUser.event;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectionNotificationEvent {
    private String clientId;
    private String email;
    private String nom;
    private String raison;
    private LocalDateTime timestamp;
}