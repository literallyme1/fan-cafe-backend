package com.example.fan_cafe.user;

import com.example.fan_cafe.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class controller {


    @PostMapping("/register")
    public ApiResponse<UserRegisterResponse> register(@RequestBody @Valid UserRegisterReqeust reqeust) {
        return service.register(reqeust);
    }

}
