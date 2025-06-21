package com.serviceAnnonce.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
/* reception de la reponse d'une demande venant de l'agence */
public class Response {
    private String probleme;
    private String statut;
    private String email;
    private String id_client;
    private String id_agence;
    private Long numero;
}
