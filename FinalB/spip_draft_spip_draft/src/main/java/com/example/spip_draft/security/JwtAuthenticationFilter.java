package com.example.spip_draft.security;

import com.example.spip_draft.multitenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String jwt = getJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {

                // ✅ Extract username
                String username = jwtTokenProvider.getUsernameFromJWT(jwt);

                // 🔥 CRITICAL FIX: Extract tenantId (NOT userId)
                Long tenantId = jwtTokenProvider.getTenantIdFromJWT(jwt);

                // Load user
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ✅ Set authentication
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 🔥 SET CORRECT TENANT CONTEXT
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }

                // 🧪 DEBUG (remove later)
                System.out.println("TenantContext set to: " + tenantId);
            }

        } catch (Exception ex) {
            System.out.println("JWT Authentication failed: " + ex.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // ✅ ALWAYS CLEAR (IMPORTANT)
            TenantContext.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}