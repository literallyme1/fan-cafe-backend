package com.example.fan_cafe.notification.domain;


import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class NotificationTarget {

    private Long postId;
    private Long commentId;

    public static NotificationTarget forComment(Long postId, Long commentId){
        NotificationTarget t = new NotificationTarget();
        t.postId = postId; t.commentId = commentId;
        return t;
    }

    public static NotificationTarget forPost(Long postId){
        NotificationTarget t = new NotificationTarget();
        t.postId = postId; return t;
    }

    public static NotificationTarget none(){ return new NotificationTarget(); }
}
