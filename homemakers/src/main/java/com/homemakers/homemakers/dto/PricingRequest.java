package com.homemakers.homemakers.dto;
import com.homemakers.homemakers.model.PricingType;
import com.homemakers.homemakers.model.ServiceType;
public class PricingRequest {

    private ServiceType service;
    private PricingType pricingType; // HOURLY or FLAT
    private double price;

    public ServiceType getService() {
        return service;
    }

    public void setService(ServiceType service) {
        this.service = service;
    }

    public PricingType getPricingType() {
        return pricingType;
    }

    public void setPricingType(PricingType pricingType) {
        this.pricingType = pricingType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
