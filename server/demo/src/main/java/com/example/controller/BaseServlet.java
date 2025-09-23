package com.example.controller;

import com.example.model.ApiResponse;
import com.example.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseServlet extends HttpServlet {

    protected ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.objectMapper = new ObjectMapper();
        // Register module to handle Java 8 date/time types (e.g., LocalDateTime)
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    protected void sendSuccess(HttpServletResponse response, int status, String message, Object data) throws IOException {
        response.setStatus(status);
        sendJsonResponse(response, ApiResponse.success(message, data));
    }

    protected void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        sendJsonResponse(response, ApiResponse.error(message, status));
    }

    protected boolean validateRequest(Object dto, HttpServletResponse response) throws IOException {
        Validator validator = ValidationUtil.getValidator();
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            Map<String, String> errors = violations.stream()
                    .collect(Collectors.toMap(
                            v -> v.getPropertyPath().toString(),
                            ConstraintViolation::getMessage
                    ));
            
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.validationError("Validation failed", errors)));
            return false;
        }
        return true;
    }

    private void sendJsonResponse(HttpServletResponse response, ApiResponse<?> apiResponse) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(apiResponse));
        out.flush();
    }
}