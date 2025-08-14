package com.example.fan_cafe.auth.interfaces.dto;

import com.example.fan_cafe.auth.validation.PasswordMatch;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@PasswordMatch
@Getter
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "이메일은 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 입력하세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$",
            message = "비밀번호는 문자와 숫자를 포함한 6자 이상이어야 합니다."
    )
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank(message = "닉네임을 입력하세요.")
    @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
    @Pattern(
            regexp = "^[a-zA-Z0-9가-힣_-]+$",
            message = "닉네임은 한글, 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다."
    )
    private String nickname;
}
