package com.example.fan_cafe.notification.infrastructure;

import com.example.fan_cafe.comment.events.CommentCreatedEvent;
import com.example.fan_cafe.follow.events.FollowedEvent;
import com.example.fan_cafe.notification.application.CreateNotificationHandler;
import com.example.fan_cafe.post.events.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AmqpEventConsumer {
    private final CreateNotificationHandler handler;

//    @RabbitListener(queues = "events.followed")
//    public void consumeFollowed(FollowedEvent e){ handler.onFollowed(e); }
//
//    @RabbitListener(queues = "events.comment.created")
//    public void consumeComment(CommentCreatedEvent e){ handler.onComment(e); }
//
//    @RabbitListener(queues = "events.post.liked")
//    public void consumeLike(PostLikedEvent e){ handler.onLike(e); }
}
