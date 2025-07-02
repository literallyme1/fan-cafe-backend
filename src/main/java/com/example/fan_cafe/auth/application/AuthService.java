package com.example.fan_cafe.auth.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.JwtErrorCode;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.global.security.JwtProvider;
import com.example.fan_cafe.global.security.JwtTokenResponse;
import com.example.fan_cafe.global.security.RedisTokenRepository;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.example.fan_cafe.auth.interfaces.dto.LoginRequest;
import com.example.fan_cafe.auth.interfaces.dto.LoginResponse;
import com.example.fan_cafe.auth.interfaces.dto.RegisterRequest;
import com.example.fan_cafe.auth.interfaces.dto.UserInfoResponse;
import lombok.RequiredArgsConstructor;
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
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail()))
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        else if(userRepository.existsByEmailAndDeletedAtIsNotNull(request.getEmail()))
            throw new CustomException(UserErrorCode.USER_ALREADEY_DELETED);
        else if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname()))
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword, role);
        userRepository.save(user);

        return ApiResponse.success(ApiResponseStatus.CREATED, UserInfoResponse.from(user));

    }

    public ApiResponse<LoginResponse> login(LoginRequest request){

        Optional<User> userOptional = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail());
        if(userOptional.isEmpty()) throw new CustomException(UserErrorCode.USER_NOT_FOUND);

        User user = userOptional.get();

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new CustomException(UserErrorCode.INVALID_PASSWORD);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());
        redisTokenRepository.save(user.getId(), refreshToken);
        UserInfoResponse userInfo = UserInfoResponse.from(user);
        JwtTokenResponse jwtToken = JwtTokenResponse.from(accessToken, refreshToken);
        return ApiResponse.success(ApiResponseStatus.SUCCESS, LoginResponse.from(jwtToken, userInfo));

    }

    public ApiResponse<Void> logout(Long userId){
        redisTokenRepository.delete(userId);
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }

    public ApiResponse<JwtTokenResponse> reissueAccessToken(String refreshToken) {

        if(!jwtProvider.isValid(refreshToken)) {throw new CustomException(JwtErrorCode.INVALID_REFRESH_TOKEN);}
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        String userRole = jwtProvider.getRoleFromToken(refreshToken);
        String savedToken = redisTokenRepository.find(userId);

        if(!refreshToken.equals(savedToken)) {throw new CustomException(JwtErrorCode.REFRESH_TOKEN_MISMATCH);}

        String newAccessToken = jwtProvider.createAccessToken(userId, Role.from(userRole));
        return ApiResponse.success(ApiResponseStatus.SUCCESS, JwtTokenResponse.from(newAccessToken));
    }
}
