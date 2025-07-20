package com.example.fan_cafe.global.common;

import java.time.LocalDateTime;

public interface SoftDeletable {

    LocalDateTime getDeletedAt();
}
