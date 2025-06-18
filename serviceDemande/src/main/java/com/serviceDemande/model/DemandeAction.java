package com.serviceDemande.model;

import java.time.LocalDateTime;

import com.serviceDemande.enums.ActionType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DemandeAction {
    private ActionType actionType;
    private String description;
    private String performedBy;
    private LocalDateTime timestamp;
}
