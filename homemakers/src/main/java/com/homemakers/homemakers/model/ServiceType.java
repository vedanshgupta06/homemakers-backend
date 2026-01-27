package com.homemakers.homemakers.model;


public enum ServiceType {

    // HOURLY → monthly calculated
    BABYSITTING(PricingType.HOURLY_MONTHLY),
    ELDER_CARE(PricingType.HOURLY_MONTHLY),
    COOKING(PricingType.HOURLY_MONTHLY),

    // FLAT monthly
    DISH_WASHING(PricingType.FLAT_MONTHLY),
    CLEANING(PricingType.FLAT_MONTHLY),
    DUSTING(PricingType.FLAT_MONTHLY),
    LAUNDRY(PricingType.FLAT_MONTHLY);

    private final PricingType pricingType;

    ServiceType(PricingType pricingType) {
        this.pricingType = pricingType;
    }

    public PricingType getPricingType() {
        return pricingType;
    }
}


