package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ServiceType;
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



}
