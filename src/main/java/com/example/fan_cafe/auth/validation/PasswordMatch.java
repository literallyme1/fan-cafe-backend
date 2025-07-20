package com.example.fan_cafe.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)//클래스에 붙일 수 있게 함.
@Retention(RetentionPolicy.RUNTIME) //
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
    String message() default "비밀번호와 확인 값이 다릅니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
