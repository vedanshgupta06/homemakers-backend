package com.homemakers.homemakers.repository;

import com.homemakers.homemakers.model.Provider;
import com.homemakers.homemakers.model.ProviderNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderNotificationRepository extends JpaRepository<ProviderNotification, Long> {
    List<ProviderNotification> findByProviderOrderByCreatedAtDesc(Provider provider);
    long countByProviderAndReadFalse(Provider provider);
}