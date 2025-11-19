package wiki.kana.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import wiki.kana.dto.CommonResponse;
import wiki.kana.dto.post.PostCreateRequest;
import wiki.kana.dto.post.PostResponse;
import wiki.kana.dto.post.PostUpdateRequest;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.CategoryService;
import wiki.kana.service.PostService;
import wiki.kana.service.TagService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 博客文章控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PostController {

    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 获取博客列表
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponse>>> listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        Page<PostResponse> response = postService.findAllPosts(pageable)
                .map(this::toPostResponse);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 获取博客详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponse>> getPost(@PathVariable Long id) {
        try {
            Post post = postService.findById(id);
            postService.incrementViewCount(id);
            post.setViewCount(post.getViewCount() + 1);
            return ResponseEntity.ok(CommonResponse.success(toPostResponse(post)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 根据Slug获取博客
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CommonResponse<PostResponse>> getPostBySlug(@PathVariable String slug) {
        try {
            Post post = postService.findBySlug(slug);
            postService.incrementViewCount(post.getId());
            post.setViewCount(post.getViewCount() + 1);
            return ResponseEntity.ok(CommonResponse.success(toPostResponse(post)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 搜索博客
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> searchPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort) {

        Pageable pageable = buildPageable(page, size, sort);

        try {
            Page<Post> resultPage;

            if (category != null) {
                resultPage = postService.findByCategory(category, pageable);
            } else if (tag != null) {
                List<Post> posts = postService.findByTagId(tag);
                resultPage = buildPageFromList(posts, pageable);
            } else if (StringUtils.hasText(q)) {
                List<Post> posts = postService.searchByKeyword(q);
                resultPage = buildPageFromList(posts, pageable);
            } else {
                resultPage = postService.findAllPosts(pageable);
            }

            return ResponseEntity.ok(CommonResponse.success(resultPage.map(this::toPostResponse)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 创建博客
     */
    @PostMapping
    public ResponseEntity<CommonResponse<PostResponse>> createPost(
            HttpServletRequest request,
            @Valid @RequestBody PostCreateRequest createRequest) {

        Long authorId = resolveUserId(request);
        if (authorId == null) {
            return unauthorizedResponse();
        }

        try {
            Post post = buildPostFromCreateRequest(createRequest);
            Post created = postService.createPost(post, authorId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.success(toPostResponse(created), "博客创建成功"));
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to create post: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        } catch (DuplicateResourceException e) {
            log.warn("Duplicate resource when creating post: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error when creating post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }

    /**
     * 更新博客
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody PostUpdateRequest updateRequest) {

        Long authorId = resolveUserId(request);
        if (authorId == null) {
            return unauthorizedResponse();
        }

        try {
            Post updatedPost = buildPostFromUpdateRequest(updateRequest);
            Post result = postService.updatePost(id, updatedPost);
            return ResponseEntity.ok(CommonResponse.success(toPostResponse(result), "博客更新成功"));
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to update post {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error when updating post {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }

    /**
     * 删除博客
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deletePost(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long authorId = resolveUserId(request);
        if (authorId == null) {
            return unauthorizedResponse();
        }

        try {
            postService.deletePost(id);
            return ResponseEntity.ok(CommonResponse.success("博客删除成功"));
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to delete post {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 发布博客
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<CommonResponse<PostResponse>> publishPost(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long authorId = resolveUserId(request);
        if (authorId == null) {
            return unauthorizedResponse();
        }

        try {
            Post post = postService.publishPost(id);
            return ResponseEntity.ok(CommonResponse.success(toPostResponse(post), "博客已发布"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 取消发布博客
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<CommonResponse<PostResponse>> unpublishPost(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long authorId = resolveUserId(request);
        if (authorId == null) {
            return unauthorizedResponse();
        }

        try {
            Post post = postService.unpublishPost(id);
            return ResponseEntity.ok(CommonResponse.success(toPostResponse(post), "博客已转为草稿"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    private Pageable buildPageable(int page, int size, String sortParam) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Sort sort = Sort.by("createdAt").descending();
        if (StringUtils.hasText(sortParam)) {
            String[] parts = sortParam.split(",");
            String property = parts[0];
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) {
                sort = Sort.by(property).ascending();
            } else {
                sort = Sort.by(property).descending();
            }
        }

        return PageRequest.of(safePage, safeSize, sort);
    }

    private Long resolveUserId(HttpServletRequest request) {
        String token = jwtTokenUtil.extractTokenFromHeader(request.getHeader("Authorization"));
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return null;
        }
        return jwtTokenUtil.getUserIdFromToken(token);
    }

    private <T> ResponseEntity<CommonResponse<T>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.error("UNAUTHORIZED", "Token无效或已过期"));
    }

    private Post buildPostFromCreateRequest(PostCreateRequest request) {
        Post.PostBuilder builder = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .status(request.getStatus() != null ? request.getStatus() : Post.PostStatus.DRAFT)
                .isFeatured(Boolean.TRUE.equals(request.getIsFeatured()))
                .slug(request.getSlug());

        if (request.getCategoryId() != null) {
            builder.category(categoryService.findById(request.getCategoryId()));
        }

        List<Tag> tags = loadTags(request.getTagIds());
        if (!tags.isEmpty()) {
            builder.tags(tags);
        }

        return builder.build();
    }

    private Post buildPostFromUpdateRequest(PostUpdateRequest request) {
        Post.PostBuilder builder = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .status(request.getStatus())
                .isFeatured(request.getIsFeatured())
                .slug(request.getSlug());

        if (request.getCategoryId() != null) {
            builder.category(categoryService.findById(request.getCategoryId()));
        }

        if (!CollectionUtils.isEmpty(request.getTagIds())) {
            builder.tags(loadTags(request.getTagIds()));
        }

        return builder.build();
    }

    private List<Tag> loadTags(List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }

        return tagIds.stream()
                .map(tagService::findById)
                .collect(Collectors.toList());
    }

    private Page<Post> buildPageFromList(List<Post> posts, Pageable pageable) {
        if (posts == null) {
            posts = Collections.emptyList();
        }

        int start = Math.min((int) pageable.getOffset(), posts.size());
        int end = Math.min(start + pageable.getPageSize(), posts.size());
        List<Post> content = posts.subList(start, end);
        return new PageImpl<>(content, pageable, posts.size());
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .excerpt(post.getExcerpt())
                .status(post.getStatus())
                .isFeatured(post.getIsFeatured())
                .viewCount(post.getViewCount())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();

        if (post.getAuthor() != null) {
            response.setAuthor(PostResponse.AuthorDto.builder()
                    .id(post.getAuthor().getId())
                    .username(post.getAuthor().getUsername())
                    .nickname(post.getAuthor().getDisplayName())
                    .build());
        }

        if (post.getCategory() != null) {
            response.setCategory(PostResponse.CategoryDto.builder()
                    .id(post.getCategory().getId())
                    .name(post.getCategory().getName())
                    .slug(post.getCategory().getSlug())
                    .build());
        }

        if (!CollectionUtils.isEmpty(post.getTags())) {
            response.setTags(post.getTags().stream()
                    .map(tag -> PostResponse.TagDto.builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .slug(tag.getSlug())
                            .build())
                    .collect(Collectors.toList()));
        } else {
            response.setTags(Collections.emptyList());
        }

        return response;
    }
}
