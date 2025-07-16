package com.example.fan_cafe.auth.application;

import com.example.fan_cafe.auth.domain.JwtToken;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.auth.exception.JwtErrorCode;
import com.example.fan_cafe.user.exception.UserErrorCode;
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

    public UserInfoResponse register(RegisterRequest request, Role role)
    {
        validateRegisterRequest(request);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.of(request.getEmail(), encodedPassword, request.getNickname(), role);
        userRepository.save(user);

        return UserInfoResponse.from(user);

    }

    private void validateRegisterRequest(RegisterRequest request) {
        String email = request.getEmail();
        String nickname = request.getNickname();

        if (userRepository.existsByEmailAndDeletedAtIsNotNull(email)) {
            throw new CustomException(UserErrorCode.USER_ALREADEY_DELETED);
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    public LoginResponse login(LoginRequest request){

        User user = findByEmail(request.getEmail());
        validatePassword(request.getPassword(), user.getPassword());

        JwtToken jwtToken = issueJwtToken(user);
        UserInfoResponse userInfo = UserInfoResponse.from(user);
        return LoginResponse.from(jwtToken, userInfo);
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CustomException(UserErrorCode.INVALID_PASSWORD);
        }
    }

    private JwtToken issueJwtToken(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        redisTokenRepository.save(user.getId(), refreshToken);
        return JwtToken.from(accessToken, refreshToken);
    }

    private User findByEmail(String email){
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    public void logout(Long userId){
        redisTokenRepository.delete(userId);

    }

    public JwtTokenResponse reissueAccessToken(String refreshToken) {

        if(!jwtProvider.isValid(refreshToken)) {throw new CustomException(JwtErrorCode.INVALID_REFRESH_TOKEN);}
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        String userRole = jwtProvider.getRoleFromToken(refreshToken);
        String savedToken = redisTokenRepository.find(userId);

        if(!refreshToken.equals(savedToken)) {throw new CustomException(JwtErrorCode.REFRESH_TOKEN_MISMATCH);}

        String newAccessToken = jwtProvider.createAccessToken(userId, Role.of(userRole));
        return JwtTokenResponse.from(newAccessToken);
    }
}
