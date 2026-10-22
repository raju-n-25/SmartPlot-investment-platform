package com.example.spip_draft.repository;

import com.example.spip_draft.entity.UserDocument;
import com.example.spip_draft.enums.DocumentStatus;
import com.example.spip_draft.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    List<UserDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserDocument> findByUserIdAndCurrentTrueOrderByCreatedAtDesc(Long userId);

    List<UserDocument> findByUserIdAndDocumentTypeOrderByCreatedAtDesc(
            Long userId,
            DocumentType documentType
    );

    Optional<UserDocument> findFirstByUserIdAndDocumentTypeAndCurrentTrueOrderByCreatedAtDesc(
            Long userId,
            DocumentType documentType
    );

    List<UserDocument> findByStatusOrderByCreatedAtAsc(DocumentStatus status);

    List<UserDocument> findByRoleAndStatusOrderByCreatedAtAsc(
            String role,
            DocumentStatus status
    );

    long countByStatus(DocumentStatus status);

    long countByRoleAndStatus(String role, DocumentStatus status);

    // =========================
    // ADMIN REVIEW SUPPORT
    // =========================
    List<UserDocument> findByUserIdAndRoleOrderByCreatedAtDesc(Long userId, String role);

    List<UserDocument> findByUserIdAndRoleAndCurrentTrueOrderByCreatedAtDesc(Long userId, String role);
    
    List<UserDocument> findByRoleAndStatusOrderByCreatedAtDesc(
            String role,
            DocumentStatus status
    );
    List<UserDocument> findByRoleAndCurrentTrueOrderByCreatedAtDesc(String role);
}