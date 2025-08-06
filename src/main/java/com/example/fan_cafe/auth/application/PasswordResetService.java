package com.example.fan_cafe.auth.application;

import com.example.fan_cafe.auth.exception.MailErrorCode;
import com.example.fan_cafe.auth.infrastructure.PasswordResetPayload;
import com.example.fan_cafe.auth.infrastructure.PasswordResetTokenRedisRepository;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRedisRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final UserService userService;

    public void sendResetLink(String email){
        var user = userService.findByEmail(email);
        if (user == null) {
            throw  new CustomException(MailErrorCode.EMAIL_NOT_FOUND);
        }

        String token = UUID.randomUUID().toString();

        long now = Instant.now().getEpochSecond();
        long passwordLastUpdated = user.getPasswordUpdatedAtEpochSec();

        PasswordResetPayload payload = new PasswordResetPayload(
                user.getId(),
                now,
                passwordLastUpdated,
                "RESET_PASSWORD"
        );

        tokenRepo.save(token, payload, Duration.ofMinutes(3));
        String link = "http://localhost:8080/reset-password?token=" + token;

        mailService.sendResetMail(email, link);
    }

    public PasswordResetPayload validateToken(String token) {
        PasswordResetPayload payload = tokenRepo.find(token);

        //payload 가 없을 시 무효
        if (payload == null) {
            throw new CustomException(MailErrorCode.INVALID_TOKEN);
        }

        //이미 변경 시 무효
        long currentPasswordLastUpdated = userService.getPasswordLastUpdatedAt(payload.getUserId());

        if (currentPasswordLastUpdated != payload.getPasswordUpdatedAtEpochSecAtIssue()) {
            throw new CustomException(MailErrorCode.PASSWORD_ALREADY_CHANGED);
        }

        // 목적이 다르면 무효
        if (!"RESET_PASSWORD".equals(payload.getPurpose())) {
            throw new CustomException(MailErrorCode.INVALID_TOKEN_PURPOSE);
        }
        return payload;
    }

    public void resetPassword(String token, String newPassword){
        PasswordResetPayload payload = validateToken(token);

        //기존과 동일한 지 확인
        User user = userService.findById(payload.getUserId());
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CustomException(MailErrorCode.PASSWORD_SAME_AS_OLD);
        }

        userService.changePassword(payload.getUserId(), newPassword);

        // 토큰 폐기 (재사용 방지)
        tokenRepo.delete(token);
    }


}
