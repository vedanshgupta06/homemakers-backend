package com.homemakers.homemakers.dto;

import java.util.Set;

public class ProviderRegisterRequest {

    private Set<String> services;
    private String city;
    private int experienceYears;
    private double pricePerHour;

    public Set<String> getServices() {
        return services;
    }

    public String getCity() {
        return city;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }
}
