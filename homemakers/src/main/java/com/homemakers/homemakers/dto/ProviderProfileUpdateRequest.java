package com.homemakers.homemakers.dto;

import java.util.List;
import java.util.Set;

public class ProviderProfileUpdateRequest {

    private String city;
    private int experienceYears;
    private double pricePerHour;
    private List<String> services;

    // ── geo fields ──
    private Double homeLatitude;
    private Double homeLongitude;
    private Integer travelRadiusKm;
    private Boolean willingToTravel;
    private Set<String> serviceablePincodes;

    // ── existing getters/setters ──

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }

    // ── geo getters/setters ──

    public Double getHomeLatitude() { return homeLatitude; }
    public void setHomeLatitude(Double homeLatitude) { this.homeLatitude = homeLatitude; }

    public Double getHomeLongitude() { return homeLongitude; }
    public void setHomeLongitude(Double homeLongitude) { this.homeLongitude = homeLongitude; }

    public Integer getTravelRadiusKm() { return travelRadiusKm; }
    public void setTravelRadiusKm(Integer travelRadiusKm) { this.travelRadiusKm = travelRadiusKm; }

    public Boolean getWillingToTravel() { return willingToTravel; }
    public void setWillingToTravel(Boolean willingToTravel) { this.willingToTravel = willingToTravel; }

    public Set<String> getServiceablePincodes() { return serviceablePincodes; }
    public void setServiceablePincodes(Set<String> serviceablePincodes) {
        this.serviceablePincodes = serviceablePincodes;
    }
}