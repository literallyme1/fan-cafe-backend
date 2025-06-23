package com.example.fan_cafe.global.util;


import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.SecurityErrorCode;
import com.example.fan_cafe.global.exception.UserErrorCode;
import com.example.fan_cafe.global.security.CustomUserDetails;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor
public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(SecurityErrorCode.UNAUTHORIZED);
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}
