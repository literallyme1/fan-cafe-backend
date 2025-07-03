package com.example.fan_cafe.post.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name= "posts")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private int viewCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int commentCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();


    public void update(String title, String content, List<String> imageUrls) {
        this.title = title;
        this.content = content;
        replaceImages(imageUrls);
    }
    public void addImages(List<String> imageUrls) {
        if (imageUrls == null) return;
        for (String url : imageUrls) {
            this.addImage(new PostImage(url));
        }
    }

    public void addImage(PostImage image) {
        images.add(image);
        image.setPost(this);
    }

    public void replaceImages(List<String> newUrls) {
        this.images.clear();
        addImages(newUrls);
    }

    public static Post of(User user, String title, String content, List<String> imageUrls) {
        Post post = Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .build();
        post.addImages(imageUrls);
        return post;
    }
}
