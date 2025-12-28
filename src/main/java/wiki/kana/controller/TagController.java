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
import wiki.kana.dto.post.PostMapper;
import wiki.kana.dto.post.PostResponse;
import wiki.kana.dto.tag.TagBulkRequest;
import wiki.kana.dto.tag.TagPostsRequest;
import wiki.kana.dto.tag.TagRequest;
import wiki.kana.dto.tag.TagResponse;
import wiki.kana.dto.tag.TagSmartCreateRequest;
import wiki.kana.dto.tag.TagUpdateRequest;
import wiki.kana.dto.post.PostSummaryResponse;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.PostService;
import wiki.kana.service.TagService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final PostService postService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 获取全部标签
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<TagResponse>>> listTags() {
        List<TagResponse> data = tagService.findAllOrderByName().stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CommonResponse.success(data));
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<TagResponse>> getTag(@PathVariable Long id) {
        try {
            Tag tag = tagService.findById(id);
            return ResponseEntity.ok(CommonResponse.success(toTagResponse(tag)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 根据slug获取标签
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CommonResponse<TagResponse>> getTagBySlug(@PathVariable String slug) {
        try {
            Tag tag = tagService.findBySlug(slug);
            return ResponseEntity.ok(CommonResponse.success(toTagResponse(tag)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 热门标签
     */
    @GetMapping("/popular")
    public ResponseEntity<CommonResponse<List<TagResponse>>> getPopularTags(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<TagResponse> tags = tagService.getPopularTags(safeLimit).stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CommonResponse.success(tags));
    }

    /**
     * 标签模糊搜索
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResponse<Page<TagResponse>>> searchTags(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean ignoreCase) {

        if (!StringUtils.hasText(q)) {
            Page<TagResponse> empty = new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), 0);
            return ResponseEntity.ok(CommonResponse.success(empty));
        }

        Pageable pageable = buildPageable(page, size, "name,asc");
        List<Tag> result = ignoreCase ?
                tagService.searchByNameIgnoreCase(q) :
                tagService.searchByName(q);
        Page<Tag> pageData = buildPageFromList(result, pageable);
        return ResponseEntity.ok(CommonResponse.success(pageData.map(this::toTagResponse)));
    }

    /**
     * 标签输入建议
     */
    @GetMapping("/suggestions")
    public ResponseEntity<CommonResponse<List<TagResponse>>> suggestTags(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "true") boolean ignoreCase) {

        if (!StringUtils.hasText(q)) {
            return ResponseEntity.ok(CommonResponse.success(Collections.emptyList()));
        }

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Tag> searchResult = ignoreCase ?
                tagService.searchByNameIgnoreCase(q) :
                tagService.searchByName(q);

        List<TagResponse> suggestions = searchResult.stream()
                .limit(safeLimit)
                .map(this::toTagResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CommonResponse.success(suggestions));
    }

    /**
     * 获取标签下文章
     */
    @GetMapping("/{id}/posts")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> getTagPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort,
            @RequestParam(required = false) String status) {

        Pageable pageable = buildPageable(page, size, sort);

        try {
            List<Post> posts = postService.findByTagId(id);
            if (StringUtils.hasText(status)) {
                Post.PostStatus desired = Post.PostStatus.valueOf(status.toUpperCase());
                posts = posts.stream()
                        .filter(post -> desired.equals(post.getStatus()))
                        .collect(Collectors.toList());
            }

            Page<Post> pageData = buildPageFromList(posts, pageable);
            return ResponseEntity.ok(CommonResponse.success(pageData.map(PostMapper::toPostResponse)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "无效的状态参数"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 创建标签
     */
    @PostMapping
    public ResponseEntity<CommonResponse<TagResponse>> createTag(
            HttpServletRequest request,
            @Valid @RequestBody TagRequest tagRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Tag tag = Tag.builder()
                    .name(tagRequest.getName())
                    .slug(tagRequest.getSlug())
                    .description(tagRequest.getDescription())
                    .color(tagRequest.getColor())
                    .build();

            Tag created = tagService.createTag(tag);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.success(toTagResponse(created), "标签创建成功"));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<TagResponse>> updateTag(
            @PathVariable Long id,
            HttpServletRequest request,
            @Valid @RequestBody TagUpdateRequest tagRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Tag updateData = Tag.builder()
                    .name(tagRequest.getName())
                    .slug(tagRequest.getSlug())
                    .description(tagRequest.getDescription())
                    .color(tagRequest.getColor())
                    .build();

            Tag updated = tagService.updateTag(id, updateData);
            return ResponseEntity.ok(CommonResponse.success(toTagResponse(updated), "标签更新成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteTag(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") boolean force) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            if (force) {
                tagService.forceDeleteTag(id);
            } else {
                tagService.deleteTag(id);
            }
            return ResponseEntity.ok(CommonResponse.success("标签删除成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("TAG_IN_USE", e.getMessage()));
        }
    }

    /**
     * 批量创建或绑定标签
     */
    @PostMapping("/bulk")
    public ResponseEntity<CommonResponse<List<TagResponse>>> bulkCreateTags(
            HttpServletRequest request,
            @Valid @RequestBody TagBulkRequest tagBulkRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        List<TagResponse> responses = tagService.getOrCreateTags(tagBulkRequest.getNames()).stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    /**
     * 智能创建标签接口
     */
    @PostMapping("/smart-create")
    public ResponseEntity<CommonResponse<TagResponse>> smartCreateTag(
            HttpServletRequest request,
            @Valid @RequestBody TagSmartCreateRequest smartCreateRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Tag tag = tagService.findOrCreateByName(
                    smartCreateRequest.getName(),
                    smartCreateRequest.getDescription(),
                    smartCreateRequest.getColor()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.success(toTagResponse(tag), "标签创建成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 获取标签关联文章简要信息
     */
    @GetMapping("/{id}/posts-summary")
    public ResponseEntity<CommonResponse<Page<PostSummaryResponse>>> getTagPostsSummary(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = buildPageable(page, size, "createdAt,desc");

        try {
            List<Post> posts = tagService.getTagPostsSummary(id, status);
            Page<Post> pageData = buildPageFromList(posts, pageable);
            return ResponseEntity.ok(CommonResponse.success(pageData.map(PostSummaryResponse::fromPost)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "无效的状态参数"));
        }
    }

    /**
     * 批量添加文章到标签
     */
    @PostMapping("/{id}/posts")
    public ResponseEntity<CommonResponse<Void>> addPostsToTag(
            @PathVariable Long id,
            @Valid @RequestBody TagPostsRequest request,
            HttpServletRequest httpRequest) {

        if (resolveUserId(httpRequest) == null) {
            return unauthorizedResponse();
        }

        try {
            int addedCount = request.getPostIds().size();
            tagService.addPostsToTag(id, request.getPostIds());
            return ResponseEntity.ok(CommonResponse.success(
                    String.format("批量添加成功，共添加%d篇文章", addedCount)
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 批量从标签移除文章
     */
    @DeleteMapping("/{id}/posts")
    public ResponseEntity<CommonResponse<Void>> removePostsFromTag(
            @PathVariable Long id,
            @Valid @RequestBody TagPostsRequest request,
            HttpServletRequest httpRequest) {

        if (resolveUserId(httpRequest) == null) {
            return unauthorizedResponse();
        }

        try {
            int removedCount = request.getPostIds().size();
            tagService.removePostsFromTag(id, request.getPostIds());
            return ResponseEntity.ok(CommonResponse.success(
                    String.format("批量移除成功，共移除%d篇文章", removedCount)
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("TAG_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    private TagResponse toTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .description(tag.getDescription())
                .usageCount(tag.getUsageCount())
                .postCount(tag.getUsageCount())
                .color(tag.getColor())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
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

    private <T> Page<T> buildPageFromList(List<T> data, Pageable pageable) {
        if (CollectionUtils.isEmpty(data)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int start = Math.min((int) pageable.getOffset(), data.size());
        int end = Math.min(start + pageable.getPageSize(), data.size());
        List<T> content = data.subList(start, end);
        return new PageImpl<>(content, pageable, data.size());
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
}
