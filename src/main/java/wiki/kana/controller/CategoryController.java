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
import wiki.kana.dto.category.CategoryReorderRequest;
import wiki.kana.dto.category.CategoryRequest;
import wiki.kana.dto.category.CategoryResponse;
import wiki.kana.dto.category.CategoryStatsResponse;
import wiki.kana.dto.category.CategoryTreeResponse;
import wiki.kana.dto.category.CategoryUpdateRequest;
import wiki.kana.dto.post.PostMapper;
import wiki.kana.dto.post.PostResponse;
import wiki.kana.entity.Category;
import wiki.kana.entity.Post;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.CategoryService;
import wiki.kana.service.PostService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final PostService postService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 获取分类列表
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<CategoryResponse>>> listCategories(
            @RequestParam(defaultValue = "true") boolean includeCounts,
            @RequestParam(required = false) Long parentId) {

        List<Category> categories = parentId != null
                ? categoryService.findChildren(parentId)
                : categoryService.findAll();

        List<CategoryResponse> responses = categories.stream()
                .map(category -> toCategoryResponse(category, includeCounts))
                .collect(Collectors.toList());

        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<CategoryResponse>> getCategory(@PathVariable Long id) {
        try {
            Category category = categoryService.findById(id);
            return ResponseEntity.ok(CommonResponse.success(toCategoryResponse(category, true)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("CATEGORY_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 根据Slug获取分类
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CommonResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        try {
            Category category = categoryService.findBySlug(slug);
            return ResponseEntity.ok(CommonResponse.success(toCategoryResponse(category, true)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("CATEGORY_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 获取分类树
     */
    @GetMapping("/tree")
    public ResponseEntity<CommonResponse<List<CategoryTreeResponse>>> getCategoryTree(
            @RequestParam(defaultValue = "false") boolean includeEmpty) {

        List<CategoryTreeResponse> tree = categoryService.findAllTopLevelCategories().stream()
                .map(category -> buildTreeNode(category, includeEmpty))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CommonResponse.success(tree));
    }

    /**
     * 获取分类下的文章
     */
    @GetMapping("/{id}/posts")
    public ResponseEntity<CommonResponse<Page<PostResponse>>> getCategoryPosts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt,desc") String sort,
            @RequestParam(required = false) String status) {

        Pageable pageable = buildPageable(page, size, sort);

        try {
            Page<Post> posts = postService.findByCategory(id, pageable);

            if (StringUtils.hasText(status)) {
                Post.PostStatus desiredStatus = Post.PostStatus.valueOf(status.toUpperCase());
                List<Post> filtered = posts.getContent().stream()
                        .filter(post -> desiredStatus.equals(post.getStatus()))
                        .collect(Collectors.toList());
                posts = buildPageFromList(filtered, pageable);
            }

            return ResponseEntity.ok(CommonResponse.success(posts.map(PostMapper::toPostResponse)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "无效的状态参数"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("CATEGORY_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 创建分类
     */
    @PostMapping
    public ResponseEntity<CommonResponse<CategoryResponse>> createCategory(
            HttpServletRequest request,
            @Valid @RequestBody CategoryRequest categoryRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Category category = Category.builder()
                    .name(categoryRequest.getName())
                    .description(categoryRequest.getDescription())
                    .slug(categoryRequest.getSlug())
                    .sortOrder(categoryRequest.getSortOrder())
                    .isActive(categoryRequest.getIsActive())
                    .build();

            Category created = categoryService.createCategory(category, categoryRequest.getParentId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.success(toCategoryResponse(created, true), "分类创建成功"));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("CATEGORY_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            HttpServletRequest request,
            @Valid @RequestBody CategoryUpdateRequest categoryRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Category updateData = Category.builder()
                    .name(categoryRequest.getName())
                    .description(categoryRequest.getDescription())
                    .slug(categoryRequest.getSlug())
                    .sortOrder(categoryRequest.getSortOrder())
                    .isActive(categoryRequest.getIsActive())
                    .build();

            Category updated = categoryService.updateCategory(id, updateData, categoryRequest.getParentId());
            return ResponseEntity.ok(CommonResponse.success(toCategoryResponse(updated, true), "分类更新成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("CATEGORY_NOT_FOUND", e.getMessage()));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update category {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteCategory(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(CommonResponse.success("分类删除成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("CATEGORY_NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("CATEGORY_IN_USE", e.getMessage()));
        }
    }

    /**
     * 批量调整分类顺序
     */
    @PatchMapping("/reorder")
    public ResponseEntity<CommonResponse<Void>> reorderCategories(
            HttpServletRequest request,
            @Valid @RequestBody CategoryReorderRequest reorderRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        Map<Long, Integer> sortOrders = reorderRequest.getOrders().stream()
                .collect(Collectors.toMap(CategoryReorderRequest.OrderItem::getId, CategoryReorderRequest.OrderItem::getOrder));

        categoryService.updateCategorySortOrders(sortOrders);
        return ResponseEntity.ok(CommonResponse.success("分类顺序已更新"));
    }

    /**
     * 分类统计
     */
    @GetMapping("/stats")
    public ResponseEntity<CommonResponse<CategoryStatsResponse>> getCategoryStats(HttpServletRequest request) {
        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        List<Category> allCategories = categoryService.findAll();
        List<Category> categoriesWithPosts = categoryService.findCategoriesWithPublishedPosts();
        long total = allCategories.size();
        long withPosts = categoriesWithPosts.size();
        long empty = Math.max(total - withPosts, 0);

        List<CategoryStatsResponse.CategorySummary> topCategories = categoryService.findCategoriesOrderByPostCount().stream()
                .limit(5)
                .map(category -> CategoryStatsResponse.CategorySummary.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .postCount(categoryService.countPublishedPosts(category.getId()))
                        .build())
                .collect(Collectors.toList());

        CategoryStatsResponse statsResponse = CategoryStatsResponse.builder()
                .totalCategories(total)
                .categoriesWithPosts(withPosts)
                .emptyCategories(empty)
                .topCategories(topCategories)
                .build();

        return ResponseEntity.ok(CommonResponse.success(statsResponse));
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

    private Page<Post> buildPageFromList(List<Post> posts, Pageable pageable) {
        if (CollectionUtils.isEmpty(posts)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int start = Math.min((int) pageable.getOffset(), posts.size());
        int end = Math.min(start + pageable.getPageSize(), posts.size());
        List<Post> content = posts.subList(start, end);
        return new PageImpl<>(content, pageable, posts.size());
    }

    private CategoryResponse toCategoryResponse(Category category, boolean includeCounts) {
        Long postCount = includeCounts ? categoryService.countPublishedPosts(category.getId()) : null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .postCount(postCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private CategoryTreeResponse buildTreeNode(Category category, boolean includeEmpty) {
        long postCount = categoryService.countPublishedPosts(category.getId());

        List<CategoryTreeResponse> children = category.getChildren() == null
                ? Collections.emptyList()
                : category.getChildren().stream()
                .map(child -> buildTreeNode(child, includeEmpty))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!includeEmpty && postCount == 0 && children.isEmpty()) {
            return null;
        }

        return CategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .postCount(postCount)
                .children(children)
                .build();
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
