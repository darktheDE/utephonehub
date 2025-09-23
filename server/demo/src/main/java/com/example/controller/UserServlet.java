package com.example.controller;

import com.example.model.ChangePasswordRequest;
import com.example.model.User;
import com.example.model.UserRole;
import com.example.model.dto.AdminUserUpdateDTO;
import com.example.model.dto.ProfileUpdateDTO;
import com.example.model.dto.UserDTO;
import com.example.service.UserService;
import com.example.util.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/users/*")
public class UserServlet extends BaseServlet {

    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = (UserService) getServletContext().getAttribute("userService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        User currentUser = (User) request.getAttribute("user");

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/users -> Get all users (Admin only)
            handleGetAllUsers(request, response, currentUser);
        } else if (pathInfo.equals("/profile")) {
            // GET /api/users/profile -> Get current user's profile
            handleGetProfile(request, response);
        } else if (pathInfo.matches("/\\d+")) {
            // GET /api/users/{id} -> Get user by ID (Admin only)
            handleGetUserById(request, response, currentUser, pathInfo);
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }

    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
            return;
        }

        User currentUser = (User) request.getAttribute("user");

        if (pathInfo.equals("/profile")) {
            handleUpdateProfile(request, response);
        } else if (pathInfo.equals("/change-password")) {
            handleChangePassword(request, response);
        } else if (pathInfo.matches("/\\d+")) {
            // PUT /api/users/{id} -> Update user by ID (Admin only)
            handleAdminUpdateUser(request, response, currentUser, pathInfo);
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || !pathInfo.matches("/\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "User ID is required.");
            return;
        }

        User currentUser = (User) request.getAttribute("user");

        // DELETE /api/users/{id} -> Delete user by ID (Admin only)
        handleDeleteUser(request, response, currentUser, pathInfo);
    }

    private void handleGetProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            User currentUser = (User) request.getAttribute("user");
            // The user object from the filter might not be the most up-to-date.
            // It's better to fetch it again from the database.
            User user = userService.getUserById(currentUser.getId()).orElse(null);

            if (user == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }
            
            UserDTO userDTO = UserMapper.toUserDTO(user);
            sendSuccess(response, HttpServletResponse.SC_OK, "Profile retrieved successfully", userDTO);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            User currentUser = (User) request.getAttribute("user");
            ProfileUpdateDTO profileUpdateDTO = objectMapper.readValue(request.getReader(), ProfileUpdateDTO.class);
            if (!validateRequest(profileUpdateDTO, response)) return;

            // Get the most recent user data from DB
            User userToUpdate = userService.getUserById(currentUser.getId()).orElse(null);
            if (userToUpdate == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            // Update only allowed fields
            userToUpdate.setFullName(profileUpdateDTO.getFullName());
            userToUpdate.setPhoneNumber(profileUpdateDTO.getPhoneNumber());

            User updatedUser = userService.updateUser(userToUpdate);
            UserDTO userDTO = UserMapper.toUserDTO(updatedUser);

            sendSuccess(response, HttpServletResponse.SC_OK, "Profile updated successfully", userDTO);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            User currentUser = (User) request.getAttribute("user");
            ChangePasswordRequest changePasswordRequest = objectMapper.readValue(request.getReader(), ChangePasswordRequest.class);
            if (!validateRequest(changePasswordRequest, response)) return;

            boolean success = userService.changePassword(
                currentUser.getId(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
            );

            if (success) {
                sendSuccess(response, HttpServletResponse.SC_OK, "Password changed successfully", null);
            } else {
                // This path should ideally not be reached if exceptions are used for flow control
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to change password");
            }
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleGetAllUsers(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        if (!isAdmin(currentUser, response)) return;

        try {
            List<User> users = userService.getAllUsers();
            List<UserDTO> userDTOs = UserMapper.toUserDTOList(users);
            sendSuccess(response, HttpServletResponse.SC_OK, "Users retrieved successfully", userDTOs);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleGetUserById(HttpServletRequest request, HttpServletResponse response, User currentUser, String pathInfo) throws IOException {
        if (!isAdmin(currentUser, response)) return;

        Long userId = parseIdFromPath(pathInfo, response);
        if (userId == null) return;

        try {
            User user = userService.getUserById(userId).orElse(null);
            if (user == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }
            UserDTO userDTO = UserMapper.toUserDTO(user);
            sendSuccess(response, HttpServletResponse.SC_OK, "User retrieved successfully", userDTO);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleAdminUpdateUser(HttpServletRequest request, HttpServletResponse response, User currentUser, String pathInfo) throws IOException {
        if (!isAdmin(currentUser, response)) return;

        Long userId = parseIdFromPath(pathInfo, response);
        if (userId == null) return;

        try {
            AdminUserUpdateDTO updateDTO = objectMapper.readValue(request.getReader(), AdminUserUpdateDTO.class);
            User updatedUser = userService.adminUpdateUser(userId, updateDTO);
            UserDTO userDTO = UserMapper.toUserDTO(updatedUser);
            sendSuccess(response, HttpServletResponse.SC_OK, "User updated successfully by admin", userDTO);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleDeleteUser(HttpServletRequest request, HttpServletResponse response, User currentUser, String pathInfo) throws IOException {
        if (!isAdmin(currentUser, response)) return;

        Long userId = parseIdFromPath(pathInfo, response);
        if (userId == null) return;

        if (currentUser.getId().equals(userId)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Admin cannot delete their own account.");
            return;
        }

        try {
            boolean deleted = userService.deleteUser(userId);
            if (deleted) {
                sendSuccess(response, HttpServletResponse.SC_OK, "User deleted successfully", null);
            } else {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found or could not be deleted");
            }
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private boolean isAdmin(User user, HttpServletResponse response) throws IOException {
        if (user.getRole() != UserRole.ADMIN) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied. Admin role required.");
            return false;
        }
        return true;
    }

    private Long parseIdFromPath(String pathInfo, HttpServletResponse response) throws IOException {
        try {
            return Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException | NullPointerException | StringIndexOutOfBoundsException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format.");
            return null;
        }
    }
}