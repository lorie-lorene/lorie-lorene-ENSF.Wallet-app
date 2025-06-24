package com.serviceAgence.dto.admin;

import com.serviceAgence.enums.UserRole;
import com.serviceAgence.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    private String id;
    private String username;
    private String email;
    private String temporaryPassword;
    private Set<UserRole> roles;
    private UserStatus status;
    private String message;
}
