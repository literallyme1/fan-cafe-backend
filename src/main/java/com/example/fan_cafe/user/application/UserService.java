package com.example.fan_cafe.user.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.exception.UserErrorCode;
import com.example.fan_cafe.global.response.ApiResponse;
import com.example.fan_cafe.global.response.ApiResponseStatus;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.example.fan_cafe.user.interfaces.dto.UserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public UserResponse get(User user){
        return UserResponse.from(user);
    }


    //password 변경(patch)
    //nickname 변경(patch)
    @Transactional
    public void delete(Long principalUserId){
        //jpa 영속을 위해 다시 한 번 조회
        User user = userRepository.findByIdAndDeletedAtIsNull(principalUserId)
                        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        user.delete();
    }


}
