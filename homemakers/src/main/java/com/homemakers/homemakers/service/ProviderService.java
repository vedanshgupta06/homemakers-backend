package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.ProviderRegisterRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.Role;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.ProviderAvailabilityRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class ProviderService {

    private final ProviderRepository providerRepo;
    private final UserRepository userRepo;
    private final ProviderAvailabilityRepository availabilityRepo;

    public ProviderService(
            ProviderRepository providerRepo,
            UserRepository userRepo,
            ProviderAvailabilityRepository availabilityRepo
    ) {
        this.providerRepo = providerRepo;
        this.userRepo = userRepo;
        this.availabilityRepo = availabilityRepo;
    }

    // ------------------------
    // GET ALL PROVIDERS
    // ------------------------
    public List<Provider> getAllProviders() {
        return providerRepo.findAll();
    }

    // ------------------------
    // REGISTER PROVIDER
    // ------------------------
    @Transactional
    public void registerProvider(ProviderRegisterRequest request, User user) {

        if (providerRepo.existsByUser(user)) {
            throw new IllegalStateException("Already registered as provider");
        }

        // 🔐 VALIDATION: at least one service required
        if (request.getServices() == null || request.getServices().isEmpty()) {
            throw new RuntimeException("Provider must select at least one service");
        }

        // Assign PROVIDER role
        user.setRole(Role.PROVIDER);
        userRepo.save(user);

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setServices(new HashSet<>(request.getServices()));
        provider.setCity(request.getCity());
        provider.setExperienceYears(request.getExperienceYears());
        provider.setPricePerHour(request.getPricePerHour());

        provider.setVerified(false);
        provider.setRating(0.0);
        provider.setTotalRatings(0);

        providerRepo.save(provider);
    }

    // ------------------------
    // FILTER PROVIDERS BY SERVICE + ACTIVE AVAILABILITY
    // ------------------------
    public List<Provider> getProvidersByServiceWithAvailability(String service) {

        return providerRepo.findAll()
                .stream()
                .filter(provider ->
                        provider.getServices().contains(service) &&
                                availabilityRepo.existsByProviderAndActiveTrue(provider)
                )
                .toList();
    }

    // ------------------------
    // GET PROVIDER BY EMAIL
    // ------------------------
    public Provider getProviderByEmail(String email) {
        return providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    // ------------------------
    // UPDATE PROVIDER SERVICES
    // ------------------------
    @Transactional
    public Provider updateProviderServices(String email, List<String> services) {

        // 🔐 VALIDATION: cannot remove all services
        if (services == null || services.isEmpty()) {
            throw new RuntimeException("At least one service must be selected");
        }

        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        provider.setServices(new HashSet<>(services));
        return providerRepo.save(provider);
    }

    // ------------------------
    // GET PROVIDER BY ID
    // ------------------------
    public Provider getProviderById(Long id) {
        return providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }
}
