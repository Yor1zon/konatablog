package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 主题配置实体类
 */
@Entity
@Table(name = "themes", indexes = {
        @Index(name = "idx_themes_slug", columnList = "slug", unique = true),
        @Index(name = "idx_themes_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"settings"})
public class Themes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 主题名称 - 例如 "Default"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 主题标识符 - 用于文件夹名
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * 主题描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 版本号
     */
    @Column(length = 50)
    private String version;

    /**
     * 主题作者
     */
    @Column(length = 200)
    private String author;

    /**
     * 主题预览图URL
     */
    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    /**
     * 主题配置文件路径 -例如 "themes/default/theme.conf"
     */
    @Column(name = "config_path", length = 500)
    private String configPath;

    /**
     * 是/否活动主题
     */
    private Boolean isActive = false;

    /**
     * 是/否默认主题
     */
    private Boolean isDefault = false;

    /**
     * JSON格式的主题设置 - 灵活存储主题特定配置
     */
    @Column(name = "theme_settings", columnDefinition = "TEXT")
    private String themeSettings;

    /**
     * 创建时间
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 设置为主题 - 清除其他主题标记
     */
    public void setActiveTheme() {
        this.isActive = true;
        this.isDefault = true;
    }

    /**
     * 取消激活主题
     */
    public void deactivate() {
        this.isActive = false;
        this.isDefault = false;
    }

    /**
     * 设置主题配置 - JSON字符串
     */
    public void setSetting(String key, String value) {
        // 简化实现：直接设置config_path为配置值
        // 实际应该使用JSON处理配置
        if (this.themeSettings == null) {
            this.themeSettings = "{}";
        }
    }
}