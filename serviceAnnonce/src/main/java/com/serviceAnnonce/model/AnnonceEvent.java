package com.serviceAnnonce.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
/* corps de l'evenement pour l'envoie des annonces vers les utilisateurs */
public class AnnonceEvent {
    private int id;
    @JsonProperty("description")
    private String description;
    @JsonProperty("titre")
    private String titre;
    @JsonProperty("id_agence")
    private String id_agence;
    @JsonProperty("id_client")
    private String id_client;
    @JsonProperty("email")
    private String email;

}
