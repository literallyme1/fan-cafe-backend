package com.example.fan_cafe.user;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class service {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<UserRegisterResponse> register(UserRegisterReqeust request, Role role)
    {
        if (userRepository.existsByEmail(request.getEmail())){
            throw new CustomException(UserErrorCode.USER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user =
    }
}
