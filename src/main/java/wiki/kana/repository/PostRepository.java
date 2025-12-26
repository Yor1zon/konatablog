package wiki.kana.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wiki.kana.entity.Category;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;
import wiki.kana.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 博客文章数据访问层
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 根据Slug查找博客
     */
    Optional<Post> findBySlug(String slug);

    /**
     * 根据状态查找博客
     */
    List<Post> findByStatus(Post.PostStatus status);

    /**
     * 查找已发布的博客（按发布时间倒序）
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    List<Post> findPublishedPosts();

    /**
     * 查找已发布的博客（分页）
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    List<Post> findPublishedPosts(org.springframework.data.domain.Pageable pageable);

    /**
     * 根据作者查找博客
     */
    List<Post> findByAuthor(User author);

    /**
     * 根据作者分页查找博客
     */
    Page<Post> findByAuthor(User author, Pageable pageable);

    /**
     * 根据分类查找博客
     */
    List<Post> findByCategory(wiki.kana.entity.Category category);

    /**
     * 根据分类分页查找博客
     */
    Page<Post> findByCategory(Category category, Pageable pageable);

    /**
     * 根据标签查找博客
     */
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t = :tag AND p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    List<Post> findByTag(@Param("tag") Tag tag);

    /**
     * 根据标签查找已发布博客（分页）
     */
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t = :tag AND p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    Page<Post> findPublishedByTag(@Param("tag") Tag tag, Pageable pageable);

    /**
     * 根据标题搜索博客
     */
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% AND p.status = 'PUBLISHED'")
    List<Post> searchByTitle(@Param("keyword") String keyword);

    /**
     * 根据内容搜索博客
     */
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword% AND p.status = 'PUBLISHED'")
    List<Post> searchByContent(@Param("keyword") String keyword);

    /**
     * 查找热门博客（按浏览量）
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.viewCount DESC")
    List<Post> findPopularPosts(org.springframework.data.domain.Pageable pageable);

    /**
     * 查找特色博客
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' AND p.isFeatured = true ORDER BY p.publishedAt DESC")
    List<Post> findFeaturedPosts(org.springframework.data.domain.Pageable pageable);

    /**
     * 查找最近的博客
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    List<Post> findRecentPosts(org.springframework.data.domain.Pageable pageable);

    /**
     * 查找草稿博客
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'DRAFT' ORDER BY p.updatedAt DESC")
    List<Post> findDraftPosts();

    /**
     * 统计已发布博客数量
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.status = 'PUBLISHED'")
    long countPublishedPosts();

    /**
     * 统计指定状态博客数量
     */
    long countByStatus(Post.PostStatus status);

    /**
     * 统计指定作者的博客数量
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.author = :author AND p.status = 'PUBLISHED'")
    long countPublishedPostsByAuthor(@Param("author") User author);

    /**
     * 统计作者的全部博客数量
     */
    long countByAuthor(User author);

    /**
     * 查找指定时间范围内的博客
     */
    @Query("SELECT p FROM Post p WHERE p.publishedAt BETWEEN :start AND :end AND p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    List<Post> findPublishedPostsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查找指定时间范围内的已发布博客（分页）
     */
    @Query("SELECT p FROM Post p WHERE p.publishedAt BETWEEN :start AND :end AND p.status = 'PUBLISHED' ORDER BY p.publishedAt DESC")
    Page<Post> findPublishedPostsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    /**
     * 查找相关博客（按标签和分类）
     */
    @Query("SELECT p FROM Post p WHERE p.category = :category AND p.status = 'PUBLISHED' AND p.id != :postId ORDER BY p.publishedAt DESC")
    List<Post> findRelatedPosts(@Param("category") wiki.kana.entity.Category category, @Param("postId") Long postId, org.springframework.data.domain.Pageable pageable);
}
