package com.example.fan_cafe.post.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    public List<String> update(String title, String content, List<String> updatedImageUrls) {
        this.title = title;
        this.content = content;

        List<String> removedUrls = removeDeletedImages(updatedImageUrls);
        addNewImages(updatedImageUrls);
        reorderImages(updatedImageUrls);

        return removedUrls;
    }

    private List<String> removeDeletedImages(List<String> updatedImageUrls) {
        Set<String> updatedSet = new HashSet<>(updatedImageUrls);
        List<PostImage> toRemove = this.images.stream()
                .filter(img -> !updatedSet.contains(img.getImageUrl()))
                .toList();
        this.images.removeAll(toRemove);
        return toRemove.stream().map(PostImage::getImageUrl).toList();
    }

    private void addNewImages(List<String> updatedImageUrls) {
        Set<String> currentUrls = this.images.stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toSet());

        updatedImageUrls.stream()
                .filter(url -> !currentUrls.contains(url))
                .map(url -> PostImage.of(this, url))
                .forEach(this.images::add);
    }

    private void reorderImages(List<String> updatedImageUrls) {
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < updatedImageUrls.size(); i++) {
            orderMap.put(updatedImageUrls.get(i), i);
        }
        this.images.sort(Comparator.comparingInt(img -> orderMap.getOrDefault(img.getImageUrl(), Integer.MAX_VALUE)));
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


    public List<String> getImageUrls(){
        return this.images.stream()
                .map(PostImage::getImageUrl)
                .toList();
    }

    public String getThumbnailUrl(){
        return images != null && !images.isEmpty()
                ? images.getFirst().getImageUrl()
                : null;
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

    public void increaseLikeCount() {
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount -= 1;
        }
    }

}
