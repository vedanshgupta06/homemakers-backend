package com.homemakers.homemakers.util;

/**
 * Utility for geo-distance calculations.
 * Used by the provider matching algorithm to compute
 * straight-line distance between two lat/lng points.
 */
public class GeoUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Haversine formula — returns distance in kilometres
     * between two (lat, lng) coordinate pairs.
     */
    public static double distanceKm(
            double lat1, double lng1,
            double lat2, double lng2
    ) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Returns true if the two points are within maxKm of each other.
     */
    public static boolean isWithinRadius(
            double lat1, double lng1,
            double lat2, double lng2,
            double maxKm
    ) {
        return distanceKm(lat1, lng1, lat2, lng2) <= maxKm;
    }
}