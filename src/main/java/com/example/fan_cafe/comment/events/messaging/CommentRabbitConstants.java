package com.example.fan_cafe.comment.events.messaging;

//class final : 상속 금지
public final class CommentRabbitConstants {

    public static final String COMMENT_EXCHANGE = "comment.exchange";
    public static final String COMMENT_QUEUE = "comment.notification.queue";
    public static final String COMMENT_ROUTING_KEY = "comment.created";

    //DLQ
    public static final String COMMENT_DLX = "comment.dlx"; //저장할 곳
    public static final String COMMENT_DLQ = "comment.notification.dlq";
    public static final String COMMENT_DLQ_ROUTING_KEY = "comment.created.dlq";
    //객체가 아닌 네임 스페이스 임을 강조
    private CommentRabbitConstants() {}
}
