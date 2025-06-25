package com.serviceAgence.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📦 Standardized API Response Wrapper
 * 
 * Provides consistent response format across all endpoints
 * Matches frontend expectations: { success: boolean, data: object, error: string }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Success indicator - matches frontend expectation
     */
    private boolean success;
    
    /**
     * Response data - only present on success
     */
    private T data;
    
    /**
     * Error message - only present on failure
     */
    private String error;
    
    /**
     * Error details - for debugging (optional)
     */
    private String details;
    
    /**
     * Response timestamp
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // =====================================
    // STATIC FACTORY METHODS
    // =====================================
    
    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create successful response without data
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with message
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with message and details
     */
    public static <T> ApiResponse<T> error(String errorMessage, String details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorMessage)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response from exception
     */
    public static <T> ApiResponse<T> error(Exception exception) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(exception.getMessage())
                .details(exception.getClass().getSimpleName())
                .timestamp(LocalDateTime.now())
                .build();
    }
}