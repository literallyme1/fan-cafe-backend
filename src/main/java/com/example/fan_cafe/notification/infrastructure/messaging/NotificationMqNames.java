package com.example.fan_cafe.notification.infrastructure.messaging;

public final class NotificationMqNames {

    // Main
    public static final String MAIN_EXCHANGE = "notif.main.ex";
    public static final String MAIN_QUEUE = "notif.main.q";
    public static final String MAIN_ROUTING_KEY = "notif.main";

    // Retry
    public static final String RETRY_EXCHANGE = "notif.retry.ex";
    public static final String RETRY_5S_QUEUE = "notif.retry.5s.q";
    public static final String RETRY_30S_QUEUE = "notif.retry.30s.q";
    public static final String RETRY_5S_ROUTING_KEY = "notif.retry.5s";
    public static final String RETRY_30S_ROUTING_KEY = "notif.retry.30s";

    // TTL
    public static final int RETRY_5S_TTL_MS = 5_000;
    public static final int RETRY_30S_TTL_MS = 30_000;

    private NotificationMqNames(){ }
}
