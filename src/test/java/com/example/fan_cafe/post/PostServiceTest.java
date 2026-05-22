package com.example.fan_cafe.post;

import com.example.fan_cafe.bookmark.infrastructure.BookmarkRepository;
import com.example.fan_cafe.global.common.Cursor;
import com.example.fan_cafe.global.redis.RedisService;
import com.example.fan_cafe.like.application.LikeService;
import com.example.fan_cafe.like.domain.LikeTargetType;
import com.example.fan_cafe.like.infrastructure.LikeRepository;
import com.example.fan_cafe.post.application.PostHelper;
import com.example.fan_cafe.post.application.PostService;
import com.example.fan_cafe.post.domain.Post;
import com.example.fan_cafe.post.infrastructure.PostRepository;
import com.example.fan_cafe.post.interfaces.dto.*;
import com.example.fan_cafe.user.domain.Role;
import com.example.fan_cafe.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostHelper postHelper;

    @Mock
    BookmarkRepository bookmarkRepository;

    @Mock
    LikeService likeService;

    @Mock
    private RedisService redisService;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private Cache<String, Integer> commentCountLocalCache;

    @InjectMocks
    private PostService postService;

    User mockUser;
    Post mockPost;

    @BeforeEach
    void setUp() {
        lenient().when(redisService.getInt(anyString())).thenReturn(0);
        mockUser = User.of("test@test.com", "encode_pw", "nickname", Role.USER);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        mockPost = Post.builder()
                .id(1L)
                .user(mockUser)
                .title("오늘의 송가인")
                .content("오늘도 아름답네요")
                .build();
    }

    @Test
    void create_shouldPost_whenValidRequest() {
        //given

        PostCreateRequest request = PostCreateRequest.builder()
                .title("제목입니다.")
                .content("내용입니다.")
                .build();

        MultipartFile image = mock(MultipartFile.class);
        List<MultipartFile> imageList = List.of(image);

        List<String> uploadedUrls = List.of("new-url.jpg");
        when(postHelper.uploadImagesAndGetUrls(imageList, "post")).thenReturn(uploadedUrls);

        //when
        PostResponse response = postService.create(mockUser, request, imageList);

        //then
        verify(postRepository, times(1)).save(any(Post.class));
        assertThat(response.getTitle()).isEqualTo("제목입니다.");
    }


    @Test
    @DisplayName("커서가 없고, 최신 글이 하나 있으면 최신 글 하나 반환")
    void givenNoCursor_whenGetNewPosts_thenReturnFirstPost() {
        //given
        Cursor cursor = null;
        int size = 2;
        Long userId = 1L;

        when(postRepository.countNewPosts(any(Cursor.class))).thenReturn(1L);
        when(postRepository.findNewPosts(any(Cursor.class), eq(size), eq(userId))).thenReturn(List.of(
                new PostResponse(1L, "title1", "content1", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 16, 11, 0), List.of(), false, false)));

        //when
        PostListResponse result = postService.getNewPosts(cursor, size, userId);

        //then
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getAfterCursor()).isNotNull();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("커서가 주어졌을 때 커서보다 최신 글 반환")
    void givenCursor_whenGetNewPosts_thenReturnNewPosts() {
        //given
        Cursor cursor = new Cursor(10L, LocalDateTime.of(2025, 10, 16, 10, 10, 10));
        int size = 2;
        Long userId = 1L;

        when(postRepository.countNewPosts(any(Cursor.class))).thenReturn(3L);
        List<PostResponse> posts = List.of(
                new PostResponse(13L, "title1", "content1", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 17, 11, 0), List.of(), false, false),
                new PostResponse(12L, "title2", "content2", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 17, 11, 0), List.of(), false, false),
                new PostResponse(11L, "title3", "content3", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 17, 11, 0), List.of(), false, false)
        );
        when(postRepository.findNewPosts(any(Cursor.class), eq(size), eq(userId))).thenReturn(posts);

        //when
        PostListResponse result = postService.getNewPosts(cursor, size, userId);

        //then
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().getFirst().getId()).isGreaterThan(cursor.id());
        assertThat(result.getAfterCursor()).isNotNull();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("커서가 없을 때(첫 페이지): 캐시 미스 -> DB 조회 -> 유저 정보 조합 후 반환")
    void givenNoCursor_whenGetPosts_thenReturnFirstPage() throws Exception{
        // given
        Cursor cursor = null;
        int size = 2;
        Long userId = 1L;
        String fakeRedisKey = "post:list:latest:size:" + size;

        // 1. Redis: 캐시 미스 상황 가정
        when(redisService.get(anyString())).thenReturn(null);

        // 2. DB: CachedPostItem 리스트 반환
        List<CachedPostItem> dbItems = List.of(
                new CachedPostItem(10L, "title1", "content1", 1L, "user1", "url", 1, 1,LocalDateTime.now()),
                new CachedPostItem(9L, "title2", "content2", 2L, "user2", "url", 1, 1, LocalDateTime.now()),
                new CachedPostItem(8L, "title3", "content3", 3L, "user3", "url", 1, 1, LocalDateTime.now())
        );
        when(postRepository.findLatestCachedPosts(size)).thenReturn(dbItems);

        // 3. Enrichment: 좋아요/북마크 여부 (빈값 혹은 특정 값 설정)
        when(likeRepository.findLikedPostIds(anyLong(), anyList())).thenReturn(Set.of(10L)); // 10번 글 좋아요 함
        when(bookmarkRepository.findBookmarkedPostIds(anyLong(), anyList())).thenReturn(Set.of());

        // Redis 저장 로직 모킹 (에러 방지용)
        when(objectMapper.writeValueAsString(any())).thenReturn("json_string");

        // when
        PostListResponse result = postService.get(null, size, userId);

        // then
        // 1. 데이터 검증
        assertThat(result.getData()).hasSize(2); // 3개 가져와서 2개로 자름
        assertThat(result.getData().getFirst().getId()).isEqualTo(10L);
        assertThat(result.getData().getFirst().isLiked()).isTrue(); // 좋아요 반영 확인

        // 2. 커서 검증
        assertThat(result.getNextCursor()).isNotNull();
        assertThat(result.getAfterCursor()).isNotNull();
        assertThat(result.isHasNext()).isTrue();

        // 3. 호출 확인 (Verify)
        verify(postRepository).findLatestCachedPosts(size); // 이 메소드가 불렸는지 확인
        verify(redisService).set(eq(fakeRedisKey), anyString(), any()); // Redis 저장이 일어났는지 확인
    }

    @Test
    @DisplayName("커서가 주어졌을 때 커서 이후부터 반환")
    void givenCursor_whenGetPosts_thenReturnNextPage() {
        //given
        Cursor cursor = new Cursor(10L, LocalDateTime.of(2025, 10, 16, 10, 10, 10));
        int size = 2;
        Long userId = 1L;

        List<PostResponse> posts = List.of(
                new PostResponse(9L, "title1", "content1", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 16, 11, 0), List.of(), false, false),
                new PostResponse(8L, "title2", "content2", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 16, 11, 0), List.of(), false, false),
                new PostResponse(7L, "title3", "content3", 1L, "nickname", "avatar_url", 0,
                        0, LocalDateTime.of(2025, 10, 16, 11, 0), List.of(), false, false)
        );
        when(postRepository.findNextPage(any(Cursor.class), eq(size), eq(userId))).thenReturn(posts);

        //when
        PostListResponse result = postService.get(cursor, size, userId);

        //then
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().getFirst().getId()).isLessThan(cursor.id());
        assertThat(result.getNextCursor()).isNotNull();
        assertThat(result.getAfterCursor()).isNull();
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    void update_shouldUpdatePost_whenValidRequest() {
        //given

        Long postId = 1L;
        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("update")
                .content("update content")
                .imageUrls(List.of("old1.jpg"))
                .build();
        List<MultipartFile> images = List.of(mock(MultipartFile.class));
        List<String> uploadedUrls = List.of("new-uploaded.jpg");
        List<String> finalImageUrls = List.of("old1.jpg", "new-uploaded.jpg");

        when(postHelper.findByIdOrThrow(postId)).thenReturn(mockPost);
        doNothing().when(postHelper).validateOwner(mockUser, mockPost);

        when(postHelper.mergeImageUrls(eq(request.getImageUrls()), anyList()))
                .thenReturn(List.of("old1.jpg", "new-uploaded.jpg"));
        when(bookmarkRepository.existsByUserAndPost(mockUser, mockPost)).thenReturn(true);

        //when
        var response = postService.update(mockUser, postId, request, images);

        //then
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getImageUrls()).containsExactly("old1.jpg", "new-uploaded.jpg");

        assertThat(mockPost.getContent()).isEqualTo(request.getContent());
        assertThat(mockPost.getImageUrls()).containsExactly("old1.jpg", "new-uploaded.jpg");
    }

    @Test
    @DisplayName("toggleLike()가 true를 반환했을 때, toggleLike 를 호출하면 likecount 가 올라간다.")
    void givenIdAndIsLikedIsTrue_whenToggleLike_thenFetchIncreaseLikeCount() {
        //given
        User user = mock(User.class);
        Post post = mock(Post.class);
        Long postId = 1L;
        when(postHelper.findByIdOrThrow(anyLong())).thenReturn(post);
        when(likeService.toggleLike(any(User.class), anyLong(), any(LikeTargetType.class))).thenReturn(true);

        //when
        postService.toggleLike(user, postId);

        //then
        verify(post, times(1)).increaseLikeCount();
    }

    @Test
    @DisplayName("toggleLike()가 false를 반환했을 때, toggleLike 를 호출하면 likecount 가 감소한다.")
    void givenIdAndIsLikedIsFalse_whenToggleLike_thenFetchDecreaseLikeCount() {
        //given
        User user = mock(User.class);
        Post post = mock(Post.class);
        Long postId = 1L;
        when(postHelper.findByIdOrThrow(anyLong())).thenReturn(post);
        when(likeService.toggleLike(any(User.class), anyLong(), any(LikeTargetType.class))).thenReturn(false);

        //when
        postService.toggleLike(user, postId);

        //then
        verify(post, times(1)).decreaseLikeCount();
    }

}
