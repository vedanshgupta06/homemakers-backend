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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        // Prevent duplicate provider registration
        if (providerRepo.existsByUser(user)) {
            throw new IllegalStateException("User already registered as provider");
        }

        // Validate services
        if (request.getServices() == null || request.getServices().isEmpty()) {
            throw new RuntimeException("Provider must select at least one service");
        }

        // Assign PROVIDER role
        user.setRole(Role.PROVIDER);
        userRepo.save(user);

        Provider provider = new Provider();

        provider.setUser(user);
        provider.setCity(request.getCity());
        provider.setExperienceYears(request.getExperienceYears());
        provider.setPricePerHour(request.getPricePerHour());

        // Convert services list → set
        provider.setServices(new HashSet<>(request.getServices()));

        // Default values
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

        return providerRepo.save(provider);
    }
    @Transactional
    public Provider uploadProfilePhoto(
            String email,
            MultipartFile file
    ) throws IOException {

        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        String uploadDir = "uploads/providers/";

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = provider.getId() + "_" + file.getOriginalFilename();

        Path filePath = Paths.get(uploadDir + fileName);

        Files.write(filePath, file.getBytes());

        provider.setProfilePhotoUrl("/providers/" + fileName);

        return providerRepo.save(provider);
    }
    @Transactional
    public Provider uploadDocuments(
            String email,
            MultipartFile idProof,
            MultipartFile addressProof
    ) throws IOException {

        Provider provider = providerRepo.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        String uploadDir = "uploads/provider-documents/";

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String idProofName =
                provider.getId() + "_id_" + idProof.getOriginalFilename();

        String addressProofName =
                provider.getId() + "_address_" + addressProof.getOriginalFilename();

        Path idProofPath = Paths.get(uploadDir + idProofName);
        Path addressProofPath = Paths.get(uploadDir + addressProofName);

        Files.write(idProofPath, idProof.getBytes());
        Files.write(addressProofPath, addressProof.getBytes());

        provider.setIdProofUrl("/provider-documents/" + idProofName);
        provider.setAddressProofUrl("/provider-documents/" + addressProofName);

        return providerRepo.save(provider);
    }
}