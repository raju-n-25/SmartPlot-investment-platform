package com.example.spip_draft.service;

import com.example.spip_draft.entity.AuditLog;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.enums.AuditAction;
import com.example.spip_draft.repository.AuditLogRepository;
import com.example.spip_draft.repository.UserRepository;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(
            AuditAction action,
            String entityType,
            Long entityId,
            String description
    ) {
        log(action, entityType, entityId, description, null, null);
    }

    public void log(
            AuditAction action,
            String entityType,
            Long entityId,
            String description,
            String oldValue,
            String newValue
    ) {
        logDetailed(
                action,
                resolveCategory(action),
                entityType,
                entityId,
                null,
                description,
                oldValue,
                newValue,
                null
        );
    }

    public void logDetailed(
            AuditAction action,
            String category,
            String entityType,
            Long entityId,
            String targetLabel,
            String description,
            String oldValue,
            String newValue,
            String metadataJson
    ) {
        try {
            AuditLog audit = new AuditLog();

            audit.setAction(action != null ? action.name() : "UNKNOWN_ACTION");
            audit.setCategory(category != null ? category : resolveCategory(action));
            audit.setEntityType(entityType);
            audit.setEntityId(entityId);
            audit.setTargetLabel(targetLabel);
            audit.setDescription(description);
            audit.setOldValue(oldValue);
            audit.setNewValue(newValue);
            audit.setMetadataJson(metadataJson);

            attachActorDetails(audit);

            auditLogRepository.save(audit);
        } catch (Exception ignored) {
            // Audit should never break business flow
        }
    }

    public void logSystem(
            AuditAction action,
            String category,
            String entityType,
            Long entityId,
            String targetLabel,
            String description,
            String oldValue,
            String newValue,
            String metadataJson
    ) {
        try {
            AuditLog audit = new AuditLog();

            audit.setAction(action != null ? action.name() : "SYSTEM_ACTION");
            audit.setCategory(category != null ? category : resolveCategory(action));
            audit.setEntityType(entityType);
            audit.setEntityId(entityId);
            audit.setTargetLabel(targetLabel);
            audit.setDescription(description);
            audit.setOldValue(oldValue);
            audit.setNewValue(newValue);
            audit.setMetadataJson(metadataJson);

            audit.setActorUserId(null);
            audit.setActorName("System");
            audit.setActorEmail(null);
            audit.setActorRole("SYSTEM");

            try {
                audit.setTenantId(SecurityUtil.getCurrentTenantId());
            } catch (Exception ignored) {
                audit.setTenantId(null);
            }

            auditLogRepository.save(audit);
        } catch (Exception ignored) {
            // Audit should never break business flow
        }
    }

    private void attachActorDetails(AuditLog audit) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            audit.setActorUserId(userId);

            userRepository.findById(userId).ifPresent(user -> {
                audit.setActorName(resolveName(user));
                audit.setActorEmail(user.getEmail());
            });
        } catch (Exception ignored) {
            audit.setActorUserId(null);
            audit.setActorName("System");
        }

        try {
            audit.setActorRole(SecurityUtil.getCurrentUserRole());
        } catch (Exception ignored) {
            audit.setActorRole("SYSTEM");
        }

        try {
            audit.setTenantId(SecurityUtil.getCurrentTenantId());
        } catch (Exception ignored) {
            audit.setTenantId(null);
        }
    }

    private String resolveName(User user) {
        if (user == null) {
            return "System";
        }

        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }

        return "User #" + user.getId();
    }

    private String resolveCategory(AuditAction action) {
        if (action == null) {
            return "GENERAL";
        }

        String value = action.name();

        if (value.contains("BOOKING")) return "BOOKING";
        if (value.contains("PAYMENT")) return "PAYMENT";
        if (value.contains("ESCROW")) return "ESCROW";
        if (value.contains("AGREEMENT")) return "AGREEMENT";
        if (value.contains("COMPLAINT")) return "COMPLAINT";
        if (value.contains("MEETING")) return "MEETING";
        if (value.contains("PROJECT")) return "PROJECT";
        if (value.contains("PLOT")) return "PLOT";
        if (value.contains("DOCUMENT") || value.contains("KYC")) return "DOCUMENT_KYC";
        if (value.contains("USER") || value.contains("PROFILE")) return "USER";

        return "GENERAL";
    }
}