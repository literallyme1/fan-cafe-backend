package com.example.fan_cafe.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserRegisterReqeust {

    @NotBlank(message = "이메일은 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 입력하세요.")
    @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임을 입력하세요.")
    private String nickname;

}
