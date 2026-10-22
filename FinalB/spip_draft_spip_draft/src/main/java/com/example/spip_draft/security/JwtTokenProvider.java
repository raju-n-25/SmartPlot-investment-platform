package com.example.spip_draft.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private long jwtExpirationInMs;

    /**
     * 🔐 Generate JWT token (FIXED)
     */
    public String generateToken(CustomUserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", userDetails.getUserId());
        claims.put("role", userDetails.getRole());
        claims.put("builderId", userDetails.getBuilderId());

        // 🔥 CRITICAL FIX
        claims.put("tenantId", userDetails.getTenantId());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username
     */
    public String getUsernameFromJWT(String token) {
        return getAllClaims(token).getSubject();
    }

    /**
     * Extract userId
     */
    public Long getUserIdFromJWT(String token) {
        return getAllClaims(token).get("userId", Long.class);
    }

    /**
     * Extract builderId
     */
    public Long getBuilderIdFromJWT(String token) {
        return getAllClaims(token).get("builderId", Long.class);
    }

    /**
     * 🔥 Extract tenantId (NEW)
     */
    public Long getTenantIdFromJWT(String token) {
        return getAllClaims(token).get("tenantId", Long.class);
    }

    /**
     * Get all claims
     */
    public Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            System.out.println("Invalid JWT: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Signing key
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}