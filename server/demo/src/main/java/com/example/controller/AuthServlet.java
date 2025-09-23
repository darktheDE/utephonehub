package com.example.controller;

import com.example.model.*;
import com.example.model.dto.LoginResponseDTO;
import com.example.model.dto.RefreshTokenRequestDTO;
import com.example.model.dto.UserDTO;
import com.example.service.AuthService;
import com.example.service.UserService;
import com.example.util.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);

    private UserService userService;
    private AuthService authService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = (UserService) getServletContext().getAttribute("userService");
        this.authService = (AuthService) getServletContext().getAttribute("authService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/verify".equals(pathInfo)) {
            // The AuthFilter has already validated the token and set the user attribute.
            User currentUser = (User) request.getAttribute("user");
            if (currentUser != null) {
                UserDTO userDTO = UserMapper.toUserDTO(currentUser);
                sendSuccess(response, HttpServletResponse.SC_OK, "Token is valid", userDTO);
            } else {
                // This case should technically not be reached if AuthFilter is working correctly
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid session");
            }
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        System.out.println("AuthServlet doPost - pathInfo: " + pathInfo); // Debug print

        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
        } else if (pathInfo.startsWith("/register")) {
            handleRegister(request, response);
        } else if (pathInfo.startsWith("/login")) {
            handleLogin(request, response);
        } else if (pathInfo.startsWith("/refresh")) {
            handleRefresh(request, response);
        } else if (pathInfo.startsWith("/logout")) {
            // Although logout is primarily a client-side action (deleting the token),
            // a server-side endpoint is good practice to invalidate refresh tokens.
            handleLogout(request, response);
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Not Found");
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RegisterRequest registerRequest = objectMapper.readValue(request.getReader(), RegisterRequest.class);
            if (!validateRequest(registerRequest, response)) return;

            User user = userService.registerUser(
                registerRequest.getFullName(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
            );

            UserDTO userDTO = UserMapper.toUserDTO(user);

            sendSuccess(response, HttpServletResponse.SC_CREATED, "User registered successfully", userDTO);

        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("An error occurred during registration.", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred: " + e.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LoginRequest loginRequest = objectMapper.readValue(request.getReader(), LoginRequest.class);
            if (!validateRequest(loginRequest, response)) return;

            User user = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
            String accessToken = authService.generateAccessToken(user);
            String refreshToken = authService.generateRefreshToken(user);

            UserDTO userDTO = UserMapper.toUserDTO(user);
            LoginResponseDTO loginResponse = new LoginResponseDTO(userDTO, accessToken, refreshToken);

            sendSuccess(response, HttpServletResponse.SC_OK, "Login successful", loginResponse);

        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            logger.error("An error occurred during login.", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred: " + e.getMessage());
        }
    }

    private void handleRefresh(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RefreshTokenRequestDTO refreshTokenRequest = objectMapper.readValue(request.getReader(), RefreshTokenRequestDTO.class);
            Map<String, String> tokens = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
            sendSuccess(response, HttpServletResponse.SC_OK, "Token refreshed successfully", tokens);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            logger.error("An error occurred during token refresh.", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RefreshTokenRequestDTO refreshTokenRequest = objectMapper.readValue(request.getReader(), RefreshTokenRequestDTO.class);
            authService.logoutUser(refreshTokenRequest.getRefreshToken());
            sendSuccess(response, HttpServletResponse.SC_OK, "Logged out successfully", null);
        } catch (Exception e) {
            // We don't want to fail the logout process if the request is malformed.
            // Just log it and send a success response.
            logger.warn("Logout request might be malformed or token was already invalid.", e);
            sendSuccess(response, HttpServletResponse.SC_OK, "Logged out successfully", null);
        }
    }
}
