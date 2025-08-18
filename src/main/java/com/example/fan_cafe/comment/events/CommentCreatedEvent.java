package com.example.fan_cafe.comment.events;

public record CommentCreatedEvent(Long commentId, Long postId, Long authorId,
                                  Long postAuthorId, String preview) {}