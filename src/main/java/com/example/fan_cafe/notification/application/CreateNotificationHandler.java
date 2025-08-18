package com.example.fan_cafe.notification.application;


import com.example.fan_cafe.comment.events.CommentCreatedEvent;
import com.example.fan_cafe.follow.events.FollowedEvent;
import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.NotificationPolicy;
import com.example.fan_cafe.notification.infrastructure.JpaNotificationRepository;
import com.example.fan_cafe.post.events.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateNotificationHandler {
    private final NotificationPolicy policy;
    private final JpaNotificationRepository repo;

    @Transactional
    public void onFollowed(FollowedEvent e){
        if (!policy.canNotify(e.targetId(), e.followerId())) return;
        repo.save(Notification.follow(e.targetId(), e.followerId(), e.followerName()));
    }

    @Transactional
    public void onComment(CommentCreatedEvent e){
        if (!policy.canNotify(e.postAuthorId(), e.authorId())) return;
        repo.save(Notification.comment(e.postAuthorId(), e.authorId(), e.postId(), e.commentId(), e.preview()));
    }

    @Transactional
    public void onLike(PostLikedEvent e){
        if (!policy.canNotify(e.postAuthorId(), e.likerId())) return;
        repo.save(Notification.like(e.postAuthorId(), e.likerId(), e.postId()));
    }
}
