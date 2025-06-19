package com.example.fan_cafe.user.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.security.JwtProvider;
import com.example.fan_cafe.global.security.RedisTokenRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.example.fan_cafe.user.interfaces.dto.LoginRequest;
import com.example.fan_cafe.user.interfaces.dto.LoginResponse;
import com.example.fan_cafe.user.interfaces.dto.RegisterRequest;
import com.example.fan_cafe.user.interfaces.dto.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTokenRepository redisTokenRepository;

    public ApiResponse<UserInfoResponse> register(RegisterRequest request, Role role)
    {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        else if (userRepository.existsByNickname(request.getNickname()))
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword, role);
        userRepository.save(user);

        return ApiResponse.success(ApiResponseStatus.CREATED, UserInfoResponse.from(user));

    }

    public ApiResponse<LoginResponse> login(LoginRequest request){

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if(userOptional.isEmpty()) throw new CustomException(UserErrorCode.USER_NOT_FOUND);

        User user = userOptional.get();

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new CustomException(UserErrorCode.INVALID_PASSWORD);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        redisTokenRepository.save(user.getId(), refreshToken);
        UserInfoResponse userInfo = UserInfoResponse.from(user);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, LoginResponse.from(accessToken, refreshToken, userInfo));




    }
}
