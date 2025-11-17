package com.example.fan_cafe.user;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.s3.S3Uploader;
import com.example.fan_cafe.user.application.UserService;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.exception.UserErrorCode;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
        when(userRepository.existsNickname(anyString(), anyLong())).thenReturn(false);
        when(s3Uploader.upload(any(MultipartFile.class), anyString())).thenReturn(url);


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
        assertThat(result.getAvatarUrl()).isEqualTo("user_url");
        assertThat(result.getNickname()).isEqualTo("example");
        assertThat(result.getIntroduction()).isEqualTo("소개글");
    }

    @Test
    @DisplayName("DB에 있는 NickName 과 동일 할 시 에러 던짐")
    void givenSameNickname_whenUpdate_thenThrowsError(){
        //given
        Long userId = 1L;
        ProfileRequest request = new ProfileRequest("소개글", "example", false);
        MultipartFile image = null;
        String url = "right_url";

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsNickname(anyString(), anyLong())).thenReturn(true);

        //when & then
        assertThrows(CustomException.class,
                () -> userService.update(userId, request, image));
    }

    @Test
    @DisplayName("s3 실패했을 때 롤백되어 DB 관련 함수 실행 X")
    void givenS3Error_whenUpdate_thenThrowsError(){
        //given

        // given
        Long userId = 1L;
        User mockUser = mock(User.class);
        MultipartFile image = mock(MultipartFile.class);
        ProfileRequest request = new ProfileRequest("example", "example", true);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUser));
        // S3Uploader가 예외 던지도록 설정
        when(s3Uploader.upload(any(MultipartFile.class), eq("user")))
                .thenThrow(new RuntimeException("S3 error"));

        // when & then
        assertThrows(RuntimeException.class, () ->
                userService.update(userId, request, image)
        );

        // DB 관련 함수들이 실행되지 않았는지 검증
        verify(mockUser, never()).updateProfile(any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("userId 가 올바르다면 프로필을 반환한다.")
    void givenValidateUserId_whenGetProfile_thenReturnsProfile(){

        //given
        Long userId = 1L;
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUser));

        //when
        ProfileResponse result = userService.getProfile(userId);

        //then
        assertThat(result.getId()).isEqualTo(mockUser.getId());
    }

    @Test
    @DisplayName("userId 가 올바르지 않다면 에럴를 반환한다.")
    void givenValidateUserId_whenGetProfile_thenThrowsError(){

        //given
        Long userId = 1L;

        //when & then
        CustomException exception = assertThrows(CustomException.class, () ->
                userService.getProfile(userId)
        );

        assertThat(exception.getStatus()).isEqualTo(UserErrorCode.USER_NOT_FOUND.getStatus());
    }

}
