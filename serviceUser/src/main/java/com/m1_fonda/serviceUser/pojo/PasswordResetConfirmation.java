package com.m1_fonda.serviceUser.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetConfirmation {
    private String cni;
    private String newPassword;
    private String agence;
    private String email;
}
