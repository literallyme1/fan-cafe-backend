package com.example.fan_cafe.follow.interfaces.rest;


import com.example.fan_cafe.follow.application.FollowService;
import com.example.fan_cafe.follow.interfaces.dto.FollowerListResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{targetId}/follow")
    public void follow(@PathVariable Long targetId,
                       @AuthenticationPrincipal(expression = "user") User user) {
        followService.follow(user.getId(), targetId);
    }

    @DeleteMapping("/{targetId}/follow")
    public void unfollow(@PathVariable Long targetId,
                         @AuthenticationPrincipal(expression = "user") User user) {
        followService.unfollow(user.getId(), targetId);
    }

//    @GetMapping("/{id}/followers")
//    public FollowerListResponse followers(@PathVariable("id") Long targetId,
//                                          @AuthenticationPrincipal(expression = "user") User user,
//                                          @RequestParam(required = false) LocalDateTime cursorAt,
//                                          @RequestParam(required = false) Long cursorId,
//                                          @RequestParam(defaultValue = "20") int size) {
//        return followService.getFollowers(targetId, user.getId(), cursorAt, cursorId, size);
//    }

    @GetMapping("/{userId}/following")
    public FollowerListResponse getFollowing(@PathVariable("userId") Long userId,
                             @RequestParam(required = false)Cursor cursor,
                             @RequestParam(defaultValue = "20") int size){
        return followService.getFollowing(userId, cursor, size);
    }
}
