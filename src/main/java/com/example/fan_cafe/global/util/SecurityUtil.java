package com.example.fan_cafe.global.util;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.SecurityErrorCode;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.security.CustomUserDetails;
import com.example.fan_cafe.user.domain.User;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor
public class SecurityUtil {

    public static Long getCurrentUserId() {
        return getCurrentUserDetails().getUserId();
    }

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(SecurityErrorCode.UNAUTHORIZED);
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

}
