package com.example.spip_draft.util;

import com.example.spip_draft.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static CustomUserDetails getUserDetails() {
        Authentication authentication = getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("No authentication found");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    public static Long getCurrentUserId() {
        return getUserDetails().getUserId();
    }

    public static Long getCurrentTenantId() {
        return getUserDetails().getTenantId();
    }

    public static Long getCurrentBuilderId() {
        return getUserDetails().getBuilderId();
    }

    public static String getCurrentUserRole() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("No authentication found");
        }

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse("UNKNOWN");
    }

    public static boolean hasRole(String role) {
        try {
            return role != null && role.equalsIgnoreCase(getCurrentUserRole());
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isInvestor() {
        return hasRole("ROLE_INVESTOR");
    }

    public static boolean isBuilder() {
        return hasRole("ROLE_BUILDER");
    }

    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
