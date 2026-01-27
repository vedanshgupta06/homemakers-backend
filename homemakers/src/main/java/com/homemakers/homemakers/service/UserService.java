package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.LoginResponse;
import com.homemakers.homemakers.dto.ProviderRegisterRequest;
import com.homemakers.homemakers.dto.RegisterRequest;
import com.homemakers.homemakers.model.RefreshToken;
import com.homemakers.homemakers.model.Role;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.RefreshTokenRepository;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // =========================
    // ✅ PUBLIC REGISTER (USER)
    // =========================
    public User registerUser(RegisterRequest request) {

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    // =========================
    // ✅ LOGIN (JWT + REFRESH)
    // =========================
    public LoginResponse login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        // one refresh token per user
        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiry(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken.getToken(),
                user.getEmail(),
                user.getRole().name()
        );
    }



    // =========================
    // 🔁 REFRESH ACCESS TOKEN
    // =========================
    public LoginResponse refreshAccessToken(String refreshTokenValue) {

        RefreshToken oldToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (oldToken.isRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (oldToken.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = oldToken.getUser(); // ✅ get user FIRST

        // ❌ delete old token
        refreshTokenRepository.delete(oldToken);

        // 🔐 new access token
        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        // 🔁 rotate refresh token
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(UUID.randomUUID().toString());
        newRefreshToken.setUser(user);
        newRefreshToken.setExpiry(LocalDateTime.now().plusDays(7));
        newRefreshToken.setRevoked(false);

        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                user.getEmail(),
                user.getRole().name()
        );
    }


    // =========================
    // 🚪 LOGOUT
    // =========================
    public void logout(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    // =========================
    // 🔐 ADMIN → CREATE PROVIDER
    // =========================
    public User createProvider(
            RegisterRequest request,
            String adminEmail
    ) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only ADMIN can create PROVIDER");
        }

        User provider = new User();
        provider.setName(request.getName());
        provider.setEmail(request.getEmail());
        provider.setPassword(passwordEncoder.encode(request.getPassword()));
        provider.setRole(Role.PROVIDER);

        return userRepository.save(provider);
    }
}
