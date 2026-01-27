package com.homemakers.homemakers.controller;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.service.ProviderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@PreAuthorize("hasRole('USER')")
public class UserProviderController {

    private final ProviderService providerService;

    public UserProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }


    @GetMapping("/search")
    public List<Provider> searchProviders(
            @RequestParam String service
    ) {
        return providerService.getProvidersByServiceWithAvailability(service);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Provider getProviderById(@PathVariable Long id) {
        return providerService.getProviderById(id);
    }

}