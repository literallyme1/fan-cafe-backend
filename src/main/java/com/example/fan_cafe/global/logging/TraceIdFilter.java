package com.example.fan_cafe.global.logging;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter { //요청당 한번만
    //traceId 생성, MDC 관리

    private static final String TRACE_ID =  "traceId"; //JSON 설정의 <mdc />와 매칭

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try{

            String traceId = TraceIdGenerator.generate();
            MDC.put(TRACE_ID, traceId); //mdc 저장

            //for Debug
            log.debug("Request started: {} {}", request.getMethod(), request.getRequestURI());
            //다음 체인
            filterChain.doFilter(request, response);
        } finally {
            //mdc 는 무슨 일이 있어도 정리 완료
            MDC.clear();
        }
    }
}
