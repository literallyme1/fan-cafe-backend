package com.example.fan_cafe.global.test;


import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
@Tag(name = "로그 테스트", description = "개발 환경 로그 출력 확인")
public class LogTestController {
    @GetMapping("/test")
    @Operation(summary = "로그 출력 확인", description = "INFO 로그를 출력하고 정상 응답을 반환함.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "출력 성공")
    public String test() {
        log.info("TEST LOG");
        return "ok";
    }

}
