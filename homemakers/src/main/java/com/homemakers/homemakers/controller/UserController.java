package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.LoginRequest;
import com.homemakers.homemakers.dto.LoginResponse;
import com.homemakers.homemakers.dto.RegisterRequest;
import com.homemakers.homemakers.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // REGISTER → USER ONLY
    @PostMapping("/register")
    public Object register(@RequestBody RegisterRequest request) {
        return userService.registerUser(request);
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
}
