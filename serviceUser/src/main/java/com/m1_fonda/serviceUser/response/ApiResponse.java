package com.m1_fonda.serviceUser.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 📋 Standardized API Response for ServiceUser
 * 
 * Provides consistent response format for all API endpoints
 * Compatible with AgenceService dashboard expectations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;
    private String path;
    
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
     * Create successful response with data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with path
     */
    public static <T> ApiResponse<T> error(String errorMessage, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorMessage)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create simple success response without data
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create simple success response with message only
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}