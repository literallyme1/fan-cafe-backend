package com.example.fan_cafe.global.security;


import com.example.fan_cafe.global.exception.JwtErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.security.PrivateKey;
import java.security.PublicKey;

import com.example.fan_cafe.global.exception.CustomException;
import java.security.spec.InvalidKeySpecException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private KeyProvider keyProvider;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = keyProvider.loadPrivateKey("src/main/resources/private_key.pem");
            this.publicKey = keyProvider.loadPublicKey("src/main/resources/public_key.pem");

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CustomException(JwtErrorCode.KEY_LOAD_FAILED);
        }
    }
}
