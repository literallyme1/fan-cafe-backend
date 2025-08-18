package com.example.fan_cafe.notification.interfaces.dto;

import com.example.fan_cafe.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResponse(
        List<NotificationItemResponse> items,
        boolean hasNext,
        Long nextId,
        LocalDateTime nextCreatedAt) {
    public static NotificationListResponse of(List<Notification> list, int size){
        boolean hasNext = list.size() > size;
        List<Notification> page = hasNext ? list.subList(0, size) : list;
        Notification last = page.isEmpty() ? null : page.get(page.size()-1);
        return new NotificationListResponse(
                page.stream().map(NotificationItemResponse::from).toList(),
                hasNext,
                last != null ? last.getId() : null,
                last != null ? last.getCreatedAt() : null
        );
    }
}