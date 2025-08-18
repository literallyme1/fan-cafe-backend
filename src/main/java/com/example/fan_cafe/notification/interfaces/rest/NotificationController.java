package com.example.fan_cafe.notification.interfaces.rest;

import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.notification.application.NotificationService;
import com.example.fan_cafe.notification.interfaces.dto.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public NotificationListResponse list(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @RequestParam(required=false) Long cursorId,
            @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorAt,
            @RequestParam(defaultValue = "20") int size){
        return service.list(userId, new Cursor(cursorId, cursorAt), size);
    }

    @GetMapping("/badge")
    public Map<String, Long> unreadCount(
            @AuthenticationPrincipal(expression = "user.id") Long userId){
        return Map.of("unread", service.countUnread(userId));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long id){
        service.markRead(userId, id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(
            @AuthenticationPrincipal(expression = "user.id") Long userId){
        service.markAllRead(userId);
    }
}
