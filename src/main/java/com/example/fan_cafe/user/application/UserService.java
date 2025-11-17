package com.example.fan_cafe.user.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.user.exception.UserErrorCode;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.example.fan_cafe.user.interfaces.dto.ProfileRequest;
import com.example.fan_cafe.user.interfaces.dto.ProfileResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;

    public ProfileResponse update(Long userId, ProfileRequest request, MultipartFile image) {

        User user = findById(userId);

        // 닉네임이 바뀌는 경우만 중복체크 (대소문자 무시, 자기자신 제외 후 탐색)
        validateNickname(user, request.nickname());

        //프로필 이미지 변경 여부 확인
        String newAvatarUrl = null;
        if(request.isImageChanged()){
            if(user.getAvatarUrl() != null){
                s3Uploader.delete(s3Uploader.extractFileKey(user.getAvatarUrl()));
            }
            if(image != null){
                newAvatarUrl = s3Uploader.upload(image, "user");
            }
        }
        return updateWithImage(user, request, newAvatarUrl);

    }

    @Transactional
    private ProfileResponse updateWithImage(User user, ProfileRequest request, String avatarUrl){
        if(request.isImageChanged()){
            user.updateProfile(request.nickname(), request.introduction(), avatarUrl);
        }else{
            user.updateProfile(request.nickname(), request.introduction());
        }
        return ProfileResponse.from(user);
    }





    public ProfileResponse get(User user){
        return ProfileResponse.from(user);
    }

    public ProfileResponse getProfile(Long userId){
        User user = findById(userId);
        return ProfileResponse.from(user);

    }


    //password 변경(patch)
    //nickname 변경(patch)
    @Transactional
    public void delete(Long principalUserId){
        //jpa 영속을 위해 다시 한 번 조회
        User user = findById(principalUserId);
        user.delete();
    }

    public void changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.changePassword(passwordEncoder.encode(newPassword)); // 암호화 필요
        userRepository.save(user);
    }

    public User findById(Long userId){
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email){
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    public long getPasswordLastUpdatedAt(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        return user.getPasswordUpdatedAtEpochSec();
    }


    private void validateNickname(User user, String newNickname) {

        if (!Objects.equals(user.getNickname(), newNickname)) {
            if (userRepository.existsNickname(newNickname, user.getId() )) {
                throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
            }
        }
    }
}
