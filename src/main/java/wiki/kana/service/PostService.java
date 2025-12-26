package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.*;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 博客文章服务类
 * 负责博客相关的业务逻辑处理
 *
 * @author Kana Kana Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    // ==================== 查找方法 ====================

    /**
     * 根据ID查找博客
     */
    public Post findById(Long id) {
        log.debug("查找博客 ID: {}", id);
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    /**
     * 根据ID查找已发布的博客（公共访问）
     */
    @Transactional(readOnly = true)
    public Post findPublishedById(Long id) {
        log.debug("Finding published post by ID: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Published post not found with id: " + id);
        }

        return post;
    }

    /**
     * 根据Slug查找博客
     */
    public Post findBySlug(String slug) {
        log.debug("Finding post by slug: {}", slug);
        return postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));
    }

    /**
     * 根据Slug查找已发布的博客（公共访问）
     */
    @Transactional(readOnly = true)
    public Post findPublishedBySlug(String slug) {
        log.debug("Finding published post by slug: {}", slug);
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));

        if (post.getStatus() != Post.PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Published post not found with slug: " + slug);
        }

        return post;
    }

    /**
     * 查找所有博客
     */
    public List<Post> findAll() {
        log.debug("查询全部博客");
        return postRepository.findAll();
    }

    /**
     * 查找所有已发布的博客（公共访问）
     */
    @Transactional(readOnly = true)
    public List<Post> findPublishedPosts() {
        log.debug("查询所有已发布的博客");
        return postRepository.findPublishedPosts();
    }

    /**
     * 按发布时间倒序查找所有博客
     */
    public List<Post> findAllOrderByPublishedAtDesc() {
        log.debug("按时间排序博客列表");
        return postRepository.findAll()
                .stream()
                .sorted((p1, p2) -> {
                    LocalDateTime time1 = p1.getPublishedAt() != null ? p1.getPublishedAt() : LocalDateTime.MIN;
                    LocalDateTime time2 = p2.getPublishedAt() != null ? p2.getPublishedAt() : LocalDateTime.MIN;
                    return time2.compareTo(time1);
                })
                .collect(Collectors.toList());
    }

    /**
     * 分页查询博客
     */
    @Transactional(readOnly = true)
    public Page<Post> findAllPosts(Pageable pageable) {
        log.debug("分页查询博客, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return postRepository.findAll(pageable);
    }

    /**
     * 分页查询已发布的博客（公共访问）
     */
    @Transactional(readOnly = true)
    public Page<Post> findPublishedPosts(Pageable pageable) {
        log.debug("分页查询已发布的博客, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        List<Post> publishedPosts = postRepository.findPublishedPosts(pageable);

        // 转换为Page对象
        int start = Math.min((int) pageable.getOffset(), publishedPosts.size());
        int end = Math.min(start + pageable.getPageSize(), publishedPosts.size());
        List<Post> content = publishedPosts.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(content, pageable, publishedPosts.size());
    }

    // ==================== 搜索功能 ====================

    /**
     * 搜索博客 - 按标题和内容
     */
    public List<Post> searchByKeyword(String keyword) {
        log.info("搜索博客，关键词: {}", keyword);
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        List<Post> posts = new ArrayList<>();
        // 仅搜索已发布文章（与仓库层保持一致）
        posts.addAll(postRepository.searchByTitle(keyword));
        posts.addAll(postRepository.searchByContent(keyword));

        // 去重
        Set<Post> uniquePosts = new HashSet<>(posts);
        log.debug("找到 {} 篇博客匹配关键词", uniquePosts.size());

        return new ArrayList<>(uniquePosts);
    }

    /**
     * 按状态搜索
     */
    @Transactional(readOnly = true)
    public List<Post> findByStatus(Post.PostStatus status) {
        log.debug("搜索状态为 {} 的博客", status);
        return postRepository.findByStatus(status);
    }

    /**
     * 分页搜索
     */
    public Page<Post> searchPosts(String keyword, Boolean isFeatured, Pageable pageable) {
        log.debug("搜索博客: keyword={}, featured={}, page={} size={}", keyword, isFeatured, pageable.getPageNumber(), pageable.getPageSize());

        // 这里可以添加更复杂的搜索逻辑
        // 目前使用分页接口
        return postRepository.findAll(pageable);
    }

    /**
     * 按作者搜索
     */
    public Page<Post> findByAuthor(Long authorId, Pageable pageable) {
        log.debug("查询作者 {} 的博客", authorId);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + authorId));

        return postRepository.findByAuthor(author, pageable);
    }

    /**
     * 按分类搜索
     */
    public Page<Post> findByCategory(Long categoryId, Pageable pageable) {
        log.debug("查询分类 {} 的博客", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        return postRepository.findByCategory(category, pageable);
    }

    /**
     * 按标签搜索
     */
    @Transactional(readOnly = true)
    public List<Post> findByTagId(Long tagId) {
        log.debug("查询标签 {} 的博客", tagId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        return postRepository.findByTag(tag);
    }

    /**
     * 分页根据标签查询已发布文章
     */
    @Transactional(readOnly = true)
    public Page<Post> findPublishedByTag(Long tagId, Pageable pageable) {
        log.debug("Paginate published posts by tag {}", tagId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));
        return postRepository.findPublishedByTag(tag, pageable);
    }

    /**
     * 分页根据年份查询已发布文章
     */
    @Transactional(readOnly = true)
    public Page<Post> findPublishedByYear(int year, Pageable pageable) {
        if (year < 1970 || year > 9999) {
            throw new IllegalArgumentException("Invalid year: " + year);
        }

        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = start.plusYears(1).minusNanos(1);
        log.debug("Paginate published posts between {} and {}", start, end);
        return postRepository.findPublishedPostsBetween(start, end, pageable);
    }

    // ==================== 状态管理功能 ====================

    /**
     * 发布博客
     */
    @Transactional
    public Post publishPost(Long id) {
        log.info("发布博客 ID: {}", id);

        Post post = findById(id);
        post.setStatus(Post.PostStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());

        log.info("博客 [{}] 已发布", post.getTitle());
        return postRepository.save(post);
    }

    /**
     * 撤销发布 - 转为草稿
     */
    @Transactional
    public Post unpublishPost(Long id) {
        log.info("撤销发布博客 ID: {}", id);

        Post post = findById(id);
        post.setStatus(Post.PostStatus.DRAFT);
        post.setPublishedAt(null);

        log.info("博客 [{}] 已撤销发布", post.getTitle());
        return postRepository.save(post);
    }

    // ==================== 统计功能 ====================

    /**
     * 统计总博客数量
     */
    @Transactional(readOnly = true)
    public long countAllPosts() {
        log.debug("统计博客总数");
        return postRepository.count();
    }

    /**
     * 统计已发布博客数量
     */
    @Transactional(readOnly = true)
    public long countPublishedPosts() {
        log.debug("统计已发布博客数量");
        return postRepository.countByStatus(Post.PostStatus.PUBLISHED);
    }

    /**
     * 统计草稿博客数量
     */
    @Transactional(readOnly = true)
    public long countDraftPosts() {
        log.debug("搜索状态为 DRAFT 的博客");
        return postRepository.countByStatus(Post.PostStatus.DRAFT);
    }

    /**
     * 统计指定作者的博客数量
     */
    @Transactional(readOnly = true)
    public long countPostsByAuthor(Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + authorId));

        log.debug("查询作者 {} 的博客数量", author.getUsername());
        return postRepository.countByAuthor(author);
    }

    /**
     * 获取热门博客
     */
    public List<Post> findPopularPosts(int limit) {
        log.debug("获取热门博客，限制: {} 篇", limit);
        // 使用分页从仓库层获取热门博客
        return postRepository.findPopularPosts(org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)));
    }

    /**
     * 增加博客浏览量
     */
    @Transactional
    public void incrementViewCount(Long id) {
        log.debug("增加博客 ID: {}", id);
        Post post = findById(id);
        post.incrementViewCount();
        postRepository.save(post);
    }

    // ==================== 创建和更新博客 ====================

    /**
     * 创建新博客
     */
    @Transactional
    public Post createPost(Post post, Long authorId) {
        log.info("创建新博客，作者ID: {}", authorId);

        // 确保阅读量从0开始
        post.setViewCount(0);

        // 设置作者
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + authorId));
        post.setAuthor(author);

        // 设置默认状态
        post.setStatus(post.getStatus() != null ? post.getStatus() : Post.PostStatus.DRAFT);

        // 处理标签
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            post.setTags(processTags(post.getTags()));
        }

        // 自动生成slug
        if (!StringUtils.hasText(post.getSlug()) && StringUtils.hasText(post.getTitle())) {
            post.setSlug(generateSlug(post.getTitle()));
        }

        Post savedPost = postRepository.save(post);
        log.info("已创建博客 ID: {}", savedPost.getId());

        return savedPost;
    }

    /**
     * 更新博客
     */
    @Transactional
    public Post updatePost(Long id, Post updatedPost) {
        log.info("更新博客 ID: {}", id);

        // 验证博客存在
        Post existingPost = findById(id);

        // 更新基本信息
        if (StringUtils.hasText(updatedPost.getTitle())) {
            existingPost.setTitle(updatedPost.getTitle());
        }
        if (StringUtils.hasText(updatedPost.getContent())) {
            existingPost.setContent(updatedPost.getContent());
        }

        // 更新摘要
        if (updatedPost.getExcerpt() != null) {
            existingPost.setExcerpt(updatedPost.getExcerpt());
        }

        // 更新状态
        if (updatedPost.getStatus() != null) {
            existingPost.setStatus(updatedPost.getStatus());
        }

        // 更新特色标记
        if (updatedPost.getIsFeatured() != null) {
            existingPost.setIsFeatured(updatedPost.getIsFeatured());
        }

        // 更新分类
        if (updatedPost.getCategory() != null) {
            existingPost.setCategory(updatedPost.getCategory());
        }

        // 更新slug
        if (StringUtils.hasText(updatedPost.getSlug())) {
            existingPost.setSlug(updatedPost.getSlug());
        } else if (!StringUtils.hasText(existingPost.getSlug()) && StringUtils.hasText(updatedPost.getTitle())) {
            existingPost.setSlug(generateSlug(updatedPost.getTitle()));
        }

        // 处理标签
        if (updatedPost.getTags() != null) {
            existingPost.setTags(processTags(updatedPost.getTags()));
        }

        return postRepository.save(existingPost);
    }

    /**
     * 删除博客
     */
    @Transactional
    public void deletePost(Long id) {
        log.info("删除博客 ID: {}", id);

        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("Post not found with id: " + id);
        }

        postRepository.deleteById(id);
        log.info("博客 ID: {} 已删除", id);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理标签 - 确保标签存在
     */
    private List<Tag> processTags(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> processedTags = new ArrayList<>();

        for (Tag tag : tags) {
            if (StringUtils.hasText(tag.getName())) {
                Tag existingTag = tagRepository.findByName(tag.getName())
                        .orElseGet(() -> {
                            // 标签不存在，创建新标签并生成slug
                            Tag newTag = Tag.builder()
                                    .name(tag.getName())
                                    .build();
                            newTag.generateSlug();
                            return tagRepository.save(newTag);
                        });

                processedTags.add(existingTag);
            }
        }

        return processedTags;
    }

    /**
     * 生成博客slug
     */
    private String generateSlug(String title) {
        if (!StringUtils.hasText(title)) {
            return "untitled-post";
        }

        // 将中文title转换为URL友好的slug
        String sanitized = title.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");

        return sanitized;
    }

    // ==================== 书签绑定需求特有功能 ====================

    /**
     * 设置文章标签（替换当前所有标签）
     * @param postId 文章ID
     * @param tagIds 标签ID列表
     * @return 更新后的文章
     */
    @Transactional
    public Post setPostTags(Long postId, List<Long> tagIds) {
        log.debug("Setting tags for post {}: {}", postId, tagIds);

        Post post = findById(postId);

        // 清除现有标签关联
        List<Tag> existingTags = new ArrayList<>(post.getTags());
        for (Tag tag : existingTags) {
            post.getTags().remove(tag);
            tag.removePost(post);
        }

        // 添加新的标签关联
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

                if (!post.getTags().contains(tag)) {
                    post.getTags().add(tag);
                    tag.addPost(post);
                }
            }
        }

        // 保存更新
        postRepository.save(post);

        // 保存所有相关标签
        for (Tag tag : post.getTags()) {
            tagRepository.save(tag);
        }

        log.info("Successfully set {} tags for post {}", post.getTags().size(), postId);
        return post;
    }

    /**
     * 添加单个标签到文章
     * @param postId 文章ID
     * @param tagId 标签ID
     * @return 更新后的文章
     */
    @Transactional
    public Post addTagToPost(Long postId, Long tagId) {
        log.debug("Adding tag {} to post {}", tagId, postId);

        Post post = findById(postId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        if (!post.getTags().contains(tag)) {
            post.getTags().add(tag);
            tag.addPost(post);
            postRepository.save(post);
            tagRepository.save(tag);
            log.info("Successfully added tag {} to post {}", tagId, postId);
        } else {
            log.debug("Tag {} is already associated with post {}", tagId, postId);
        }

        return post;
    }

    /**
     * 从文章移除单个标签
     * @param postId 文章ID
     * @param tagId 标签ID
     * @return 更新后的文章
     */
    @Transactional
    public Post removeTagFromPost(Long postId, Long tagId) {
        log.debug("Removing tag {} from post {}", tagId, postId);

        Post post = findById(postId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        if (post.getTags().contains(tag)) {
            post.getTags().remove(tag);
            tag.removePost(post);
            postRepository.save(post);
            tagRepository.save(tag);
            log.info("Successfully removed tag {} from post {}", tagId, postId);
        } else {
            log.debug("Tag {} is not associated with post {}", tagId, postId);
        }

        return post;
    }
}
