package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.ProviderProfileUpdateRequest;
import com.homemakers.homemakers.dto.ProviderRegisterRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/provider")
public class ProviderController {

    private final ProviderService providerService;
    private final UserRepository userRepository;

    public ProviderController(
            ProviderService providerService,
            UserRepository userRepository
    ) {
        this.providerService = providerService;
        this.userRepository = userRepository;
    }

    // =====================================================
    // USER → REGISTER AS PROVIDER
    // =====================================================
    @PostMapping("/register")
    @PreAuthorize("hasRole('USER')")
    public Provider registerProvider(
            @RequestBody ProviderRegisterRequest request
    ) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return providerService.registerProvider(request, user);
    }

    // =====================================================
    // USER → SEARCH PROVIDERS BY SERVICE
    // =====================================================
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public List<Provider> searchProviders(
            @RequestParam String service
    ) {
        return providerService.getProvidersByServiceWithAvailability(service);
    }

    // =====================================================
    // PROVIDER → GET OWN PROFILE
    // =====================================================
    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public Provider getMyProfile() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return providerService.getProviderByEmail(email);
    }

    // =====================================================
    // PROVIDER → UPDATE PROFILE
    // =====================================================
    @PutMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public Provider updateMyProfile(
            @RequestBody ProviderProfileUpdateRequest request
    ) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return providerService.updateProviderProfile(email, request);
    }

    // =====================================================
    // PROVIDER → UPDATE SERVICES
    // =====================================================
    @PutMapping("/me/services")
    @PreAuthorize("hasRole('PROVIDER')")
    public Provider updateMyServices(
            @RequestBody List<String> services
    ) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return providerService.updateProviderServices(email, services);
    }
    @PostMapping("/me/photo")
    @PreAuthorize("hasRole('PROVIDER')")
    public Provider uploadProfilePhoto(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return providerService.uploadProfilePhoto(email, file);
    }
    @PostMapping("/me/documents")
    @PreAuthorize("hasRole('PROVIDER')")
    public Provider uploadDocuments(
            @RequestParam("idProof") MultipartFile idProof,
            @RequestParam("addressProof") MultipartFile addressProof
    ) throws IOException {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return providerService.uploadDocuments(email, idProof, addressProof);
    }
}