package com.example.fan_cafe.follow.domain;


import com.example.fan_cafe.global.common.HasCreatedAt;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
        name = "follow",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"follower_id", "following_id"}
        ))
public class Follow implements HasCreatedAt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;


    private Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
    }


    public static Follow create(User follower, User following) {
        return new Follow(follower, following);
    }
}