package com.example.fan_cafe.user;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<UserRegisterResponse> register(UserRegisterRequest request, Role role)
    {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        else if (userRepository.existsByNickname(request.getNickname()))
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword, role);
        userRepository.save(user);

        return ApiResponse.success(ApiResponseStatus.CREATED, UserRegisterResponse.from(user));

    }
}
