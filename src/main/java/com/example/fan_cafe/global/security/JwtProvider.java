package com.example.fan_cafe.global.security;

import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.JwtErrorCode;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    private KeyProvider keyProvider;



    public JwtProvider(
            @Value("${jwt.private-key-path}") Resource privateKeyPath,
            @Value("${jwt.public-key-path}") Resource publicKeyPath,
            @Value("${jwt.access-token-expiration}") long accessTokenValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenValidity //시간
    ){
        try{
            this.privateKey = keyProvider.loadPrivateKey(privateKeyPath);
            this.publicKey = keyProvider.loadPublicKey(publicKeyPath);
            this.accessTokenValidity = accessTokenValidity;
            this.refreshTokenValidity = refreshTokenValidity;
        } catch(Exception e){
            throw new CustomException(JwtErrorCode.KEY_LOAD_FAILED);
        }
    }

    public String generateAccessToken(Long userId) {
        return
    }
}
