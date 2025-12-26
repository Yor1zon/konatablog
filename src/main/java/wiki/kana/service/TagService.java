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

    // ==================== 标签管理和验证 ====================

    /**
     * 验证标签是否可以被删除
     *
     * @param id 标签ID
     * @return 验证结果信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateTagDeletion(Long id) {
        log.debug("Validating deletion for tag ID: {}", id);
        Tag tag = findById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("canDelete", tag.getUsageCount() == 0);
        result.put("usageCount", tag.getUsageCount());
        result.put("tagName", tag.getName());

        if (tag.getUsageCount() > 0) {
            result.put("message", String.format("标签 '%s' 被 %d 篇博客使用，无法删除",
                    tag.getName(), tag.getUsageCount()));
            result.put("forceDeleteOption", true);
        } else {
            result.put("message", "标签未被使用，可以安全删除");
        }

        return result;
    }

    /**
     * 清理未使用的标签
     *
     * @return 清理的标签数量
     */
    public int cleanupUnusedTags() {
        log.info("Starting cleanup of unused tags");
        List<Tag> unusedTags = tagRepository.findAll().stream()
                .filter(tag -> tag.getUsageCount() == 0)
                .collect(Collectors.toList());

        int deletedCount = 0;
        for (Tag tag : unusedTags) {
            try {
                tagRepository.delete(tag);
                deletedCount++;
                log.debug("Deleted unused tag: {}", tag.getName());
            } catch (Exception e) {
                log.warn("Failed to delete unused tag {}: {}", tag.getName(), e.getMessage());
            }
        }

        log.info("Cleaned up {} unused tags", deletedCount);
        return deletedCount;
    }

    /**
     * 检查标签名称的有效性
     *
     * @param name 标签名称
     * @return 验证结果
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateTagName(String name) {
        log.debug("Validating tag name: {}", name);

        Map<String, Object> result = new HashMap<>();

        if (!StringUtils.hasText(name)) {
            result.put("valid", false);
            result.put("error", "标签名称不能为空");
            return result;
        }

        if (name.length() > 100) {
            result.put("valid", false);
            result.put("error", "标签名称不能超过100个字符");
            return result;
        }

        // 检查是否包含特殊字符
        if (!name.matches("^[\\w\\u4e00-\\u9fa5\\s\\-]+$")) {
            result.put("valid", false);
            result.put("error", "标签名称只能包含字母、数字、中文、空格和连字符");
            return result;
        }

        // 检查是否已存在
        Optional<Tag> existing = tagRepository.findByName(name.trim());
        if (existing.isPresent()) {
            result.put("valid", false);
            result.put("error", "标签名称已存在");
            result.put("existingTag", existing.get());
            return result;
        }

        result.put("valid", true);
        result.put("suggestedSlug", generateSlug(name));

        return result;
    }

    // ==================== 高级搜索和过滤 ====================

    /**
     * 高级标签搜索
     *
     * @param name        名称关键词（可选）
     * @param description 描述关键词（可选）
     * @param minUsage    最小使用次数（可选）
     * @param maxUsage    最大使用次数（可选）
     * @param hasColor    是否有颜色（可选）
     * @param sortBy      排序字段：name, usage, created, updated
     * @param ascending   是否升序
     * @return 匹配的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> advancedSearch(String name, String description,
                                   Integer minUsage, Integer maxUsage,
                                   Boolean hasColor, String sortBy, Boolean ascending) {
        log.debug("Advanced search: name={}, description={}, minUsage={}, maxUsage={}, hasColor={}, sortBy={}",
                name, description, minUsage, maxUsage, hasColor, sortBy);

        // 获取所有标签并进行内存过滤（由于SQLite限制）
        List<Tag> allTags = tagRepository.findAll();

        List<Tag> filtered = allTags.stream()
                .filter(tag -> {
                    if (StringUtils.hasText(name) &&
                        !tag.getName().toLowerCase().contains(name.toLowerCase())) {
                        return false;
                    }
                    if (StringUtils.hasText(description) &&
                        (tag.getDescription() == null ||
                         !tag.getDescription().toLowerCase().contains(description.toLowerCase()))) {
                        return false;
                    }
                    if (minUsage != null && tag.getUsageCount() < minUsage) {
                        return false;
                    }
                    if (maxUsage != null && tag.getUsageCount() > maxUsage) {
                        return false;
                    }
                    if (hasColor != null) {
                        if (hasColor && !StringUtils.hasText(tag.getColor())) {
                            return false;
                        }
                        if (!hasColor && StringUtils.hasText(tag.getColor())) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((t1, t2) -> {
                    int compare = 0;
                    if ("name".equals(sortBy)) {
                        compare = t1.getName().compareToIgnoreCase(t2.getName());
                    } else if ("usage".equals(sortBy)) {
                        compare = Integer.compare(t1.getUsageCount(), t2.getUsageCount());
                    } else if ("created".equals(sortBy)) {
                        compare = t1.getCreatedAt().compareTo(t2.getCreatedAt());
                    } else if ("updated".equals(sortBy)) {
                        compare = t1.getUpdatedAt().compareTo(t2.getUpdatedAt());
                    } else {
                        // 默认按名称排序
                        compare = t1.getName().compareToIgnoreCase(t2.getName());
                    }
                    return ascending != null && ascending ? compare : -compare;
                })
                .collect(Collectors.toList());

        return filtered;
    }

    /**
     * 获取标签云数据（用于标签云展示）
     *
     * @param maxTags 最大标签数量
     * @return 标签云数据
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTagCloudData(int maxTags) {
        log.debug("Getting tag cloud data, maxTags: {}", maxTags);

        List<Tag> usedTags = getUsedTags().stream()
                .filter(tag -> tag.getUsageCount() > 0)
                .sorted((t1, t2) -> Integer.compare(t2.getUsageCount(), t1.getUsageCount()))
                .limit(maxTags > 0 ? maxTags : 50)
                .collect(Collectors.toList());

        if (usedTags.isEmpty()) {
            return Collections.emptyList();
        }

        int maxCount = usedTags.get(0).getUsageCount();
        int minCount = usedTags.get(usedTags.size() - 1).getUsageCount();

        List<Map<String, Object>> cloudData = new ArrayList<>();
        for (Tag tag : usedTags) {
            Map<String, Object> tagData = new HashMap<>();
            tagData.put("id", tag.getId());
            tagData.put("name", tag.getName());
            tagData.put("slug", tag.getSlug());
            tagData.put("count", tag.getUsageCount());
            tagData.put("color", tag.getColor());

            // 计算权重（1-5级别）
            int weight = 1;
            if (maxCount > minCount) {
                weight = 1 + (tag.getUsageCount() - minCount) * 4 / (maxCount - minCount);
            }
            tagData.put("weight", weight);

            cloudData.add(tagData);
        }

        return cloudData;
    }

    // ==================== 标签趋势分析 ====================

    /**
     * 获取标签使用趋势统计
     *
     * @return 趋势统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTagTrendAnalysis() {
        log.debug("Getting tag trend analysis");

        List<Tag> allTags = tagRepository.findAll();

        Map<String, Object> analysis = new HashMap<>();

        // 基本统计
        long totalTags = allTags.size();
        long usedTags = allTags.stream().mapToLong(tag -> tag.getUsageCount() > 0 ? 1 : 0).sum();
        long unusedTags = totalTags - usedTags;

        analysis.put("totalTags", totalTags);
        analysis.put("usedTags", usedTags);
        analysis.put("unusedTags", unusedTags);
        analysis.put("usageRate", totalTags > 0 ? (double) usedTags / totalTags : 0.0);

        // 使用分布
        Map<String, Long> usageDistribution = new HashMap<>();
        usageDistribution.put("unused", allTags.stream().mapToLong(tag -> tag.getUsageCount() == 0 ? 1 : 0).sum());
        usageDistribution.put("rare", allTags.stream().mapToLong(tag -> tag.getUsageCount() > 0 && tag.getUsageCount() <= 2 ? 1 : 0).sum());
        usageDistribution.put("moderate", allTags.stream().mapToLong(tag -> tag.getUsageCount() > 2 && tag.getUsageCount() <= 5 ? 1 : 0).sum());
        usageDistribution.put("popular", allTags.stream().mapToLong(tag -> tag.getUsageCount() > 5 && tag.getUsageCount() <= 10 ? 1 : 0).sum());
        usageDistribution.put("veryPopular", allTags.stream().mapToLong(tag -> tag.getUsageCount() > 10 ? 1 : 0).sum());

        analysis.put("usageDistribution", usageDistribution);

        // 热门标签
        List<Tag> topTags = allTags.stream()
                .filter(tag -> tag.getUsageCount() > 0)
                .sorted((t1, t2) -> Integer.compare(t2.getUsageCount(), t1.getUsageCount()))
                .limit(10)
                .collect(Collectors.toList());
        analysis.put("topTags", topTags);

        // 平均使用次数
        double avgUsage = allTags.stream()
                .filter(tag -> tag.getUsageCount() > 0)
                .mapToInt(Tag::getUsageCount)
                .average()
                .orElse(0.0);
        analysis.put("averageUsage", avgUsage);

        return analysis;
    }

    /**
     * 获取最近创建的标签
     *
     * @param days 天数
     * @param limit 限制数量
     * @return 最近创建的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getRecentlyCreatedTags(int days, int limit) {
        log.debug("Getting recently created tags, days: {}, limit: {}", days, limit);

        return tagRepository.findAll().stream()
                .filter(tag -> tag.getCreatedAt() != null)
                .filter(tag -> tag.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(days)))
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());
    }

    /**
     * 获取最近更新的标签
     *
     * @param days 天数
     * @param limit 限制数量
     * @return 最近更新的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> getRecentlyUpdatedTags(int days, int limit) {
        log.debug("Getting recently updated tags, days: {}, limit: {}", days, limit);

        return tagRepository.findAll().stream()
                .filter(tag -> tag.getUpdatedAt() != null)
                .filter(tag -> tag.getUpdatedAt().isAfter(java.time.LocalDateTime.now().minusDays(days)))
                .sorted((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());
    }

    // ==================== 标签合并功能 ====================

    /**
     * 合并标签（将源标签的所有关联转移到目标标签）
     *
     * @param sourceId 源标签ID
     * @param targetId 目标标签ID
     * @return 合并后的目标标签
     */
    @Transactional
    public Tag mergeTags(Long sourceId, Long targetId) {
        log.info("Merging tag {} into tag {}", sourceId, targetId);

        Tag sourceTag = findById(sourceId);
        Tag targetTag = findById(targetId);

        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge tag with itself");
        }

        // 获取源标签的所有关联博客
        List<Post> sourcePosts = new ArrayList<>(sourceTag.getPosts());

        // 将关联转移到目标标签
        for (Post post : sourcePosts) {
            if (!post.getTags().contains(targetTag)) {
                post.getTags().add(targetTag);
                targetTag.addPost(post);
            }
            post.getTags().remove(sourceTag);
            sourceTag.removePost(post);
            postRepository.save(post);
        }

        // 保存目标标签（更新使用计数）
        targetTag.setUsageCount(targetTag.getPostCount());
        tagRepository.save(targetTag);

        // 记录合并信息
        String mergeInfo = String.format("Merged from '%s' (ID: %d) on %s",
                sourceTag.getName(), sourceTag.getId(), java.time.LocalDateTime.now());
        if (StringUtils.hasText(targetTag.getDescription())) {
            targetTag.setDescription(targetTag.getDescription() + "\n\n" + mergeInfo);
        } else {
            targetTag.setDescription(mergeInfo);
        }

        tagRepository.save(targetTag);

        // 删除源标签
        tagRepository.delete(sourceTag);

        log.info("Successfully merged tag '{}' into '{}' with {} posts transferred",
                sourceTag.getName(), targetTag.getName(), sourcePosts.size());

        return targetTag;
    }

    /**
     * 批量合并标签
     *
     * @param sourceIds 源标签ID列表
     * @param targetId 目标标签ID
     * @return 合并结果信息
     */
    @Transactional
    public Map<String, Object> batchMergeTags(List<Long> sourceIds, Long targetId) {
        log.info("Batch merging {} tags into tag {}", sourceIds.size(), targetId);

        Map<String, Object> result = new HashMap<>();
        List<String> mergedTags = new ArrayList<>();
        List<String> failedTags = new ArrayList<>();
        int totalPostsTransferred = 0;

        Tag targetTag = findById(targetId);

        for (Long sourceId : sourceIds) {
            try {
                if (sourceId.equals(targetId)) {
                    failedTags.add("ID " + sourceId + " (same as target)");
                    continue;
                }

                Tag sourceTag = findById(sourceId);
                int transferredPosts = sourceTag.getUsageCount();

                Tag merged = mergeTags(sourceId, targetId);
                mergedTags.add(sourceTag.getName());
                totalPostsTransferred += transferredPosts;

            } catch (Exception e) {
                failedTags.add("ID " + sourceId + ": " + e.getMessage());
                log.warn("Failed to merge tag {}: {}", sourceId, e.getMessage());
            }
        }

        result.put("targetTag", targetTag.getName());
        result.put("mergedCount", mergedTags.size());
        result.put("totalPostsTransferred", totalPostsTransferred);
        result.put("mergedTags", mergedTags);
        result.put("failedTags", failedTags);
        result.put("success", failedTags.isEmpty());

        log.info("Batch merge completed: {} tags merged, {} failed", mergedTags.size(), failedTags.size());

        return result;
    }

    /**
     * 建议标签合并（基于相似名称）
     *
     * @param similarityThreshold 相似度阈值（0.0-1.0）
     * @return 合并建议列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggestTagMerges(double similarityThreshold) {
        log.debug("Suggesting tag merges with similarity threshold: {}", similarityThreshold);

        List<Tag> allTags = tagRepository.findAll();
        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (int i = 0; i < allTags.size(); i++) {
            for (int j = i + 1; j < allTags.size(); j++) {
                Tag tag1 = allTags.get(i);
                Tag tag2 = allTags.get(j);

                double similarity = calculateNameSimilarity(tag1.getName(), tag2.getName());
                if (similarity >= similarityThreshold) {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("tag1", Map.of("id", tag1.getId(), "name", tag1.getName(), "usageCount", tag1.getUsageCount()));
                    suggestion.put("tag2", Map.of("id", tag2.getId(), "name", tag2.getName(), "usageCount", tag2.getUsageCount()));
                    suggestion.put("similarity", similarity);
                    suggestion.put("recommendedTarget", tag1.getUsageCount() >= tag2.getUsageCount() ? tag1.getId() : tag2.getId());
                    suggestions.add(suggestion);
                }
            }
        }

        return suggestions.stream()
                .sorted((s1, s2) -> Double.compare((Double) s2.get("similarity"), (Double) s1.get("similarity")))
                .collect(Collectors.toList());
    }

    /**
     * 计算两个标签名称的相似度（简单的Levenshtein距离算法）
     *
     * @param name1 名称1
     * @param name2 名称2
     * @return 相似度（0.0-1.0）
     */
    private double calculateNameSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }

        String s1 = name1.toLowerCase().trim();
        String s2 = name2.toLowerCase().trim();

        if (s1.equals(s2)) {
            return 1.0;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 计算Levenshtein距离
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 距离
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // ==================== 书签绑定需求特有功能 ====================

    /**
     * 不区分大小写搜索标签
     * @param keyword 搜索关键词
     * @return 匹配的标签列表
     */
    @Transactional(readOnly = true)
    public List<Tag> searchByNameIgnoreCase(String keyword) {
        log.debug("Searching tags by keyword (ignore case): {}", keyword);
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return tagRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    /**
     * 智能创建标签：先查找（不区分大小写），找不到则创建
     * @param name 标签名称
     * @param description 标签描述（可选）
     * @param color 标签颜色（可选）
     * @return 现有或新创建的标签
     */
    public Tag findOrCreateByName(String name, String description, String color) {
        log.debug("Find or creating tag by name: {}", name);

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }

        // 先不区分大小写查找
        Optional<Tag> existing = tagRepository.findByNameIgnoreCase(name.trim());
        if (existing.isPresent()) {
            log.debug("Found existing tag: {}", existing.get().getName());
            return existing.get();
        }

        // 创建新标签
        Tag newTag = Tag.builder()
                .name(name.trim())
                .description(StringUtils.hasText(description) ? description.trim() : null)
                .color(StringUtils.hasText(color) ? color.trim() : null)
                .usageCount(0)
                .build();
        newTag.generateSlug();

        Tag created = tagRepository.save(newTag);
        log.info("Created new tag: {}", created.getName());
        return created;
    }

    /**
     * 添加文章到标签关联
     * @param tagId 标签ID
     * @param postId 文章ID
     */
    public void addPostToTag(Long tagId, Long postId) {
        log.debug("Adding post {} to tag {}", postId, tagId);

        Tag tag = findById(tagId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getTags().contains(tag)) {
            post.getTags().add(tag);
            tag.addPost(post);
            postRepository.save(post);
            tagRepository.save(tag);
            log.debug("Successfully added post {} to tag {}", postId, tagId);
        } else {
            log.debug("Post {} is already associated with tag {}", postId, tagId);
        }
    }

    /**
     * 从标签移除文章关联
     * @param tagId 标签ID
     * @param postId 文章ID
     */
    public void removePostFromTag(Long tagId, Long postId) {
        log.debug("Removing post {} from tag {}", postId, tagId);

        Tag tag = findById(tagId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (post.getTags().contains(tag)) {
            post.getTags().remove(tag);
            tag.removePost(post);
            postRepository.save(post);
            tagRepository.save(tag);
            log.debug("Successfully removed post {} from tag {}", postId, tagId);
        } else {
            log.debug("Post {} is not associated with tag {}", postId, tagId);
        }
    }

    /**
     * 批量添加文章到标签关联
     * @param tagId 标签ID
     * @param postIds 文章ID列表
     */
    @Transactional
    public void addPostsToTag(Long tagId, List<Long> postIds) {
        log.debug("Batch adding posts {} to tag {}", postIds, tagId);

        if (postIds == null || postIds.isEmpty()) {
            throw new IllegalArgumentException("Post ID list cannot be null or empty");
        }

        Tag tag = findById(tagId);
        int addedCount = 0;

        for (Long postId : postIds) {
            try {
                Post post = postRepository.findById(postId)
                        .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

                if (!post.getTags().contains(tag)) {
                    post.getTags().add(tag);
                    tag.addPost(post);
                    postRepository.save(post);
                    addedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to add post {} to tag {}: {}", postId, tagId, e.getMessage());
            }
        }

        if (addedCount > 0) {
            tagRepository.save(tag);
        }

        log.info("Successfully added {} posts to tag {}", addedCount, tagId);
    }

    /**
     * 批量从标签移除文章关联
     * @param tagId 标签ID
     * @param postIds 文章ID列表
     */
    @Transactional
    public void removePostsFromTag(Long tagId, List<Long> postIds) {
        log.debug("Batch removing posts {} from tag {}", postIds, tagId);

        if (postIds == null || postIds.isEmpty()) {
            throw new IllegalArgumentException("Post ID list cannot be null or empty");
        }

        Tag tag = findById(tagId);
        int removedCount = 0;

        for (Long postId : postIds) {
            try {
                Post post = postRepository.findById(postId)
                        .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

                if (post.getTags().contains(tag)) {
                    post.getTags().remove(tag);
                    tag.removePost(post);
                    postRepository.save(post);
                    removedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to remove post {} from tag {}: {}", postId, tagId, e.getMessage());
            }
        }

        if (removedCount > 0) {
            tagRepository.save(tag);
        }

        log.info("Successfully removed {} posts from tag {}", removedCount, tagId);
    }

    /**
     * 获取标签关联文章简要信息
     * @param tagId 标签ID
     * @param status 文章状态筛选（可选）
     * @return 文章简要信息列表
     */
    @Transactional(readOnly = true)
    public List<Post> getTagPostsSummary(Long tagId, String status) {
        log.debug("Getting posts summary for tag {}, status filter: {}", tagId, status);

        Tag tag = findById(tagId);
        List<Post> posts = new ArrayList<>(tag.getPosts());

        if (StringUtils.hasText(status)) {
            try {
                Post.PostStatus desiredStatus = Post.PostStatus.valueOf(status.toUpperCase());
                posts = posts.stream()
                        .filter(post -> desiredStatus.equals(post.getStatus()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
            }
        }

        return posts;
    }
}