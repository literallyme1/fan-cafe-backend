package com.example.fan_cafe.global.config;

import com.example.fan_cafe.global.logging.TraceIdFilter;
import com.example.fan_cafe.global.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class LocalSecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain localSecurityChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth
                                //local에서만 health 추가
                                .requestMatchers("/actuator/health/**").permitAll()

                                // 기존 그대로
                                .requestMatchers(
                                        "/auth/register",
                                        "/auth/login",
                                        "/auth/refresh",
                                        "/reset-password/*"
                                ).permitAll()
                                // 관리자 DLQ 화면: local·dev 환경에서 인증 없이 접근 가능
                                .requestMatchers("/admin/dlq/**").permitAll()
                                .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(new TraceIdFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtFilter(jwtProvider, userDetailsService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
