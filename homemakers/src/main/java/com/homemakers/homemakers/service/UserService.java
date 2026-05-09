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

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtil                jwtUtil;
    private final ProviderRepository     providerRepository;
    private final BookingRepository      bookingRepository;

    @Autowired
    private UserWalletTransactionRepository walletTransactionRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            ProviderRepository providerRepository,
            BookingRepository bookingRepository
    ) {
        this.userRepository         = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder        = passwordEncoder;
        this.jwtUtil                = jwtUtil;
        this.providerRepository     = providerRepository;
        this.bookingRepository      = bookingRepository;
    }

    // =====================================================
    // REGISTER
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
                ? Role.PROVIDER : Role.USER;
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
    // LOGIN
    // =====================================================
    public LoginResponse login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiry(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(accessToken, refreshToken.getToken(),
                user.getEmail(), user.getRole().name());
    }

    // =====================================================
    // REFRESH TOKEN
    // =====================================================
    public LoginResponse refreshAccessToken(String refreshTokenValue) {

        RefreshToken oldToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (oldToken.isRevoked())                              throw new RuntimeException("Refresh token revoked");
        if (oldToken.getExpiry().isBefore(LocalDateTime.now())) throw new RuntimeException("Refresh token expired");

        User user = oldToken.getUser();
        refreshTokenRepository.delete(oldToken);

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(UUID.randomUUID().toString());
        newRefreshToken.setUser(user);
        newRefreshToken.setExpiry(LocalDateTime.now().plusDays(7));
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(newAccessToken, newRefreshToken.getToken(),
                user.getEmail(), user.getRole().name());
    }

    // =====================================================
    // LOGOUT
    // =====================================================
    public void logout(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    // =====================================================
    // PROFILE
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
        dto.setPincode(user.getPincode());
        dto.setLatitude(user.getLatitude());
        dto.setLongitude(user.getLongitude());
        return dto;
    }

    public UserProfileDto updateProfile(String email, UserProfileDto req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(req.getName());
        user.setCity(req.getCity());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        user.setPincode(req.getPincode());
        user.setLatitude(req.getLatitude());
        user.setLongitude(req.getLongitude());
        userRepository.save(user);

        return getProfile(email);
    }

    // =====================================================
    // DASHBOARD STATS
    // =====================================================
    public int getTotalBookings(String email) {
        return bookingRepository.countByUserEmail(email);
    }

    public int getUpcomingBookings(String email) {
        return bookingRepository.countByUserEmailAndStatus(email, BookingStatus.CONFIRMED);
    }

    // =====================================================
    // RECENT ACTIVITY
    // ✅ Each activity item is a typed map so the frontend can
    //    render fields independently (e.g. reason on its own line).
    //
    // Shape per type:
    //   BOOKING  → { type, bookingId, status, message, terminationReason?, time }
    //   PAYMENT  → { type, message, time }
    //   REFUND   → { type, message, time }
    // =====================================================
    public List<Map<String, Object>> getRecentActivity(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Map<String, Object>> activity = new ArrayList<>();

        // ── Bookings (top 5) ──────────────────────────────────────────────────
        List<Booking> bookings = bookingRepository.findTop5ByUserOrderByCreatedAtDesc(user);
        for (Booking b : bookings) {
            Map<String, Object> item = new HashMap<>();
            item.put("type",      "BOOKING");
            item.put("bookingId", b.getId());
            item.put("status",    b.getStatus().name());
            item.put("time",      b.getCreatedAt());

            String message;
            String link;

            switch (b.getStatus()) {
                case CONFIRMED -> {
                    if (b.getPaymentStatus() == PaymentStatus.PAYMENT_REQUIRED) {
                        message = "Booking #" + b.getId() + " accepted by provider — payment required!";
                        link    = "/user/payments";
                    } else {
                        message = "Booking #" + b.getId() + " confirmed — payment done.";
                        link    = "/user/bookings/" + b.getId();
                    }
                }
                case PENDING -> {
                    message = "Booking #" + b.getId() + " is waiting for provider to accept.";
                    link    = "/user/bookings/" + b.getId();
                }
                case SERVICE_IN_PROGRESS -> {
                    message = "Booking #" + b.getId() + " is in progress.";
                    link    = "/user/bookings/" + b.getId();
                }
                case COMPLETED -> {
                    message = "Booking #" + b.getId() + " completed successfully.";
                    link    = "/user/bookings/" + b.getId();
                }
                case TERMINATED -> {
                    message = "Booking #" + b.getId() + " was terminated early.";
                    link    = "/user/bookings/" + b.getId();
                }
                case CANCELLED -> {
                    message = "Booking #" + b.getId() + " was cancelled.";
                    link    = "/user/bookings/" + b.getId();
                }
                default -> {
                    message = "Booking #" + b.getId() + " — " + b.getStatus();
                    link    = "/user/bookings/" + b.getId();
                }
            }

            item.put("message", message);
            item.put("link",    link);

            if (b.getStatus() == BookingStatus.TERMINATED
                    && b.getTerminationReason() != null
                    && !b.getTerminationReason().isBlank()) {
                item.put("terminationReason", b.getTerminationReason());
            }

            activity.add(item);
        }

        // ── Payments (top 5) ──────────────────────────────────────────────────
        List<PaymentTransaction> payments =
                paymentTransactionRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
        for (PaymentTransaction p : payments) {
            Map<String, Object> item = new HashMap<>();
            item.put("type",    "PAYMENT");
            item.put("message", "Payment ₹" + p.getAmount() + " — " + p.getStatus());
            item.put("time",    p.getCreatedAt());
            item.put("link",    "/user/payments/history");
            activity.add(item);
        }

        // ── Refunds (top 5) ───────────────────────────────────────────────────
        List<UserWalletTransaction> refunds =
                walletTransactionRepository.findTop5ByUserIdAndTypeOrderByCreatedAtDesc(
                        user.getId(), TransactionType.REFUND);
        for (UserWalletTransaction r : refunds) {
            Map<String, Object> item = new HashMap<>();
            item.put("type",    "REFUND");
            item.put("message", "Refund ₹" + r.getAmount() + " — " + r.getDescription());
            item.put("time",    r.getCreatedAt());
            item.put("link",    "/user/wallet");
            activity.add(item);
        }

        // ── Sort newest first, cap at 5 ───────────────────────────────────────
        activity.sort((a, b) ->
                ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time"))
        );

        return activity.stream().limit(5).toList();
    }

    // =====================================================
    // UNREAD NOTIFICATION COUNT
    // =====================================================
    public long getUnreadNotificationCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime since = LocalDateTime.now().minusHours(24);

        long bookingCount = bookingRepository.countByUserAndStatusInAndUpdatedAtAfter(
                user,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.TERMINATED),
                since
        );

        long refundCount = walletTransactionRepository
                .findTop5ByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), TransactionType.REFUND)
                .stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(since))
                .count();

        return bookingCount + refundCount;
    }
}