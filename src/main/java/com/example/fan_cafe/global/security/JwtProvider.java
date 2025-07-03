package com.example.fan_cafe.global.security;

import com.example.fan_cafe.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.auth.exception.JwtErrorCode;

@Component
public class JwtProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private final KeyProvider keyProvider;

    public JwtProvider(
            KeyProvider keyProvider,
            @Value("${jwt.private-key-path}") Resource privateKeyPath,
            @Value("${jwt.public-key-path}") Resource publicKeyPath,
            @Value("${jwt.access-token-expiration}") long accessTokenValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenValidity
    ) {
        this.keyProvider = keyProvider;
        try {
            this.privateKey = keyProvider.loadPrivateKey(privateKeyPath);
            this.publicKey = keyProvider.loadPublicKey(publicKeyPath);
            this.accessTokenValidity = accessTokenValidity;
            this.refreshTokenValidity = refreshTokenValidity;
        } catch (Exception e) {
            throw new CustomException(JwtErrorCode.KEY_LOAD_FAILED);
        }
    }

    public String generateAccessToken(Long userId, Role role) {
        return createAccessToken(userId, role);
    }

    public String generateRefreshToken(Long userId, Role role) {
        return createRefreshToken(userId, role);
    }

    public String createRefreshToken(Long userId, Role role) {

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(privateKey)
                .compact();
    }

    public String createAccessToken(Long userId, Role role) {

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(privateKey)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
