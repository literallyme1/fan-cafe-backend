package com.example.fan_cafe.auth;

import com.example.fan_cafe.auth.application.MailService;
import com.example.fan_cafe.auth.application.PasswordResetService;
import com.example.fan_cafe.auth.infrastructure.PasswordResetPayload;
import com.example.fan_cafe.auth.infrastructure.PasswordResetTokenRedisRepository;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRedisRepository tokenRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.of("test@test.com", "encode_pw", "nickname", Role.USER);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
    }

    @Test
    void sendResetLink_shouldSend_whenRequestIsValid() {
        String email = "test@test.com";
        when(userService.findByEmail(email)).thenReturn(mockUser);


        // when
        passwordResetService.sendResetLink(email);

        //then
        verify(userService).findByEmail(email);
        verify(tokenRepo).save(anyString(), any(PasswordResetPayload.class), any(Duration.class));
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendResetMail(eq(email), linkCaptor.capture());

        String capturedLink = linkCaptor.getValue();
        assertThat(capturedLink).startsWith("https://yourapp.com/reset-password?token=");
        assertThat(capturedLink).contains("token=");
    }

    @Test
    void sendResetLink_UserNotFound_ThrowsException() {
        // given
        String email = "notfound@example.com";
        when(userService.findByEmail(email)).thenReturn(null);

        // when & then
        assertThrows(CustomException.class, () -> passwordResetService.sendResetLink(email));
    }

    @Test
    void resetPassword_shouldChangePassword_andDeleteToken_whenTokenValidAndPasswordDifferent(){
        //given
        String token = "valid-token";
        String newPassword = "newPassword123!";
        Long userId = mockUser.getId();

        PasswordResetPayload payload = new PasswordResetPayload(
                userId,
                System.currentTimeMillis() / 1000,
                1234567890L,
                "RESET_PASSWORD"
        );

        when(tokenRepo.find(token)).thenReturn(payload);
        when(userService.getPasswordLastUpdatedAt(userId)).thenReturn(1234567890L);
        when(userService.findById(userId)).thenReturn(mockUser);
        when(passwordEncoder.matches(newPassword, mockUser.getPassword())).thenReturn(false);

        // when
        passwordResetService.resetPassword(token, newPassword);

        // then
        verify(userService).changePassword(userId, newPassword);
        verify(tokenRepo).delete(token);
    }

    @Test
    void resetPassword_shouldThrow_whenPasswordIsSameAsOld() {
        // given
        String token = "valid-token";
        String samePassword = "samePassword123!";
        Long userId = mockUser.getId();

        PasswordResetPayload payload = new PasswordResetPayload(
                userId,
                System.currentTimeMillis() / 1000,
                1234567890L,
                "RESET_PASSWORD"
        );

        when(tokenRepo.find(token)).thenReturn(payload);
        when(userService.getPasswordLastUpdatedAt(userId)).thenReturn(1234567890L);
        when(userService.findById(userId)).thenReturn(mockUser);
        when(passwordEncoder.matches(samePassword, mockUser.getPassword())).thenReturn(true); // 기존과 같음

        // when & then
        assertThrows(CustomException.class, () -> passwordResetService.resetPassword(token, samePassword));

    }
}
