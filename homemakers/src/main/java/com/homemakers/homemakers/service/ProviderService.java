package com.homemakers.homemakers.service;

import com.homemakers.homemakers.dto.ProviderProfileUpdateRequest;
import com.homemakers.homemakers.dto.ProviderRegisterRequest;
import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.Role;
import com.homemakers.homemakers.model.User;
import com.homemakers.homemakers.repository.ProviderAvailabilityRepository;
import com.homemakers.homemakers.repository.ProviderRepository;
import com.homemakers.homemakers.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

@Service
public class ProviderService {

    private final ProviderRepository providerRepo;
    private final UserRepository userRepo;
    private final ProviderAvailabilityRepository availabilityRepo;
    private final CloudinaryService cloudinaryService;

    public ProviderService(
            ProviderRepository providerRepo,
            UserRepository userRepo,
            ProviderAvailabilityRepository availabilityRepo,
            CloudinaryService cloudinaryService
    ) {
        this.providerRepo      = providerRepo;
        this.userRepo          = userRepo;
        this.availabilityRepo  = availabilityRepo;
        this.cloudinaryService = cloudinaryService;
    }

    // =====================================================
    // GET ALL PROVIDERS
    // =====================================================
    public List<Provider> getAllProviders() {
        return providerRepo.findAll();
    }

    // =====================================================
    // REGISTER PROVIDER (USER → PROVIDER)
    // =====================================================
    @Transactional
    public Provider registerProvider(ProviderRegisterRequest request, User user) {

        if (providerRepo.existsByUser(user)) {
            throw new IllegalStateException("User already registered as provider");
        }

        if (request.getServices() == null || request.getServices().isEmpty()) {
            throw new RuntimeException("Provider must select at least one service");
        }

        user.setRole(Role.PROVIDER);
        userRepo.save(user);

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setCity(request.getCity());
        provider.setExperienceYears(request.getExperienceYears());
        provider.setPricePerHour(request.getPricePerHour());
        provider.setServices(new HashSet<>(request.getServices()));

        // ── geo fields ──
        if (request.getHomeLatitude() != null) {
            provider.setHomeLatitude(request.getHomeLatitude());
        }
        if (request.getHomeLongitude() != null) {
            provider.setHomeLongitude(request.getHomeLongitude());
        }
        provider.setTravelRadiusKm(
                request.getTravelRadiusKm() != null ? request.getTravelRadiusKm() : 10
        );
        provider.setWillingToTravel(
                request.getWillingToTravel() != null ? request.getWillingToTravel() : false
        );
        if (request.getServiceablePincodes() != null && !request.getServiceablePincodes().isEmpty()) {
            provider.setServiceablePincodes(request.getServiceablePincodes());
        }

        // Defaults
        provider.setVerified(false);
        provider.setRating(0.0);
        provider.setTotalRatings(0);

        return providerRepo.save(provider);
    }

    // =====================================================
    // FILTER PROVIDERS BY SERVICE + ACTIVE AVAILABILITY
    // =====================================================
    public List<Provider> getProvidersByServiceWithAvailability(String service) {

        return providerRepo.findAll()
                .stream()
                .filter(provider ->
                        provider.isVerified() &&
                                provider.getServices().contains(service) &&
                                availabilityRepo.existsByProviderAndActiveTrue(provider)
                )
                .toList();
    }

    // =====================================================
    // GET PROVIDER BY EMAIL
    // =====================================================
    public Provider getProviderByEmail(String email) {

        return providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    // =====================================================
    // UPDATE PROVIDER SERVICES
    // =====================================================
    @Transactional
    public Provider updateProviderServices(String email, List<String> services) {

        if (services == null || services.isEmpty()) {
            throw new RuntimeException("At least one service must be selected");
        }

        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        provider.setServices(new HashSet<>(services));

        return providerRepo.save(provider);
    }

    // =====================================================
    // GET PROVIDER BY ID
    // =====================================================
    public Provider getProviderById(Long id) {

        return providerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    // =====================================================
    // UPDATE PROVIDER PROFILE (includes geo fields)
    // =====================================================
    @Transactional
    public Provider updateProviderProfile(
            String email,
            ProviderProfileUpdateRequest request
    ) {
        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        provider.setCity(request.getCity());
        provider.setExperienceYears(request.getExperienceYears());
        provider.setPricePerHour(request.getPricePerHour());

        if (request.getServices() != null && !request.getServices().isEmpty()) {
            provider.setServices(new HashSet<>(request.getServices()));
        }

        // ── geo fields (only update if provided) ──
        if (request.getHomeLatitude() != null) {
            provider.setHomeLatitude(request.getHomeLatitude());
        }
        if (request.getHomeLongitude() != null) {
            provider.setHomeLongitude(request.getHomeLongitude());
        }
        if (request.getTravelRadiusKm() != null) {
            provider.setTravelRadiusKm(request.getTravelRadiusKm());
        }
        if (request.getWillingToTravel() != null) {
            provider.setWillingToTravel(request.getWillingToTravel());
        }
        if (request.getServiceablePincodes() != null && !request.getServiceablePincodes().isEmpty()) {
            provider.setServiceablePincodes(request.getServiceablePincodes());
        }

        return providerRepo.save(provider);
    }

    // =====================================================
    // UPLOAD PROFILE PHOTO — Cloudinary
    // =====================================================
    @Transactional
    public Provider uploadProfilePhoto(
            String email,
            MultipartFile file
    ) throws IOException {

        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        // Uploads to Cloudinary and returns full https://res.cloudinary.com/... URL
        String imageUrl = cloudinaryService.uploadFile(file, "homemakers/providers");
        provider.setProfilePhotoUrl(imageUrl);

        return providerRepo.save(provider);
    }

    // =====================================================
    // UPLOAD DOCUMENTS — Cloudinary
    // =====================================================
    @Transactional
    public Provider uploadDocuments(
            String email,
            MultipartFile idProof,
            MultipartFile addressProof
    ) throws IOException {

        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        // Uploads to Cloudinary and returns full https://res.cloudinary.com/... URLs
        String idProofUrl      = cloudinaryService.uploadFile(idProof,      "homemakers/documents");
        String addressProofUrl = cloudinaryService.uploadFile(addressProof, "homemakers/documents");

        provider.setIdProofUrl(idProofUrl);
        provider.setAddressProofUrl(addressProofUrl);

        return providerRepo.save(provider);
    }
}