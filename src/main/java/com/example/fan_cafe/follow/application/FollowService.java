package com.example.fan_cafe.follow.application;


import com.example.fan_cafe.follow.domain.Follow;
import com.example.fan_cafe.follow.domain.FollowPolicy;
import com.example.fan_cafe.follow.exception.FollowErrorCode;
import com.example.fan_cafe.follow.infrastructure.FollowRepository;
import com.example.fan_cafe.follow.interfaces.dto.FollowResponse;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.common.CursorResolver;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.util.CursorUtils;

import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.exception.UserErrorCode;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
//    private final FollowPolicy followPolicy;

    @Transactional
    public void follow(Long followerId, Long followingId){

        //user 가 실제 존재하는 지 확인 후 User 를 가져옴.
        validateUser(followerId, followingId);

        //Follow 타당성 여부 확인
//        followPolicy.validate(followerId, followingId);

        User follower = userRepository.getReferenceById(followerId);
        User following = userRepository.getReferenceById(followingId);
        Follow entity = Follow.create(follower, following);

        //각 followCount 를 증가시킨다.
        entity.getFollower().increaseFollowingCount(); //내가 following 함
        entity.getFollowing().increaseFollowerCount();

        //저장
        followRepository.save(entity);


        // TODO: 이벤트 발행 (알림/피드)
    }

    private void validateUser(Long followerId, Long followingId){
        long userCount = userRepository.countByIdIn(List.of(followerId, followingId));
        if(userCount < 2) { throw new CustomException(UserErrorCode.USER_NOT_FOUND);}
    }


    @Transactional
    public void unfollow(Long followerId, Long followingId) {

        //해당 follow 가져오고 각 user decrease 후 삭제
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                        .orElseThrow(() -> new CustomException(FollowErrorCode.FOLLOW_NOT_FOUND));

        follow.getFollower().decreaseFollowingCount();
        follow.getFollowing().decreaseFollowerCount();

        followRepository.delete(follow);

        // TODO: 이벤트 발행
    }

//    public FollowingListResponse getFollowingList(Long userId, Cursor cursor, int size) {
//
//        Cursor resolvedCursor = getResolvedCursor(cursor);
//        List<FollowResponse> follows = followRepository.findNextPage(resolvedCursor, size, userId);
//        PageSlice paging = computePageSlice(follows, size);
//        return FollowingListResponse.from(paging.follows(), paging.nextCursor());
//
//    }

    private Cursor getResolvedCursor(Cursor cursor) {
        return (cursor != null) ?
                CursorResolver.resolve(cursor.id(), cursor.at(), followRepository::findTopByOrderByCreatedAtDesc)
                : CursorResolver.resolve(null, null, followRepository::findTopByOrderByCreatedAtDesc);
    }

    //반환 할 beforeCursor 생성
    private PageSlice computePageSlice(List<FollowResponse> follows, int size) {

        Cursor nextCursor = (follows.size() > size) ? CursorUtils.fromLast(follows) : null;
        //hasNext 확인 후 size 대로 cut
        if (nextCursor != null) follows = follows.subList(0, size);
        return new PageSlice(follows, nextCursor);
    }

    private record PageSlice(List<FollowResponse> follows, Cursor nextCursor) {
    }
}
