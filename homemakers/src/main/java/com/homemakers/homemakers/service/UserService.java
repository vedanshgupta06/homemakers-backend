package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.LoginResponse;
import com.homemakers.homemakers.dto.RegisterRequest;
import com.homemakers.homemakers.dto.UserProfileDto;
import com.homemakers.homemakers.model.*;
import com.homemakers.homemakers.repository.*;
import com.homemakers.homemakers.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository; // ✅ FIXED

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            ProviderRepository providerRepository,
            BookingRepository bookingRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository; // ✅ FIXED
    }

    // =====================================================
    // ✅ REGISTER
    // =====================================================
    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCity(request.getCity());

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
    // ✅ LOGIN
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
    // 🔁 REFRESH TOKEN
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
    // 👤 PROFILE
    // =====================================================
    public UserProfileDto getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setCity(user.getCity());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole().name());

        return dto;
    }

    public UserProfileDto updateProfile(String email, UserProfileDto req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(req.getName());
        user.setCity(req.getCity());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());

        userRepository.save(user);

        return getProfile(email);
    }

    // =====================================================
    // 📊 DASHBOARD STATS
    // =====================================================
    public int getTotalBookings(String email) {
        return bookingRepository.countByUserEmail(email);
    }

    public int getUpcomingBookings(String email) {
        return bookingRepository.countByUserEmailAndStatus(
                email,
                BookingStatus.CONFIRMED
        );
    }
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    public List<Map<String, Object>> getRecentActivity(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Map<String, Object>> activity = new ArrayList<>();

        // 🟢 BOOKINGS
        List<Booking> bookings =
                bookingRepository.findTop5ByUserOrderByCreatedAtDesc(user);

        for (Booking b : bookings) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "BOOKING");
            map.put("message", "Booking #" + b.getId() + " " + b.getStatus());
            map.put("time", b.getCreatedAt());
            activity.add(map);
        }

        // 🟢 PAYMENTS (✅ FIXED)
        List<PaymentTransaction> payments =
                paymentTransactionRepository
                        .findTop5ByUserIdOrderByCreatedAtDesc(user.getId());

        for (PaymentTransaction p : payments) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "PAYMENT");
            map.put("message", "Payment ₹" + p.getAmount() + " " + p.getStatus());
            map.put("time", p.getCreatedAt());
            activity.add(map);
        }

        // 🔥 SORT BY TIME DESC
        activity.sort((a, b) ->
                ((LocalDateTime) b.get("time"))
                        .compareTo((LocalDateTime) a.get("time"))
        );

        return activity.stream().limit(5).toList();
    }
}