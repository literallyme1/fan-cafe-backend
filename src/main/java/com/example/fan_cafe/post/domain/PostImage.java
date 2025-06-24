package com.example.fan_cafe.post.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name="postImages")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String imageUrl;

    public PostImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}
