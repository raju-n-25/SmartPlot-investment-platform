package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Complaint;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Complaint> findByRaisedByUserOrderByCreatedAtDesc(User user);

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findByTenantIdAndStatusOrderByCreatedAtDesc(
            Long tenantId,
            ComplaintStatus status
    );

    List<Complaint> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    long countByStatus(ComplaintStatus status);
}