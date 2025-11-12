package wiki.kana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wiki.kana.entity.Tag;

import java.util.List;
import java.util.Optional;

/**
 * 标签数据访问层
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 根据Slug查找标签
     */
    Optional<Tag> findBySlug(String slug);

    /**
     * 根据名称查找标签
     */
    Optional<Tag> findByName(String name);

    /**
     * 查找热门标签（按使用次数）
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > 0 ORDER BY t.usageCount DESC")
    List<Tag> findPopularTags(org.springframework.data.domain.Pageable pageable);

    /**
     * 查找所有热门标签
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > 0 ORDER BY t.usageCount DESC")
    List<Tag> findPopularTags();

    /**
     * 查找已使用的标签
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > 0 ORDER BY t.name ASC")
    List<Tag> findUsedTags();

    /**
     * 搜索标签
     */
    @Query("SELECT t FROM Tag t WHERE t.name LIKE %:keyword% ORDER BY t.name ASC")
    List<Tag> searchByName(@Param("keyword") String keyword);

    /**
     * 按使用次数范围查找标签
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount BETWEEN :min AND :max ORDER BY t.usageCount DESC")
    List<Tag> findByUsageCountBetween(@Param("min") Integer min, @Param("max") Integer max);

    /**
     * 查找使用次数最多的标签
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount = (SELECT MAX(t2.usageCount) FROM Tag t2)")
    List<Tag> findMostUsedTag();

    /**
     * 统计已使用标签数量
     */
    @Query("SELECT COUNT(t) FROM Tag t WHERE t.usageCount > 0")
    long countUsedTags();

    /**
     * 查找指定博客的标签
     */
    @Query("SELECT t FROM Tag t JOIN t.posts p WHERE p.id = :postId ORDER BY t.name ASC")
    List<Tag> findByPostId(@Param("postId") Long postId);

    /**
     * 查找相关标签（共同使用的标签）
     */
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.posts p WHERE p IN (SELECT p2 FROM Tag t2 JOIN t2.posts p2 WHERE t2.id = :tagId) AND t.id != :tagId ORDER BY t.usageCount DESC")
    List<Tag> findRelatedTags(@Param("tagId") Long tagId, org.springframework.data.domain.Pageable pageable);

    /**
     * 查找使用次数大于指定值的标签
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount > :count ORDER BY t.usageCount DESC")
    List<Tag> findByUsageCountGreaterThan(@Param("count") Integer count);

    /**
     * 查找按名称排序的标签
     */
    List<Tag> findAllByOrderByNameAsc();
}
