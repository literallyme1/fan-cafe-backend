package com.example.fan_cafe.like.infrastructure;

import com.example.fan_cafe.like.domain.Like;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long>, LikeRepositoryCustom  {

    List<Like> findByUser(User user);
    Optional<Like> findByUserAndPost(User user, Post post);
}
