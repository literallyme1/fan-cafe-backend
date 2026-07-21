package com.example.fan_cafe.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.global.logging.dto.LogLevelApplyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("!prod")
@Tag(name = "로그 운영", description = "개발 환경 도메인별 로그 레벨 제어")
public class LogLevelController {

    private final Map<String, String> domainLoggerPackages;

    public LogLevelController(
            @Qualifier("domainLoggerPackages") Map<String, String> domainLoggerPackages
    ) {
        this.domainLoggerPackages = domainLoggerPackages;
    }

    @PostMapping("/log-level")
    @Operation(summary = "로그 레벨 변경", description = "도메인 패키지의 런타임 로그 레벨을 변경함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "도메인 또는 로그 레벨 오류")
    })
    public LogLevelApplyResponse setLogLevel(
            @RequestParam String domain,
            @RequestParam String level
    ) {
        //패키지 이름을 가져옴. 프로젝트 전체 스캔 → domain 찾아냄
        String packageName = domainLoggerPackages.get(domain);
        if (packageName == null) {
            throw new CustomException(GlobalErrorCode.UNKNOWN_LOG_DOMAIN);
        }
        Level lbLevel = Level.toLevel(level.trim(), null);
        if (lbLevel == null) {
            throw new CustomException(GlobalErrorCode.INVALID_LOG_LEVEL);
        }
        Logger logger = (Logger) LoggerFactory.getLogger(packageName); //로그 조절 객체
        if (Logger.ROOT_LOGGER_NAME.equals(logger.getName())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        logger.setLevel(lbLevel);
        return new LogLevelApplyResponse(domain, lbLevel.toString(), "applied");
    }
}
