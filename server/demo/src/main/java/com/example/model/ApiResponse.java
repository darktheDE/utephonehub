package com.example.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Map<String, String> errors;
    private int status;

    // Constructor for success
    private ApiResponse(boolean success, String message, T data, int status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
    }

    // Constructor for error
    private ApiResponse(boolean success, String message, int status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }

    // Constructor for validation error
    private ApiResponse(boolean success, String message, Map<String, String> errors, int status) {
        this.success = success;
        this.message = message;
        this.errors = errors;
        this.status = status;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, 200);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return new ApiResponse<>(false, message, status);
    }

    public static <T> ApiResponse<T> validationError(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, errors, 400);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Map<String, String> getErrors() { return errors; }
    public int getStatus() { return status; }
}