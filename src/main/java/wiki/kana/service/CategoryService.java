package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.Category;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.CategoryRepository;

import java.util.Collections;
import java.util.List;

/**
 * 分类服务层
 * 负责博客分类的业务逻辑处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ==================== 基础查询 ====================

    /**
     * 根据ID查找分类
     *
     * @param id 分类ID
     * @return 分类实体
     * @throws ResourceNotFoundException 分类不存在
     */
    @Transactional(readOnly = true)
    public Category findById(Long id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    /**
     * 根据Slug查找分类
     *
     * @param slug 分类slug
     * @return 分类实体
     * @throws ResourceNotFoundException 分类不存在
     */
    @Transactional(readOnly = true)
    public Category findBySlug(String slug) {
        log.debug("Finding category by slug: {}", slug);
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
    }

    /**
     * 根据名称查找分类
     *
     * @param name 分类名称
     * @return 分类实体
     * @throws ResourceNotFoundException 分类不存在
     */
    @Transactional(readOnly = true)
    public Category findByName(String name) {
        log.debug("Finding category by name: {}", name);
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
    }

    /**
     * 查询全部分类
     *
     * @return 分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        log.debug("Finding all categories");
        return categoryRepository.findAll();
    }

    /**
     * 查询启用状态的分类
     *
     * @return 启用分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        log.debug("Finding active categories");
        return categoryRepository.findByIsActiveTrue();
    }

    // ==================== 层级结构查询 ====================

    /**
     * 查询顶级分类（无父分类，且启用）
     *
     * @return 顶级分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findTopLevelCategories() {
        log.debug("Finding top level categories");
        return categoryRepository.findTopLevelCategories();
    }

    /**
     * 查询所有顶级分类（包含子分类）
     *
     * @return 顶级分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findAllTopLevelCategories() {
        log.debug("Finding all top level categories (with children)");
        return categoryRepository.findAllTopLevelCategories();
    }

    /**
     * 根据父分类ID查询子分类
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findChildren(Long parentId) {
        if (parentId == null) {
            return Collections.emptyList();
        }
        log.debug("Finding children for parent category: {}", parentId);
        Category parent = findById(parentId);
        return categoryRepository.findByParent(parent);
    }

    // ==================== 创建和更新 ====================

    /**
     * 创建新分类
     *
     * @param category 分类实体
     * @param parentId 父分类ID（可为空）
     * @return 创建的分类
     */
    public Category createCategory(Category category, Long parentId) {
        log.info("Creating new category with name: {}", category != null ? category.getName() : null);

        validateCategoryInput(category);

        // 检查名称是否重复
        categoryRepository.findByName(category.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException("Category name already exists: " + category.getName());
        });

        // slug为空时自动生成
        if (!StringUtils.hasText(category.getSlug())) {
            category.setSlug(generateSlug(category.getName()));
        }

        // 检查slug是否重复
        categoryRepository.findBySlug(category.getSlug()).ifPresent(existing -> {
            throw new DuplicateResourceException("Category slug already exists: " + category.getSlug());
        });

        // 设置父分类
        if (parentId != null) {
            Category parent = findById(parentId);
            category.setParent(parent);
        }

        // 默认启用
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }

        Category saved = categoryRepository.save(category);
        log.info("Successfully created category with ID: {}", saved.getId());
        return saved;
    }

    /**
     * 更新分类
     *
     * @param id            分类ID
     * @param categoryData  更新数据
     * @param newParentId   新的父分类ID（可为空）
     * @return 更新后的分类
     */
    public Category updateCategory(Long id, Category categoryData, Long newParentId) {
        log.info("Updating category with ID: {}", id);

        Category existing = findById(id);

        // 名称更新及重复检查
        if (StringUtils.hasText(categoryData.getName())
                && !categoryData.getName().equals(existing.getName())
                && categoryRepository.findByName(categoryData.getName()).isPresent()) {
            throw new DuplicateResourceException("Category name already exists: " + categoryData.getName());
        }

        // slug 更新及重复检查
        if (StringUtils.hasText(categoryData.getSlug())
                && !categoryData.getSlug().equals(existing.getSlug())
                && categoryRepository.findBySlug(categoryData.getSlug()).isPresent()) {
            throw new DuplicateResourceException("Category slug already exists: " + categoryData.getSlug());
        }

        // 更新允许的字段
        if (StringUtils.hasText(categoryData.getName())) {
            existing.setName(categoryData.getName());
        }
        if (StringUtils.hasText(categoryData.getDescription())) {
            existing.setDescription(categoryData.getDescription());
        }
        if (StringUtils.hasText(categoryData.getSlug())) {
            existing.setSlug(categoryData.getSlug());
        }
        if (categoryData.getSortOrder() != null) {
            existing.setSortOrder(categoryData.getSortOrder());
        }
        if (categoryData.getIsActive() != null) {
            existing.setIsActive(categoryData.getIsActive());
        }

        // 更新父分类
        if (newParentId != null) {
            if (id.equals(newParentId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            Category newParent = findById(newParentId);
            existing.setParent(newParent);
        } else if (categoryData.getParent() == null) {
            // 显式设置为顶级分类
            existing.setParent(null);
        }

        Category updated = categoryRepository.save(existing);
        log.info("Successfully updated category with ID: {}", updated.getId());
        return updated;
    }

    // ==================== 状态管理 ====================

    /**
     * 启用分类
     *
     * @param id 分类ID
     * @return 启用后的分类
     */
    public Category activateCategory(Long id) {
        log.info("Activating category with ID: {}", id);
        Category category = findById(id);
        category.setIsActive(true);
        Category activated = categoryRepository.save(category);
        log.info("Successfully activated category with ID: {}", activated.getId());
        return activated;
    }

    /**
     * 禁用分类
     *
     * @param id 分类ID
     * @return 禁用后的分类
     */
    public Category deactivateCategory(Long id) {
        log.info("Deactivating category with ID: {}", id);
        Category category = findById(id);
        category.setIsActive(false);
        Category deactivated = categoryRepository.save(category);
        log.info("Successfully deactivated category with ID: {}", deactivated.getId());
        return deactivated;
    }

    // ==================== 删除和统计 ====================

    /**
     * 删除分类（要求没有子分类和关联博客）
     *
     * @param id 分类ID
     */
    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);
        Category category = findById(id);

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with child categories");
        }

        if (category.getPosts() != null && !category.getPosts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category that contains posts");
        }

        categoryRepository.delete(category);
        log.info("Successfully deleted category with ID: {}", id);
    }

    /**
     * 统计分类下已发布博客数量
     *
     * @param categoryId 分类ID
     * @return 已发布博客数量
     */
    @Transactional(readOnly = true)
    public long countPublishedPosts(Long categoryId) {
        Category category = findById(categoryId);
        return categoryRepository.countPublishedPostsByCategory(category);
    }

    /**
     * 查找有已发布博客的分类
     *
     * @return 分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findCategoriesWithPublishedPosts() {
        return categoryRepository.findCategoriesWithPublishedPosts();
    }

    /**
     * 按博客数量排序的分类
     *
     * @return 分类列表
     */
    @Transactional(readOnly = true)
    public List<Category> findCategoriesOrderByPostCount() {
        return categoryRepository.findCategoriesOrderByPostCount();
    }

    // ==================== 工具方法 ====================

    /**
     * 验证分类输入
     *
     * @param category 分类实体
     */
    private void validateCategoryInput(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (!StringUtils.hasText(category.getName())) {
            throw new IllegalArgumentException("Category name is required");
        }
    }

    /**
     * 从名称生成URL友好的slug
     *
     * @param name 分类名称
     * @return slug
     */
    private String generateSlug(String name) {
        if (!StringUtils.hasText(name)) {
            return "category";
        }
        String sanitized = name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
        return sanitized;
    }
}

