package com.m1_fonda.serviceUser.response;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;
}