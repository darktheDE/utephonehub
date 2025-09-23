package com.example.service;

import com.example.model.dto.AdminUserUpdateDTO;
import com.example.model.User;
import com.example.model.UserStatus;
import com.example.repository.UserRepository;
import com.example.util.PasswordValidator;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String fullName, String email, String password) {
        if (!PasswordValidator.isValid(password)) {
            throw new IllegalArgumentException(PasswordValidator.getValidationMessage());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Create user
        User user = new User(fullName, email, hashedPassword, null);
        return userRepository.save(user);
    }

    public User authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userOpt.get();

        // Check if account is disabled
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new IllegalArgumentException("Account is disabled");
        }
        
        // Check if account is locked
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Account is temporarily locked due to too many failed login attempts. Please try again later.");
            } else {
                // If lock time has expired, unlock the account
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // Lock account if 5 or more failed attempts
            if (user.getFailedLoginAttempts() >= 5) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                userRepository.save(user);
                throw new IllegalArgumentException("Account has been locked due to too many failed login attempts. Please try again in 15 minutes.");
            } else {
                userRepository.save(user);
                throw new IllegalArgumentException("Invalid email or password");
            }
        }

        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return user;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByRefreshToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return userRepository.findByRefreshToken(token);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        // Check if email is being changed and if it already exists
        Optional<User> existingUserOpt = userRepository.findById(user.getId());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (!existingUser.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(user.getEmail())) {
                    throw new IllegalArgumentException("Email already exists");
                }
            }
        }

        return userRepository.save(user);
    }

    public User adminUpdateUser(Long userId, AdminUserUpdateDTO dto) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (dto.getFullName() != null) {
            userToUpdate.setFullName(dto.getFullName());
        }
        if (dto.getPhoneNumber() != null) {
            userToUpdate.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getRole() != null) {
            userToUpdate.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            userToUpdate.setStatus(dto.getStatus());
            if(dto.getStatus() == UserStatus.ACTIVE) {
                // Reset lock state if admin reactivates
                userToUpdate.setFailedLoginAttempts(0);
                userToUpdate.setLockedUntil(null);
            }
        }
        return userRepository.save(userToUpdate);
    }

    public void clearRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        
        userRepository.save(user);
    }

    public boolean deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        try {
            userRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        if (!PasswordValidator.isValid(newPassword)) {
            throw new IllegalArgumentException(PasswordValidator.getValidationMessage());
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        if (!BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(hashedNewPassword);
        userRepository.save(user);

        return true;
    }
}