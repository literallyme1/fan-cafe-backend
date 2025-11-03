package com.example.fan_cafe.user;

import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.example.fan_cafe.user.interfaces.dto.ProfileRequest;
import com.example.fan_cafe.user.interfaces.dto.ProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private S3Uploader s3Uploader;

    @InjectMocks
    UserService userService;

    User mockUser;

    @BeforeEach
    void setUp(){
        mockUser = User.of("test@test.com", "encode_pw", "nickname", Role.USER);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "avatarUrl", "user_url");
    }

    @Test
    @DisplayName("image 가 변경되었을 때, s3에 upload 후 DB 저장")
    void givenIsImageChanged_whenUpdate_thenUploadedS3(){
        //given
        Long userId = 1L;
        ProfileRequest request = new ProfileRequest("소개글", "example", true);
        MultipartFile image = mock(MultipartFile.class);
        String url = "right_url";

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUser));
        when(s3Uploader.upload(any(MultipartFile.class), anyString())).thenReturn(url);
        when(userRepository.existsNickname(anyString(), anyLong())).thenReturn(false);

        //when
        ProfileResponse result = userService.update(userId, request, image);

        //then
        assertThat(result.getAvatarUrl()).isEqualTo(url);
        assertThat(result.getNickname()).isEqualTo("example");
        assertThat(result.getIntroduction()).isEqualTo("소개글");

    }

    @Test
    @DisplayName("image 가 변경되지 않았을 때, s3에 upload 없이 DB 저장")
    void givenIsNotImageChanged_whenUpdate_thenDisUploadedS3(){
        //given
        Long userId = 1L;
        ProfileRequest request = new ProfileRequest("소개글", "example", false);
        MultipartFile image = null;
        String url = "right_url";

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsNickname(anyString(), anyLong())).thenReturn(false);

        //when
        ProfileResponse result = userService.update(userId, request, image);

        //then
        assertThat(result.getAvatarUrl()).isEqualTo(url);
        assertThat(result.getNickname()).isEqualTo("example");
        assertThat(result.getIntroduction()).isEqualTo("소개글");
    }

    @Test
    @DisplayName("DB에 있는 NickName 과 동일 할 시 에러 던짐")
    void givenSameNickname_whenUpdate_thenThrowsError(){
        //given
        //when
        //then
    }

    @Test
    @DisplayName("s3 실패했을 때 롤백되어 DB 관련 함수 실행 X")
    void givenS3Error_whenUpdate_thenThrowsError(){
        //given
        //when
        //then
    }

}
