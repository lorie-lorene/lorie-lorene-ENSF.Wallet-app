package com.serviceAnnonce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
/* corps d'une annonce */
public class Annonce {
    @Id
    private String id;
    private String description;
    private String titre;
    private String id_agence;
    private String email_Client;
    private double montant;
    private String type;

}
