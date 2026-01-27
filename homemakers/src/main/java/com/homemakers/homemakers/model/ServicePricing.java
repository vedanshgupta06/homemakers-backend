package com.homemakers.homemakers.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "service_pricing",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"provider_id", "service", "city"}
        )
)
public class ServicePricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingType pricingType;

    // ✅ MUST be nullable
    @Column(name = "price_per_hour", nullable = true)
    private Double pricePerHour;

    // ✅ MUST be nullable
    @Column(name = "monthly_rate", nullable = true)
    private Double monthlyRate;

    @Column(nullable = false)
    private String city;

    /* ================= GETTERS ================= */

    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public ServiceType getService() {
        return service;
    }

    public PricingType getPricingType() {
        return pricingType;
    }

    public Double getPricePerHour() {
        return pricePerHour;
    }

    public Double getMonthlyRate() {
        return monthlyRate;
    }

    public String getCity() {
        return city;
    }

    /* ================= SETTERS ================= */

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void setService(ServiceType service) {
        this.service = service;
    }

    public void setPricingType(PricingType pricingType) {
        this.pricingType = pricingType;
    }

    public void setPricePerHour(Double pricePerHour) {
        this.pricePerHour = pricePerHour;
        this.monthlyRate = null; // ensure mutual exclusivity
    }

    public void setMonthlyRate(Double monthlyRate) {
        this.monthlyRate = monthlyRate;
        this.pricePerHour = null;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
