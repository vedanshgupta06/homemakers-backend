package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.LoginRequest;
import com.homemakers.homemakers.dto.LoginResponse;
import com.homemakers.homemakers.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // =========================
    // 🔐 LOGIN
    // =========================
    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {

        LoginResponse loginResponse =
                userService.login(request.getEmail(), request.getPassword());

        response.addHeader(
                "Set-Cookie",
                "refreshToken=" + loginResponse.getRefreshToken()
                        + "; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800"
        );


        // do NOT expose refresh token
        loginResponse.setRefreshToken(null);

        return loginResponse;
    }


    // =========================
    // 🔁 REFRESH
    // =========================
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {

        LoginResponse newTokens =
                userService.refreshAccessToken(refreshToken);

        response.addHeader(
                "Set-Cookie",
                "refreshToken=" + newTokens.getRefreshToken()
                        + "; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800"
        );


        newTokens.setRefreshToken(null);
        return newTokens;
    }


    // =========================
    // 🚪 LOGOUT
    // =========================
    @PostMapping("/logout")
    public void logout(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ) {

        userService.logout(refreshToken);

        response.addHeader(
                "Set-Cookie",
                "refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0"
        );

    }
}
