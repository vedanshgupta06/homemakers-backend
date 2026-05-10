package com.homemakers.homemakers.dto;

import java.util.Map;

/**
 * Returned by getProviderOptions() to the customer.
 * Includes distance and match reason so the frontend
 * can show "2.3 km away" or "covers your area" labels.
 */
public class ProviderOptionDTO {

    private Long providerId;
    private String providerName;
    private double rating;
    private int experienceYears;
    private double totalPrice;
    private Map<String, Double> priceBreakdown;
    private String profilePhotoUrl;

    // ── new geo fields ──
    private double distanceKm;       // straight-line distance to customer
    private String matchReason;      // "PINCODE_MATCH" | "RADIUS_MATCH" | "CITY_MATCH"

    public ProviderOptionDTO() {}

    // Original constructor — kept for any existing callers
    public ProviderOptionDTO(
            Long providerId,
            String providerName,
            double rating,
            int experienceYears,
            double totalPrice,
            Map<String, Double> priceBreakdown,
            String profilePhotoUrl
    ) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.rating = rating;
        this.experienceYears = experienceYears;
        this.totalPrice = totalPrice;
        this.priceBreakdown = priceBreakdown;
        this.profilePhotoUrl = profilePhotoUrl;
        this.distanceKm = -1;
        this.matchReason = "CITY_MATCH";
    }

    // Full constructor with geo fields
    public ProviderOptionDTO(
            Long providerId,
            String providerName,
            double rating,
            int experienceYears,
            double totalPrice,
            Map<String, Double> priceBreakdown,
            String profilePhotoUrl,
            double distanceKm,
            String matchReason
    ) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.rating = rating;
        this.experienceYears = experienceYears;
        this.totalPrice = totalPrice;
        this.priceBreakdown = priceBreakdown;
        this.profilePhotoUrl = profilePhotoUrl;
        this.distanceKm = distanceKm;
        this.matchReason = matchReason;
    }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public Map<String, Double> getPriceBreakdown() { return priceBreakdown; }
    public void setPriceBreakdown(Map<String, Double> priceBreakdown) { this.priceBreakdown = priceBreakdown; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
}