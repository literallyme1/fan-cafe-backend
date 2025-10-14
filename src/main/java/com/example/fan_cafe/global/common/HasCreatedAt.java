package com.example.fan_cafe.global.common;

import java.time.LocalDateTime;

public interface HasCreatedAt {
    Long getId();
    LocalDateTime getCreatedAt();
}
