package com.example.fan_cafe.global.response;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import com.example.fan_cafe.global.exception.UserErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiResponse<T> {

    private final String code;   // 에러/응답 코드
    private final int status;    // HTTP 상태 코드 (200, 400 등)
    private final String message;
    private final T data;

    private ApiResponse(String code, HttpStatus status, String message, T data) {
        this.code = code;
        this.status = status.value();
        this.message = message;
        this.data = data;
    }

    // ✅ 성공 응답 (ApiResponseStatus 활용)
    public static <T> ApiResponse<T> success(ApiResponseStatus status, T data) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), data);
    }

    public static <T> ApiResponse<T> success(ApiResponseStatus status) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), null);
    }

    // ✅ 실패 응답 (에러 코드 + 메시지 + 데이터 포함)
    public static <T> ApiResponse<T> fail(ApiResponseStatus status, T data) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), data);
    }

    // ✅ 실패 응답 (데이터 없이)
    public static <T> ApiResponse<T> fail(ApiResponseStatus status) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(BaseErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getStatus(), errorCode.getMessage(), data);
    }

    public static <T> ApiResponse<T> fail(BaseErrorCode errorCode) {
        return fail(errorCode, null);
    }

}
