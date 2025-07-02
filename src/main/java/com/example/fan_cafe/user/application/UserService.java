package com.example.fan_cafe.user.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public ApiResponse<Void> delete(Long principalUserId){
        //jpa 영속을 위해 다시 한 번 조회
        User user = userRepository.findById(principalUserId)
                        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        user.delete();
        return ApiResponse.success(ApiResponseStatus.SUCCESS);
    }
}
