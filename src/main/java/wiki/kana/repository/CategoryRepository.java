package wiki.kana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wiki.kana.entity.Category;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问层
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 根据Slug查找分类
     */
    Optional<Category> findBySlug(String slug);

    /**
     * 根据名称查找分类
     */
    Optional<Category> findByName(String name);

    /**
     * 查找启用的分类
     */
    List<Category> findByIsActiveTrue();

    /**
     * 查找顶级分类（无父分类）
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.sortOrder ASC")
    List<Category> findTopLevelCategories();

    /**
     * 查找子分类
     */
    @Query("SELECT c FROM Category c WHERE c.parent = :parent AND c.isActive = true ORDER BY c.sortOrder ASC")
    List<Category> findByParent(@Param("parent") Category parent);

    /**
     * 查找所有顶级分类（包含子分类）
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findAllTopLevelCategories();

    /**
     * 统计分类下的博客数量
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.category = :category AND p.status = 'PUBLISHED'")
    long countPublishedPostsByCategory(@Param("category") Category category);

    /**
     * 查找有博客的分类
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.posts p WHERE p.status = 'PUBLISHED' AND c.isActive = true")
    List<Category> findCategoriesWithPublishedPosts();

    /**
     * 按博客数量排序的分类
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY SIZE(c.posts) DESC")
    List<Category> findCategoriesOrderByPostCount();

    /**
     * 查找根分类
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findRootCategories();
}
