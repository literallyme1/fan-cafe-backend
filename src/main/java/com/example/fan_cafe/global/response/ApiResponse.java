package com.example.fan_cafe.global.response;

import com.example.fan_cafe.global.exception.BaseErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiResponse<T> {

    @Schema(description = "서비스 응답 코드", example = "S001")
    private final String code;
    @Schema(description = "HTTP 상태 코드", example = "200")
    private final int status;
    @Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
    private final String message;
    @Schema(description = "응답 데이터", example = "{\"orderId\":10001,\"status\":\"PAID\"}")
    private final T data;

    private ApiResponse(String code, HttpStatus status, String message, T data) {
        this.code = code;
        this.status = status.value();
        this.message = message;
        this.data = data;
    }

    // 성공 응답 (ApiResponseStatus 활용)
    public static <T> ApiResponse<T> success(ApiResponseStatus status, T data) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), data);
    }

    public static <T> ApiResponse<T> success(ApiResponseStatus status) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), null);
    }

    // 실패 응답 (에러 코드 + 메시지 + 데이터 포함)
    public static <T> ApiResponse<T> fail(ApiResponseStatus status, T data) {
        return new ApiResponse<>(status.getCode(), status.getStatus(), status.getMessage(), data);
    }

    // 실패 응답 (데이터 없이)
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
