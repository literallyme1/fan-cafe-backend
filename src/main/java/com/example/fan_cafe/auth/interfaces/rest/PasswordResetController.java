package com.example.fan_cafe.auth.interfaces.rest;

import com.example.fan_cafe.auth.application.PasswordResetService;
import com.example.fan_cafe.auth.interfaces.dto.ResetPasswordConfirmRequest;
import com.example.fan_cafe.auth.interfaces.dto.ResetPasswordSendRequest;
import com.example.fan_cafe.auth.interfaces.dto.ValidateResetTokenRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reset-password")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "비밀번호 재설정", description = "비밀번호 재설정 링크와 토큰 관리")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;


    @PostMapping("/send")
    @Operation(summary = "재설정 링크 발송", description = "가입 이메일로 비밀번호 재설정 링크를 발송함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이메일 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    public ResponseEntity<Void> sendResetLink(@RequestBody @Valid ResetPasswordSendRequest request) {
        passwordResetService.sendResetLink(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    @Operation(summary = "재설정 토큰 검증", description = "비밀번호 재설정 토큰의 유효성과 만료 여부를 확인함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유효한 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "토큰 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<Void> validateToken(@RequestBody @Valid ValidateResetTokenRequest dto) {
        passwordResetService.validateToken(dto.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    @Operation(summary = "비밀번호 변경", description = "검증된 토큰으로 새 비밀번호를 저장함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

}
