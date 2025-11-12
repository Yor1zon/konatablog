package wiki.kana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wiki.kana.entity.Themes;

import java.util.List;
import java.util.Optional;

/**
 * 主题数据访问层
 */
@Repository
public interface ThemesRepository extends JpaRepository<Themes, Long> {

    /**
     * 根据Slug查找主题
     */
    Optional<Themes> findBySlug(String slug);

    /**
     * 根据名称查找主题
     */
    Optional<Themes> findByName(String name);

    /**
     * 查找激活的主题
     */
    @Query("SELECT t FROM Themes t WHERE t.isActive = true")
    List<Themes> findActiveThemes();

    /**
     * 查找默认主题
     */
    @Query("SELECT t FROM Themes t WHERE t.isDefault = true")
    Optional<Themes> findDefaultTheme();

    /**
     * 查找活动主题（只有一个）
     */
    @Query("SELECT t FROM Themes t WHERE t.isActive = true")
    Optional<Themes> findCurrentActiveTheme();

    /**
     * 查找所有主题（按是否激活排序）
     */
    @Query("SELECT t FROM Themes t ORDER BY t.isActive DESC, t.name ASC")
    List<Themes> findAllOrderByActive();

    /**
     * 查找非活动主题
     */
    @Query("SELECT t FROM Themes t WHERE t.isActive = false ORDER BY t.name ASC")
    List<Themes> findInactiveThemes();

    /**
     * 统计主题数量
     */
    @Query("SELECT COUNT(t) FROM Themes t")
    long countAllThemes();

    /**
     * 统计激活主题数量
     */
    @Query("SELECT COUNT(t) FROM Themes t WHERE t.isActive = true")
    long countActiveThemes();

    /**
     * 查找包含指定配置的主题
     */
    @Query("SELECT t FROM Themes t WHERE t.themeSettings LIKE %:key%")
    List<Themes> findThemesWithSetting(@Param("key") String key);

    /**
     * 查找按版本排序的主题
     */
    @Query("SELECT t FROM Themes t ORDER BY t.version DESC")
    List<Themes> findAllOrderByVersion();

    /**
     * 查找指定作者的主题
     */
    List<Themes> findByAuthorOrderByNameAsc(String author);

    /**
     * 检查主题是否激活
     */
    @Query("SELECT COUNT(t) > 0 FROM Themes t WHERE t.isActive = true")
    boolean hasActiveTheme();

    /**
     * 查找主题名称和版本
     */
    @Query("SELECT t FROM Themes t WHERE t.name = :name AND t.version = :version")
    Optional<Themes> findByNameAndVersion(@Param("name") String name, @Param("version") String version);
}
