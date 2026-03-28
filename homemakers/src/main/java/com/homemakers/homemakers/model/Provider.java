package com.homemakers.homemakers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================
    // LINKED USER ACCOUNT
    // =====================================
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // =====================================
    // SERVICES OFFERED
    // =====================================
    @ElementCollection
    @CollectionTable(
            name = "provider_services",
            joinColumns = @JoinColumn(name = "provider_id")
    )
    @Column(name = "service")
    private Set<String> services;

    // =====================================
    // PROFILE DETAILS
    // =====================================
    private String city;

    private int experienceYears;

    private double pricePerHour;

    // =====================================
    // PROFILE PHOTO
    // =====================================
    private String profilePhotoUrl;

    // =====================================
    // DOCUMENTS FOR VERIFICATION
    // =====================================
    private String idProofUrl;

    private String addressProofUrl;

    // =====================================
    // VERIFICATION STATUS
    // =====================================
    private boolean verified = false;

    // =====================================
    // RATINGS
    // =====================================
    private double rating = 0.0;

    private int totalRatings = 0;

    // =====================================
    // WALLET / PAYOUT
    // =====================================
    @Column(name = "last_payout_requested_at")
    private LocalDateTime lastPayoutRequestedAt;

    // =====================================
    // AVAILABILITY
    // =====================================
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ProviderAvailability> availabilities;
    // =====================================
    // GETTERS & SETTERS
    // =====================================

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getIdProofUrl() {
        return idProofUrl;
    }

    public void setIdProofUrl(String idProofUrl) {
        this.idProofUrl = idProofUrl;
    }

    public String getAddressProofUrl() {
        return addressProofUrl;
    }

    public void setAddressProofUrl(String addressProofUrl) {
        this.addressProofUrl = addressProofUrl;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public LocalDateTime getLastPayoutRequestedAt() {
        return lastPayoutRequestedAt;
    }

    public void setLastPayoutRequestedAt(LocalDateTime lastPayoutRequestedAt) {
        this.lastPayoutRequestedAt = lastPayoutRequestedAt;
    }

    public List<ProviderAvailability> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(List<ProviderAvailability> availabilities) {
        this.availabilities = availabilities;
    }
}