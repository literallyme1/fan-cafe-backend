package com.example.fan_cafe.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    String getCode();
    HttpStatus getStatus();
    String getMessage();
}
