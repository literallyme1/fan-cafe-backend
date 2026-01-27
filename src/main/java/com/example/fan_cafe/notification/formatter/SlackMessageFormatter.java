package com.example.fan_cafe.notification.formatter;


import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.domain.NotificationLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//event -> slack 메세지 변환
public class SlackMessageFormatter {

    //slack 메세지 payload 생성
    public static Map<String, Object> format(NotificationEvent event) {

        Map<String, Object> payload = new HashMap<>();

        payload.put("blocks", List.of(
                headerBlock(event), //상단헤더 (심각도 + 타입)
                dividerBlock(), //구분선
                summaryBlock(event), //요약 메세지
                contextBlock(event)  // 시간 등 정보
        ));
        return payload;
    }

    //상단 헤더 블록 (심각도 타입)
    private static Map<String, Object> headerBlock(NotificationEvent event) {
        return Map.of(
                "type", "header",
                "text", Map.of(
                        "type", "plain_text",
                        //심각도 이모지 + 알림 타입
                        "text", levelLabel(event.getLevel())
                )
        );
    }

    //요약메세지 (title, description)
    private static Map<String, Object> summaryBlock(NotificationEvent event) {
        return Map.of(
                "type", "section",
                "text", Map.of(
                        "type", "mrkdwn",
                        "text",
                        // title은 항상 표시
                        "* [Title] " + event.getTitle() + "*\n"
                                // description은 null이 아니면 표시
                                + (event.getDescription() != null
                                ? event.getDescription()
                                : "")
                )

        );
    }

    //하단 컨텍스트 블록
    private static Map<String, Object> contextBlock(NotificationEvent event) {
        return Map.of(
                "type", "context",
                "elements", List.of(
                        Map.of(
                                "type","mrkdwn",
                                "text", "*Occured At*: " + event.getOccurredAt()
                         )
                )
        );
    }

    //구분선
    private static Map<String, Object> dividerBlock() {
        return Map.of("type", "divider");
    }

    //레벨 표기
    private static String levelLabel(NotificationLevel level) {
        return "[" + level.name() + "]";
    }
}
