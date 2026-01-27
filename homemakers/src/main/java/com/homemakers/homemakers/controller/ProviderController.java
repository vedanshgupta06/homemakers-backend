package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.dto.ProviderRegisterRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ServiceType;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.UserRepository;
import com.homemakers.homemakers.service.ProviderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider")
@PreAuthorize("hasRole('USER')")
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

    // USER → PROVIDER
    @PostMapping("/register")
    @PreAuthorize("hasRole('USER')")
    public String registerProvider(
            @RequestBody ProviderRegisterRequest request
    ) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        providerService.registerProvider(request, user);
        return "Provider registered successfully";
    }
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public List<Provider> searchProviders(@RequestParam String service) {
        return providerService.getProvidersByServiceWithAvailability(service);
    }
    // PROVIDER → GET OWN PROFILE
    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public Provider getMyProfile() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return providerService.getProviderByEmail(email);
    }


    // PROVIDER → UPDATE SERVICES
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



}
