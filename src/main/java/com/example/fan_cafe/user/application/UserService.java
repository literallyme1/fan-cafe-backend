package com.example.fan_cafe.user.application;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.user.exception.UserErrorCode;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.example.fan_cafe.user.interfaces.dto.ProfileRequest;
import com.example.fan_cafe.user.interfaces.dto.ProfileResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ProfileResponse update(Long userId, ProfileRequest request) {
        User user = findById(userId);
        validateNickname(user.getNickname(), request.nickname());
        user.updateProfile(request.nickname(), request.introduction());
        return ProfileResponse.from(user);
    }

    public ProfileResponse get(User user){
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

    private void validateNickname(String nickname) {

        if (userRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    private void validateNickname(String originalNickname, String changedNickname) {

        if (!originalNickname.isEmpty() && originalNickname.equals(changedNickname)) { return; }
        if (userRepository.existsByNicknameAndDeletedAtIsNull(changedNickname)) {
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }


}
