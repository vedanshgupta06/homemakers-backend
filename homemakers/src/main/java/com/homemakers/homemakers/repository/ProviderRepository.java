package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ServiceType;
import com.homemakers.homemakers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUser_Email(String email);

    boolean existsByUser(com.homemakers.homemakers.model.User user);

//    @Query("""
//        SELECT DISTINCT p
//        FROM Provider p
//        JOIN p.availabilities a
//        WHERE a.active = true
//        AND a.service = :service
//        """)
//    List<Provider> findProvidersWithActiveSlotsByService(
//            @Param("service") ServiceType service
//    );

    Optional<Provider> findByUserId(Long userId);


    Optional<Provider> findByUserEmail(String email);

    List<Provider> findByVerifiedFalse();
    List<Provider> findByCity(String city);
    List<Provider> findByCityIgnoreCase(String city);


    // ── Tier 1: providers who explicitly cover this pincode ───
    //
    // Joins provider_serviceable_pincodes and checks the
    // customer's pincode is in the provider's opted-in list.
    // Also requires the provider to be verified.
    @Query("""
            SELECT DISTINCT p FROM Provider p
            JOIN p.serviceablePincodes sp
            WHERE sp = :pincode
              AND p.verified = true
            """)
    List<Provider> findVerifiedByServiceablePincode(
            @Param("pincode") String pincode
    );

    // ── Tier 2: providers within radius (city scoped) ─────────
    //
    // Fetches all verified providers in the same city who have
    // geo coordinates set. Distance filtering (Haversine) is
    // applied in Java after this fetch because JPQL does not
    // have native trigonometric functions in all JPA providers.
    // City scoping keeps this result set small.
    @Query("""
            SELECT p FROM Provider p
            WHERE p.verified = true
              AND p.city = :city
              AND p.homeLatitude  IS NOT NULL
              AND p.homeLongitude IS NOT NULL
            """)
    List<Provider> findVerifiedWithGeoInCity(
            @Param("city") String city
    );

    // ── Tier 3: city-only fallback (willingToTravel flag) ────
    @Query("""
            SELECT p FROM Provider p
            WHERE p.verified = true
              AND LOWER(p.city) = LOWER(:city)
              AND p.willingToTravel = true
            """)
    List<Provider> findVerifiedWillingToTravelInCity(
            @Param("city") String city
    );
}
