package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.PostRepository;
import wiki.kana.repository.TagRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 标签服务层
 * 负责标签的业务逻辑处理，包括创建、管理、统计和推荐
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final PostRepository postRepository;

    // ==================== 基础查询 ====================

    /**
     * 根据ID查找标签
     *
     * @param id 标签ID
     * @return 标签实体
     * @throws ResourceNotFoundException 标签不存在
     */
    @Transactional(readOnly = true)
    public Tag findById(Long id) {
        log.debug("Finding tag by ID: {}", id);
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
    }

    /**
     * 根据Slug查找标签
     *
     * @param slug 标签slug
     * @return 标签实体
     * @throws ResourceNotFoundException 标签不存在
     */
    @Transactional(readOnly = true)
    public Tag findBySlug(String slug) {
        log.debug("Finding tag by slug: {}", slug);
        return tagRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with slug: " + slug));
    }

    /**
     * 根据名称查找标签
     *
     * @param name 标签名称
     * @return 标签实体
     * @throws ResourceNotFoundException 标签不存在
     */
    @Transactional(readOnly = true)
    public Tag findByName(String name) {
        log.debug("Finding tag by name: {}", name);
        return tagRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + name));
    }

    /**
     * 查询所有标签
     *
     * @return 标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        log.debug("Finding all tags");
        return tagRepository.findAll();
    }

    /**
     * 按名称排序查询所有标签
     *
     * @return 按名称排序的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> findAllOrderByName() {
        log.debug("Finding all tags ordered by name");
        return tagRepository.findAllByOrderByNameAsc();
    }

    // ==================== 创建和更新 ====================

    /**
     * 创建新标签
     *
     * @param tag 标签实体
     * @return 创建的标签
     */
    public Tag createTag(Tag tag) {
        log.info("Creating new tag with name: {}", tag != null ? tag.getName() : null);

        validateTagInput(tag);

        // 检查名称是否重复
        tagRepository.findByName(tag.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException("Tag name already exists: " + tag.getName());
        });

        // slug为空时自动生成
        if (!StringUtils.hasText(tag.getSlug())) {
            tag.setSlug(generateSlug(tag.getName()));
        }

        // 检查slug是否重复
        tagRepository.findBySlug(tag.getSlug()).ifPresent(existing -> {
            throw new DuplicateResourceException("Tag slug already exists: " + tag.getSlug());
        });

        // 设置默认值
        if (tag.getUsageCount() == null) {
            tag.setUsageCount(0);
        }

        Tag saved = tagRepository.save(tag);
        log.info("Successfully created tag with ID: {}", saved.getId());
        return saved;
    }

    /**
     * 更新标签
     *
     * @param id         标签ID
     * @param tagData    更新数据
     * @return 更新后的标签
     */
    public Tag updateTag(Long id, Tag tagData) {
        log.info("Updating tag with ID: {}", id);

        Tag existing = findById(id);

        // 名称更新及重复检查
        if (StringUtils.hasText(tagData.getName())
                && !tagData.getName().equals(existing.getName())
                && tagRepository.findByName(tagData.getName()).isPresent()) {
            throw new DuplicateResourceException("Tag name already exists: " + tagData.getName());
        }

        // slug 更新及重复检查
        if (StringUtils.hasText(tagData.getSlug())
                && !tagData.getSlug().equals(existing.getSlug())
                && tagRepository.findBySlug(tagData.getSlug()).isPresent()) {
            throw new DuplicateResourceException("Tag slug already exists: " + tagData.getSlug());
        }

        // 更新允许的字段（不更新使用计数，由关联关系自动管理）
        if (StringUtils.hasText(tagData.getName())) {
            existing.setName(tagData.getName());
        }
        if (StringUtils.hasText(tagData.getDescription())) {
            existing.setDescription(tagData.getDescription());
        }
        if (StringUtils.hasText(tagData.getSlug())) {
            existing.setSlug(tagData.getSlug());
        }
        if (StringUtils.hasText(tagData.getColor())) {
            existing.setColor(tagData.getColor());
        }

        Tag updated = tagRepository.save(existing);
        log.info("Successfully updated tag with ID: {}", updated.getId());
        return updated;
    }

    /**
     * 删除标签（要求没有关联的博客）
     *
     * @param id 标签ID
     */
    public void deleteTag(Long id) {
        log.info("Deleting tag with ID: {}", id);
        Tag tag = findById(id);

        if (tag.getUsageCount() > 0) {
            throw new IllegalStateException("Cannot delete tag that is used by posts (usage count: "
                    + tag.getUsageCount() + ")");
        }

        tagRepository.delete(tag);
        log.info("Successfully deleted tag with ID: {}", id);
    }

    /**
     * 强制删除标签（同时移除所有博客关联）
     *
     * @param id 标签ID
     */
    public void forceDeleteTag(Long id) {
        log.info("Force deleting tag with ID: {}", id);
        Tag tag = findById(id);

        // 获取所有关联的博客并移除标签关联
        List<Post> associatedPosts = tag.getPosts();
        if (associatedPosts != null) {
            for (Post post : associatedPosts) {
                post.getTags().remove(tag);
                postRepository.save(post);
            }
        }

        tagRepository.delete(tag);
        log.info("Successfully force deleted tag with ID: {}", id);
    }

    // ==================== 使用计数管理 ====================

    /**
     * 增加标签使用计数
     *
     * @param id 标签ID
     */
    public void incrementUsageCount(Long id) {
        log.debug("Incrementing usage count for tag ID: {}", id);
        Tag tag = findById(id);
        tag.incrementUsage();
        tagRepository.save(tag);
    }

    /**
     * 减少标签使用计数
     *
     * @param id 标签ID
     */
    public void decrementUsageCount(Long id) {
        log.debug("Decrementing usage count for tag ID: {}", id);
        Tag tag = findById(id);
        tag.decrementUsage();
        tagRepository.save(tag);
    }

    /**
     * 重新计算标签使用计数（用于数据修复）
     *
     * @param id 标签ID
     */
    public void recalculateUsageCount(Long id) {
        log.info("Recalculating usage count for tag ID: {}", id);
        Tag tag = findById(id);
        int actualCount = tag.getPostCount();
        tag.setUsageCount(actualCount);
        tagRepository.save(tag);
        log.info("Updated usage count for tag {}: {} -> {}", tag.getName(), tag.getUsageCount(), actualCount);
    }

    // ==================== 搜索和发现 ====================

    /**
     * 搜索标签（按名称）
     *
     * @param keyword 关键词
     * @return 匹配的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> searchByName(String keyword) {
        log.debug("Searching tags by keyword: {}", keyword);
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return tagRepository.searchByName(keyword);
    }

    /**
     * 获取热门标签
     *
     * @param limit 限制数量
     * @return 热门标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getPopularTags(int limit) {
        log.debug("Getting popular tags, limit: {}", limit);
        if (limit <= 0) {
            limit = 10;
        }
        return tagRepository.findPopularTags(Pageable.ofSize(limit));
    }

    /**
     * 获取所有热门标签（不分页）
     *
     * @return 热门标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getAllPopularTags() {
        log.debug("Getting all popular tags");
        return tagRepository.findPopularTags();
    }

    /**
     * 获取已使用的标签
     *
     * @return 已使用的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getUsedTags() {
        log.debug("Getting used tags");
        return tagRepository.findUsedTags();
    }

    /**
     * 获取按使用次数范围筛选的标签
     *
     * @param min 最小使用次数
     * @param max 最大使用次数
     * @return 标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getTagsByUsageCountRange(Integer min, Integer max) {
        log.debug("Getting tags by usage count range: {} - {}", min, max);
        if (min == null) min = 0;
        if (max == null) max = Integer.MAX_VALUE;
        return tagRepository.findByUsageCountBetween(min, max);
    }

    /**
     * 获取使用次数最多的标签
     *
     * @return 使用次数最多的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getMostUsedTags() {
        log.debug("Getting most used tags");
        return tagRepository.findMostUsedTag();
    }

    /**
     * 获取使用次数大于指定值的标签
     *
     * @param count 使用次数阈值
     * @return 标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getTagsWithUsageCountGreaterThan(Integer count) {
        log.debug("Getting tags with usage count greater than: {}", count);
        if (count == null) count = 0;
        return tagRepository.findByUsageCountGreaterThan(count);
    }

    // ==================== 博客关联和推荐 ====================

    /**
     * 获取指定博客的标签
     *
     * @param postId 博客ID
     * @return 标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getTagsByPostId(Long postId) {
        log.debug("Getting tags for post ID: {}", postId);
        return tagRepository.findByPostId(postId);
    }

    /**
     * 获取相关标签（共同使用的标签）
     *
     * @param tagId 标签ID
     * @param limit 限制数量
     * @return 相关标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getRelatedTags(Long tagId, int limit) {
        log.debug("Getting related tags for tag ID: {}, limit: {}", tagId, limit);
        if (limit <= 0) {
            limit = 5;
        }
        return tagRepository.findRelatedTags(tagId, Pageable.ofSize(limit));
    }

    /**
     * 获取标签推荐（基于热门程度和使用模式）
     *
     * @param limit 限制数量
     * @return 推荐标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getRecommendedTags(int limit) {
        log.debug("Getting recommended tags, limit: {}", limit);
        if (limit <= 0) {
            limit = 10;
        }

        // 综合推荐：热门标签 + 最近使用的标签
        List<Tag> popularTags = getPopularTags(limit / 2);
        List<Tag> recentUsedTags = getUsedTags().stream()
                .sorted((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
                .limit(limit / 2)
                .collect(Collectors.toList());

        List<Tag> recommended = new ArrayList<>();
        recommended.addAll(popularTags);
        recommended.addAll(recentUsedTags);

        // 去重并返回指定数量
        return recommended.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== 统计功能 ====================

    /**
     * 统计所有标签数量
     *
     * @return 标签总数
     */
    @Transactional(readOnly = true)
    public long countAllTags() {
        log.debug("Counting all tags");
        return tagRepository.count();
    }

    /**
     * 统计已使用标签数量
     *
     * @return 已使用标签总数
     */
    @Transactional(readOnly = true)
    public long countUsedTags() {
        log.debug("Counting used tags");
        return tagRepository.countUsedTags();
    }

    /**
     * 统计未使用标签数量
     *
     * @return 未使用标签总数
     */
    @Transactional(readOnly = true)
    public long countUnusedTags() {
        long total = countAllTags();
        long used = countUsedTags();
        return total - used;
    }

    /**
     * 获取标签使用统计信息
     *
     * @return 统计信息Map
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getTagStatistics() {
        log.debug("Getting tag statistics");
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalTags", countAllTags());
        stats.put("usedTags", countUsedTags());
        stats.put("unusedTags", countUnusedTags());
        return stats;
    }

    // ==================== 批量操作 ====================

    /**
     * 批量创建标签（如果不存在）
     *
     * @param tagNames 标签名称列表
     * @return 创建或找到的标签列表
     */
    public List<Tag> getOrCreateTags(List<String> tagNames) {
        log.debug("Getting or creating tags for names: {}", tagNames);
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tag> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            if (StringUtils.hasText(tagName.trim())) {
                Tag tag = tagRepository.findByName(tagName.trim())
                        .orElseGet(() -> createTagFromName(tagName.trim()));
                tags.add(tag);
            }
        }

        return tags;
    }

    /**
     * 重新计算所有标签的使用计数
     */
    public void recalculateAllUsageCounts() {
        log.info("Recalculating all tag usage counts");
        List<Tag> allTags = findAll();
        int updatedCount = 0;

        for (Tag tag : allTags) {
            try {
                recalculateUsageCount(tag.getId());
                updatedCount++;
            } catch (Exception e) {
                log.warn("Failed to recalculate usage count for tag {}: {}", tag.getName(), e.getMessage());
            }
        }

        log.info("Recalculated usage counts for {} tags", updatedCount);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证标签输入
     *
     * @param tag 标签实体
     */
    private void validateTagInput(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (!StringUtils.hasText(tag.getName())) {
            throw new IllegalArgumentException("Tag name is required");
        }
        if (tag.getName().length() > 100) {
            throw new IllegalArgumentException("Tag name must not exceed 100 characters");
        }
    }

    /**
     * 从名称创建标签（内部方法）
     *
     * @param name 标签名称
     * @return 创建的标签
     */
    private Tag createTagFromName(String name) {
        Tag tag = Tag.builder()
                .name(name.trim())
                .usageCount(0)
                .build();
        tag.generateSlug(); // 自动生成slug
        return tagRepository.save(tag);
    }

    /**
     * 从名称生成URL友好的slug
     *
     * @param name 标签名称
     * @return slug
     */
    private String generateSlug(String name) {
        if (!StringUtils.hasText(name)) {
            return "tag";
        }
        String sanitized = name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
        return sanitized;
    }
}