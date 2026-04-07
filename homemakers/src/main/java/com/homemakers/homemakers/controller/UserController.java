package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.LoginRequest;
import com.homemakers.homemakers.dto.LoginResponse;
import com.homemakers.homemakers.dto.RegisterRequest;
import com.homemakers.homemakers.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.homemakers.homemakers.dto.UserProfileDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // REGISTER → USER ONLY
    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        userService.registerUser(request);
        return "User registered successfully";
    }

    // LOGIN
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request.getEmail(), request.getPassword());
    }

    // JWT TEST
    @GetMapping("/secure")
    public String secureEndpoint() {
        return "JWT ACCESS SUCCESS ✅";
    }

    // ROLE: USER
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public String userOnly() {
        return "USER ACCESS 👤";
    }

    // ================================
// 👤 GET PROFILE
// ================================
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(userService.getProfile(email));
    }


    // ================================
// ✏️ UPDATE PROFILE
// ================================
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            Authentication authentication,
            @RequestBody UserProfileDto dto
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(userService.updateProfile(email, dto));
    }



    // ROLE: PROVIDER (will move later)
    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public String providerOnly() {
        return "PROVIDER ACCESS 🛠️";
    }

    // ROLE: ADMIN (will move later)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "ADMIN ACCESS 👑";
    }

    // CURRENT LOGGED-IN USER
    @GetMapping("/me")
    public String me() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication authentication) {

        String email = authentication.getName();

        int totalBookings = userService.getTotalBookings(email);
        int upcoming = userService.getUpcomingBookings(email);

        Map<String, Object> response = new HashMap<>();
        response.put("totalBookings", totalBookings);
        response.put("upcoming", upcoming);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/recent-activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(Authentication auth) {

        String email = auth.getName();

        return ResponseEntity.ok(userService.getRecentActivity(email));
    }
}
