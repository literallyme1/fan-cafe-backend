package com.example.fan_cafe.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ErrorLogHelper {

    //에러 로그 표준화 클래스
    public void logError(
            Exception ex,
            HttpServletRequest request,
            String errorCode
    ) {
        log.error(
                "Request failed. method={}, uri ={}, errorCode={}",
                request.getMethod(),
                request.getRequestURI(),
                errorCode,
                ex
        );
    }
}
