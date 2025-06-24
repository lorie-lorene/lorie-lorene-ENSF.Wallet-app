package com.serviceAgence.dto.admin;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private String id;
    private String username;
    private String email;
    private String nom;
    private String prenom;
    private Set<UserRole> roles;
    private UserStatus status;
    private String idAgence;
    private String nomAgence;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
