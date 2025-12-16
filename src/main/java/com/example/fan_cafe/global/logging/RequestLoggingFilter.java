package com.example.fan_cafe.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.Logger;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        //Time Count 시작
        long startTime = System.currentTimeMillis();

        try {
           //다음 필터 -> Controller
           filterChain.doFilter(request, response);
        } finally {
            //응답이 나가기 직전 시간 기록
            long endTime = System.currentTimeMillis();

            //총 처리 시간 계산(ms 단위)
            long duration = endTime - startTime;

            //요청 정보 추출
            String method = request.getMethod(); //GET, POST, PUT ..
            String uri = request.getRequestURI(); // endPoint
            String queryString = request.getQueryString(); //쿼리 파라미터
            int status = response.getStatus();

            String fullUri = (queryString == null)
                    ? uri
                    : uri + "?" + queryString;

            //로그 출력
            log.info("AOP [{}] {} -> {} ({} ms)", method, fullUri, status, duration);
        }

    }
}
