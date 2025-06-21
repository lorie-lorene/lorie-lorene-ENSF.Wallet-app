package com.serviceAnnonce.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUser {
    private String cni;
    private String Email;
    private String newPw;
    private String agence;
}
