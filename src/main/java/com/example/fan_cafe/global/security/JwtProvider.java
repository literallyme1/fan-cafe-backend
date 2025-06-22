package com.example.fan_cafe.global.security;

import com.example.fan_cafe.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.JwtErrorCode;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private  long accessTokenValidity;
    private  long refreshTokenValidity;

    private KeyProvider keyProvider;

    public JwtProvider(
            @Value("${jwt.private-key-path}") Resource privateKeyPath,
            @Value("${jwt.public-key-path}") Resource publicKeyPath,
            @Value("${jwt.access-token-expiration}") long accessTokenValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenValidity //시간
    ) {
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
        return createToken(userId, role, accessTokenValidity);
    }

    public String generateRefreshToken(Long userId, Role role) {
        return createToken(userId, role, refreshTokenValidity);
    }

    public String createToken(Long userId, Role role, long expiration) {

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
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
