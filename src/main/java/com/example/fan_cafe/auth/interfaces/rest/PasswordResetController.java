package com.example.fan_cafe.auth.interfaces.rest;

import com.example.fan_cafe.auth.application.PasswordResetService;
import com.example.fan_cafe.auth.interfaces.dto.ResetPasswordConfirmRequest;
import com.example.fan_cafe.auth.interfaces.dto.ResetPasswordSendRequest;
import com.example.fan_cafe.auth.interfaces.dto.ValidateResetTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reset-password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;


    @PostMapping("/send")
    public ResponseEntity<Void> sendResetLink(@RequestBody @Valid ResetPasswordSendRequest request) {
        passwordResetService.sendResetLink(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestBody @Valid ValidateResetTokenRequest dto) {
        passwordResetService.validateToken(dto.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

}
