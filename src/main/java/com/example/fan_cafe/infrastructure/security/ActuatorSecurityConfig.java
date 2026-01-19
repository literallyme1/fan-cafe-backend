package com.example.fan_cafe.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("prod") // 운영에서만 적용
@Order(1)
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            // actuator는 CSRF 의미 없음
            .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        //health 엔드포인트 -> Acutator role 가진 사람만 가능
                        .requestMatchers("/actuator/health/**").hasRole("ACTUATOR")
                        .anyRequest().permitAll() //그 외는 다 허용
                )
                .httpBasic(Customizer.withDefaults()); //브라우저 만들어줌.
        return http.build();
    }
}
