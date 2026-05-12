package com.homemakers.homemakers.config;

import com.homemakers.homemakers.security.JwtAuthEntryPoint;
import com.homemakers.homemakers.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthEntryPoint jwtAuthEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})   // ✅ Spring Security 6 style
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.getWriter().write("Access Denied");
                        })
                )
//                .authorizeHttpRequests(auth -> auth
//
//                        // PUBLIC
//                        .requestMatchers("/api/auth/**").permitAll()
//                        .requestMatchers("/api/users/register").permitAll()
//
//                        // ADMIN
//                        .requestMatchers("/api/admin/**")
//                        .hasRole("ADMIN")
//
//                        // PROVIDER
//                        .requestMatchers("/api/provider/**")
//                        .hasRole("PROVIDER")
//
//                        // BOOKINGS (USER + PROVIDER + ADMIN)
//                        .requestMatchers("/api/providers","/api/bookings/**").authenticated()
//
//                        // USER
//                        .requestMatchers("/api/providers/**")
//                        .hasRole("USER")
//
//                        .requestMatchers("/api/reviews/**")
//                        .hasRole("USER")
//
//                        .anyRequest().authenticated()
//                )
                .authorizeHttpRequests(auth -> auth

                        // ✅ STRIPE WEBHOOK (MUST BE FIRST)
                        .requestMatchers("/api/payments/webhook").permitAll()

                        // PUBLIC
                        .requestMatchers(
                                "/providers/**",
                                "/provider-documents/**",
                                "/uploads/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/profile").authenticated()

                        // ADMIN (before everything else so admin can access all)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // PROVIDER — specific rules BEFORE the broad /api/provider/**
                        .requestMatchers("/api/provider/me/**").hasRole("PROVIDER")
                        .requestMatchers("/api/provider/availability/**").hasAnyRole("USER", "PROVIDER", "ADMIN")
                        .requestMatchers("/api/provider/**").hasRole("PROVIDER")

                        // PROVIDERS (public provider listing — USER + ADMIN)
                        .requestMatchers("/api/providers/**").hasAnyRole("USER", "ADMIN")

                        // USER APIs
                        .requestMatchers("/api/user/**").hasRole("USER")

                        // BOOKINGS
                        .requestMatchers("/api/bookings/**").hasAnyRole("USER", "PROVIDER", "ADMIN")

                        // REVIEWS
                        .requestMatchers("/api/reviews/**").hasRole("USER")

                        // PAYMENTS (after webhook)
                        .requestMatchers("/api/payments/**").hasRole("USER")

                        // RAZORPAY
                        .requestMatchers("/api/razorpay/**").hasRole("USER")

                        .anyRequest().authenticated()
        )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}