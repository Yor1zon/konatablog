package wiki.kana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wiki.kana.entity.Settings;

import java.util.List;
import java.util.Optional;

/**
 * 系统设置数据访问层
 */
@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    /**
     * 根据配置键查找设置
     */
    Optional<Settings> findByConfigKey(String configKey);

    /**
     * 根据配置组查找设置
     */
    List<Settings> findByConfigGroup(String configGroup);

    /**
     * 查找所有配置键（按配置组排序）
     */
    @Query("SELECT s FROM Settings s ORDER BY s.configGroup ASC, s.configKey ASC")
    List<Settings> findAllGrouped();

    /**
     * 查找指定配置组的设置
     */
    @Query("SELECT s FROM Settings s WHERE s.configGroup = :group ORDER BY s.configKey ASC")
    List<Settings> findByConfigGroupOrderByConfigKey(@Param("group") String group);

    /**
     * 查找多个配置键的设置
     */
    @Query("SELECT s FROM Settings s WHERE s.configKey IN :keys")
    List<Settings> findByConfigKeyIn(@Param("keys") List<String> keys);

    /**
     * 检查配置键是否存在
     */
    @Query("SELECT COUNT(s) > 0 FROM Settings s WHERE s.configKey = :key")
    boolean existsByConfigKey(@Param("key") String configKey);

    /**
     * 根据配置类型查找设置
     */
    List<Settings> findByOptionType(Settings.OptionType optionType);

    /**
     * 获取所有配置组
     */
    @Query("SELECT DISTINCT s.configGroup FROM Settings s ORDER BY s.configGroup ASC")
    List<String> findAllConfigGroups();

    /**
     * 统计指定配置组的设置数量
     */
    @Query("SELECT COUNT(s) FROM Settings s WHERE s.configGroup = :group")
    long countByConfigGroup(@Param("group") String group);

    /**
     * 查找常用配置
     */
    @Query("SELECT s FROM Settings s WHERE s.configGroup IN ('site', 'seo', 'social') ORDER BY s.configGroup ASC, s.configKey ASC")
    List<Settings> findCommonSettings();

    /**
     * 查找主题相关设置
     */
    @Query("SELECT s FROM Settings s WHERE s.configKey LIKE 'theme.%' ORDER BY s.configKey ASC")
    List<Settings> findThemeSettings();

    /**
     * 查找站点配置
     */
    @Query("SELECT s FROM Settings s WHERE s.configKey LIKE 'site.%' ORDER BY s.configKey ASC")
    List<Settings> findSiteSettings();

    /**
     * 查找SEO配置
     */
    @Query("SELECT s FROM Settings s WHERE s.configKey LIKE 'seo.%' ORDER BY s.configKey ASC")
    List<Settings> findSeoSettings();

    /**
     * 查找社交媒体配置
     */
    @Query("SELECT s FROM Settings s WHERE s.configKey LIKE 'social.%' ORDER BY s.configKey ASC")
    List<Settings> findSocialSettings();
}
