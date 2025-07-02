package com.example.fan_cafe.auth;


import com.example.fan_cafe.auth.application.AuthService;
import com.example.fan_cafe.auth.interfaces.dto.LoginRequest;
import com.example.fan_cafe.auth.interfaces.dto.RegisterRequest;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.security.JwtProvider;
import com.example.fan_cafe.global.security.RedisTokenRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User(1L,"test@test.com", "encode_pw", "nickname", Role.USER);
    }


    @Test
    void register_success() {
        //given
        RegisterRequest request = new RegisterRequest("test@test.com", "1234567", "nickname");
        when(userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())).thenReturn(false);
        when(userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encode_pw");

        //whend
        var response = authService.register(request, Role.USER);

        //then
        assertEquals(201, response.getStatus());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_success() {
        //given
        LoginRequest request = new LoginRequest("test@test.com", "1234567");
        when(userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken(mockUser.getId(), mockUser.getRole())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(mockUser.getId(), mockUser.getRole())).thenReturn("refresh-token");

        //when
        var response = authService.login(request);
        //then
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        verify(redisTokenRepository, times(1)).save(mockUser.getId(), "refresh-token");
    }

    @Test
    void login_failed() {
        //given
        LoginRequest request = new LoginRequest("test@test.com", "wrong_pw");
        when(userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPassword())).thenReturn(false);

        //when, then
        assertThrows(CustomException.class, () -> authService.login(request));
    }
}
