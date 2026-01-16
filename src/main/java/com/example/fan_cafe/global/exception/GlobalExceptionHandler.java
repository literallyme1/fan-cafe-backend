package com.example.fan_cafe.global.exception;

import com.example.fan_cafe.global.response.ApiResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import com.example.fan_cafe.global.response.ApiResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.net.BindException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorLogHelper errorLogHelper;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(
                ApiResponse.fail(ApiResponseStatus.VALIDATION_ERROR, errors)
        );
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleCustomException(
            CustomException ex,
            HttpServletRequest request
    ) {
        //응답 외 log 에러
        errorLogHelper.logError(ex, request, ex.getCode());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(ex.getErrorCode()));
    }

    //parameter 오류
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("잘못된 파라미터 값입니다.: '%s'", ex.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiResponseStatus.VALIDATION_ERROR, message));
    }

    //request body json 형식, enum 값 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleJsonParseError(HttpMessageNotReadableException ex){
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiResponseStatus.VALIDATION_ERROR, "요청 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.fail(ApiResponseStatus.VALIDATION_ERROR, "요청에 필요한 파일 혹은 데이터가 없습니다.")
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ApiResponseStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."));
    }

    //파일 업로드 에러
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<String>> handleMultipartException(MultipartException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiResponseStatus.VALIDATION_ERROR, "파일 업로드 형식이 잘못되었습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex,
                                                               HttpServletRequest request
    ) {
        errorLogHelper.logError(
                ex,
                request,
                ApiResponseStatus.INTERNAL_ERROR.name()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.fail(ApiResponseStatus.INTERNAL_ERROR, null)
        );
    }
}
