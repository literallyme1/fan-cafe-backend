package com.example.fan_cafe.bookmark.application;

import com.example.fan_cafe.bookmark.domain.Bookmark;
import com.example.fan_cafe.bookmark.exception.BookmarkErrorCode;
import com.example.fan_cafe.bookmark.infrastructure.BookmarkRepository;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkListItemResponse;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkListResponse;
import com.example.fan_cafe.bookmark.interfaces.dto.BookmarkResponse;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.util.PageUtils;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostHelper postHelper;

    @Transactional
    public BookmarkResponse add(User user, Long postId){
        Post post = postHelper.findByIdOrThrow(postId);
        Bookmark bookmark = Bookmark.of(user, post);

        try {
            bookmarkRepository.save(bookmark);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(BookmarkErrorCode.ALREADY_MARKED);
        }
        return BookmarkResponse.from(postId, true);
    }

    @Transactional
    public BookmarkResponse remove(User user, Long postId){
        Post post = postHelper.findByIdOrThrow(postId);
        Bookmark bookmark = bookmarkRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new CustomException(BookmarkErrorCode.MARKED_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
        return BookmarkResponse.from(postId, false);
    }

    public BookmarkListResponse get(int page, int size, User user){
        Pageable pageable = PageUtils.createPageRequest(page, size, "at", "DESC");
        Slice<BookmarkListItemResponse> slice = bookmarkRepository.findBookmarkResponsesByUser(user, pageable);
        return BookmarkListResponse.from(slice.getContent(), slice.hasNext());
    }
}
