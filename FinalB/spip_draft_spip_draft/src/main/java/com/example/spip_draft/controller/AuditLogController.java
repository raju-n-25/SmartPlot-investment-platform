package com.example.spip_draft.controller;

import com.example.spip_draft.entity.AuditLog;
import com.example.spip_draft.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public List<AuditLog> getLatestLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping("/entity")
    public List<AuditLog> getByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId
    ) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @GetMapping("/actor/{userId}")
    public List<AuditLog> getByActor(@PathVariable Long userId) {
        return auditLogRepository.findByActorUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/tenant/{tenantId}")
    public List<AuditLog> getByTenant(@PathVariable Long tenantId) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @GetMapping("/action/{action}")
    public List<AuditLog> getByAction(@PathVariable String action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }
}