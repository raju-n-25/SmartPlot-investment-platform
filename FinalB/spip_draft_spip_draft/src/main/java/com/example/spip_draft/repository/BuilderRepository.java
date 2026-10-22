package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderRepository extends JpaRepository<Builder, Long> {

    // 🔗 Existing mappings
    Optional<Builder> findByUser(User user);

    Optional<Builder> findByUserId(Long userId);

    List<Builder> findByKycStatus(KycStatus kycStatus);

    // 🔥 NEW (CRITICAL for multi-tenant)
    Optional<Builder> findByTenantId(Long tenantId);

    // 🔒 Extra safety (optional but recommended)
    Optional<Builder> findByIdAndTenantId(Long builderId, Long tenantId);
 
    long countByKycStatus(KycStatus kycStatus);
}