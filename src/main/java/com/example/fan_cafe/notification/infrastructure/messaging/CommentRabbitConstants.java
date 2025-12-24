package com.example.fan_cafe.notification.infrastructure.messaging;

//class final : 상속 금지
public final class CommentRabbitConstants {

    public static final String COMMENT_EXCHANGE = "comment.exchange";
    public static final String COMMENT_QUEUE = "comment.notification.queue";
    public static final String COMMENT_ROUTING_KEY = "comment.created";

    //객체가 아닌 네임 스페이스 임을 강조
    private CommentRabbitConstants() {}
}
