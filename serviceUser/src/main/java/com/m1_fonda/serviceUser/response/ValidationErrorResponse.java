package com.m1_fonda.serviceUser.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorResponse {
    private String type;
    private String message;
    private Map<String, String> fieldErrors;
    private LocalDateTime timestamp;
}