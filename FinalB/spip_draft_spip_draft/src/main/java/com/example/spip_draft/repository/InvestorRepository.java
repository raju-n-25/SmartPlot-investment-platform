package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Investor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long> {

    // Platform-scoped investor lookup
    Optional<Investor> findByUserId(Long userId);

    // Keep this for legacy/admin/reporting use only if needed elsewhere
    Optional<Investor> findByUserIdAndTenantId(Long userId, Long tenantId);
}