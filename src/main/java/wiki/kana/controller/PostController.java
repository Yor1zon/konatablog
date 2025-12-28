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
import wiki.kana.dto.post.PostMapper;
import wiki.kana.dto.post.PostResponse;
import wiki.kana.dto.post.PostTagsRequest;
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
public class PostController {

    private final PostService postService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 获取博客列表（仅显示已发布的博客）
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponse>>> listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        Page<PostResponse> response = postService.findPublishedPosts(pageable)
                .map(PostMapper::toPostResponse);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 获取博客详情（仅显示已发布的博客）
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponse>> getPost(@PathVariable Long id) {
        try {
            Post post = postService.findPublishedById(id);
            postService.incrementViewCount(id);
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(post)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 获取所有博客列表（管理端可见所有文章）
     */
    @GetMapping("/admin/all")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> listAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        Pageable pageable = buildPageable(page, size, sort);
        Page<PostResponse> response = postService.findAllPosts(pageable)
                .map(PostMapper::toPostResponse);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * 获取博客详情（管理端可见所有文章）
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<CommonResponse<PostResponse>> getPostForAdmin(@PathVariable Long id, HttpServletRequest request) {
        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Post post = postService.findById(id);
            // 管理端访问不增加浏览量
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(post)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 根据Slug获取博客（仅显示已发布的博客）
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CommonResponse<PostResponse>> getPostBySlug(@PathVariable String slug) {
        try {
            Post post = postService.findPublishedBySlug(slug);
            postService.incrementViewCount(post.getId());
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(post)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 根据Slug获取博客（管理端可见所有文章）
     */
    @GetMapping("/admin/slug/{slug}")
    public ResponseEntity<CommonResponse<PostResponse>> getPostBySlugForAdmin(@PathVariable String slug, HttpServletRequest request) {
        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Post post = postService.findBySlug(slug);
            // 管理端访问不增加浏览量
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(post)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("POST_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 搜索博客（仅搜索已发布的博客）
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
                // 过滤出已发布的博客
                List<Post> publishedPosts = resultPage.getContent().stream()
                        .filter(post -> post.getStatus() == Post.PostStatus.PUBLISHED)
                        .collect(Collectors.toList());
                resultPage = buildPageFromList(publishedPosts, pageable);
            } else if (tag != null) {
                List<Post> posts = postService.findByTagId(tag);
                resultPage = buildPageFromList(posts, pageable);
            } else if (StringUtils.hasText(q)) {
                List<Post> posts = postService.searchByKeyword(q);
                resultPage = buildPageFromList(posts, pageable);
            } else {
                resultPage = postService.findPublishedPosts(pageable);
            }

            return ResponseEntity.ok(CommonResponse.success(resultPage.map(PostMapper::toPostResponse)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 按标签查询已发布文章
     */
    @GetMapping("/filter/tag/{tagId}")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> listPostsByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort) {

        Pageable pageable = buildPageable(page, size, sort);

        try {
            Page<PostResponse> posts = postService.findPublishedByTag(tagId, pageable)
                    .map(PostMapper::toPostResponse);
            return ResponseEntity.ok(CommonResponse.success(posts));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 按年份查询已发布文章
     */
    @GetMapping("/filter/year/{year}")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> listPostsByYear(
            @PathVariable int year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort) {

        Pageable pageable = buildPageable(page, size, sort);

        try {
            Page<PostResponse> posts = postService.findPublishedByYear(year, pageable)
                    .map(PostMapper::toPostResponse);
            return ResponseEntity.ok(CommonResponse.success(posts));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "年份参数无效"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "年份参数无效"));
        }
    }

    @GetMapping("/admin/search")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> searchAllPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

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

            return ResponseEntity.ok(CommonResponse.success(resultPage.map(PostMapper::toPostResponse)));
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
                    .body(CommonResponse.success(PostMapper.toPostResponse(created), "博客创建成功"));
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
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(result), "博客更新成功"));
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
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(post), "博客已发布"));
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
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(post), "博客已转为草稿"));
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

    /**
     * 添加单个标签到文章
     */
    @PostMapping("/{id}/tags/{tagId}")
    public ResponseEntity<CommonResponse<Void>> addTagToPost(
            @PathVariable Long id,
            @PathVariable Long tagId,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            postService.addTagToPost(id, tagId);
            return ResponseEntity.ok(CommonResponse.success("标签添加成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 从文章移除单个标签
     */
    @DeleteMapping("/{id}/tags/{tagId}")
    public ResponseEntity<CommonResponse<Void>> removeTagFromPost(
            @PathVariable Long id,
            @PathVariable Long tagId,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            postService.removeTagFromPost(id, tagId);
            return ResponseEntity.ok(CommonResponse.success("标签移除成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 批量设置文章标签
     */
    @PutMapping("/{id}/tags")
    public ResponseEntity<CommonResponse<PostResponse>> setPostTags(
            @PathVariable Long id,
            @Valid @RequestBody PostTagsRequest request,
            HttpServletRequest httpRequest) {

        if (resolveUserId(httpRequest) == null) {
            return unauthorizedResponse();
        }

        try {
            Post updated = postService.setPostTags(id, request.getTagIds());
            return ResponseEntity.ok(CommonResponse.success(PostMapper.toPostResponse(updated), "标签设置成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        }
    }

}
