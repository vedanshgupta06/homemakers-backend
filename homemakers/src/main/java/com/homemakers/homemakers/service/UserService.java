package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.LoginResponse;
import com.homemakers.homemakers.dto.RegisterRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.RefreshToken;
import com.homemakers.homemakers.model.Role;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.ProviderRepository;
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
    private final ProviderRepository providerRepository;
    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            ProviderRepository providerRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.providerRepository = providerRepository;
    }

    // =====================================================
    // ✅ PUBLIC USER REGISTRATION
    // =====================================================
    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = "PROVIDER".equalsIgnoreCase(request.getRole())
                ? Role.PROVIDER
                : Role.USER;

        user.setRole(role);

        userRepository.save(user);

        if (role == Role.PROVIDER) {

            Provider provider = new Provider();

            provider.setUser(user);
            provider.setVerified(false);
            provider.setRating(0.0);
            provider.setTotalRatings(0);

            providerRepository.save(provider);
        }

        return user;
    }

    // =====================================================
    // ✅ LOGIN (JWT + REFRESH TOKEN)
    // =====================================================
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

        // ensure one refresh token per user
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

    // =====================================================
    // 🔁 REFRESH ACCESS TOKEN
    // =====================================================
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

        User user = oldToken.getUser();

        // remove old refresh token
        refreshTokenRepository.delete(oldToken);

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

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

    // =====================================================
    // 🚪 LOGOUT
    // =====================================================
    public void logout(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    // =====================================================
    // 🔐 ADMIN → CREATE PROVIDER
    // =====================================================
    public User createProvider(RegisterRequest request, String adminEmail) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only ADMIN can create PROVIDER");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User provider = new User();
        provider.setName(request.getName());
        provider.setEmail(request.getEmail());
        provider.setPassword(passwordEncoder.encode(request.getPassword()));
        provider.setRole(Role.PROVIDER);

        return userRepository.save(provider);
    }
}