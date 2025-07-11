package com.example.fan_cafe.post.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
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

    public static PostImage of(Post post, String url){
        return PostImage.builder()
                .post(post)
                .imageUrl(url)
                .build();
    }
}
