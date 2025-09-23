package com.example.service;

import com.example.config.AppConfig;
import com.example.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final SecretKey secretKey;
    private final long jwtExpiration;
    private final long jwtRefreshExpiration;

    private UserService userService;

    public AuthService(UserService userService, AppConfig config) {
        this.userService = userService;
        String secretString = config.getProperty("jwt.secret", "defaultSecretKeyThatIsLongEnoughForHS256AndShouldBeChanged");
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes());
        this.jwtExpiration = Long.parseLong(config.getProperty("jwt.expiration.ms", "86400000")); // 24 hours default
        this.jwtRefreshExpiration = Long.parseLong(config.getProperty("jwt.refresh.expiration.ms", "604800000")); // 7 days default
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(jwtRefreshExpiration / 1000));
        userService.updateUser(user);
        return refreshToken;
    }

    public Map<String, String> refreshAccessToken(String refreshToken) {
        User user = userService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            // Clear expired token
            userService.clearRefreshToken(user.getId());
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        String newAccessToken = generateAccessToken(user);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        return tokens;
    }

    public void logoutUser(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return; // No token provided, nothing to do
        }
        userService.findByRefreshToken(refreshToken).ifPresent(user -> {
            userService.clearRefreshToken(user.getId());
            logger.info("User {} logged out successfully.", user.getEmail());
        });
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
        return claimsJws.getPayload();
    }

    public User getUserFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Long userId = claims.get("userId", Long.class);
            return userService.getUserById(userId).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting user from token", e);
            return null;
        }
    }
}