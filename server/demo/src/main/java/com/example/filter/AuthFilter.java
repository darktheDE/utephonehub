package com.example.filter;

import com.example.model.ApiResponse;
import com.example.model.User;
import com.example.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// Filter mapping is defined in web.xml to control execution order
public class AuthFilter implements Filter {
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> excludedPaths = Arrays.asList("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Get AuthService from servlet context
        this.authService = (AuthService) filterConfig.getServletContext().getAttribute("authService");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // Skip filter for excluded paths
        if (excludedPaths.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authorization header is missing or invalid");
            return;
        }

        String token = authHeader.substring(7);
        if (!authService.validateToken(token)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        User user = authService.getUserFromToken(token);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not found for the given token");
            return;
        }

        // Attach user to the request for downstream servlets
        request.setAttribute("user", user);
        chain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message, status)));
    }
}