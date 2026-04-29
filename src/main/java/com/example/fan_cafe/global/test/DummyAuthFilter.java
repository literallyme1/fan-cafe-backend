package com.example.fan_cafe.global.test;

import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Profile("test") // 부하 테스트 프로필에서만 작동
public class DummyAuthFilter extends OncePerRequestFilter {

    public record PrincipalWrapper(User user) {}

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. k6가 보낸 헤더 확인 (없으면 기본값 1L)
        String testUserId = request.getHeader("X-Test-User-ID");
        Long userId = (testUserId != null) ? Long.parseLong(testUserId) : 1L;

        // 2. 가짜 User 객체 생성
        User dummyUser = User.builder()
                .id(userId)
                .email("test" + userId + "@example.com")
                .nickname("nick")
                .role(Role.USER)
                .build();
        // 3. 스프링 시큐리티 인증 객체 강제 생성 및 주입
        // 토큰 검증 -> 인증을 스킵하고 직접 넣음.
        PrincipalWrapper principal = new PrincipalWrapper(dummyUser);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
