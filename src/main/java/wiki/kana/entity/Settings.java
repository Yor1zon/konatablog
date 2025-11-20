package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统设置实体类 - 存储博客系统配置
 */
@Entity
@Table(name = "settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"updateBy"})
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键 - 例如 "site.title"
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置组 - 比如 "site", "seo", "social", "system"
     */
    @Column(name = "config_group", length = 50)
    @Builder.Default
    private String configGroup = "site";

    /**
     * 选项类型 - 控制如何读取配置值
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", length = 20)
    @Builder.Default
    private OptionType optionType = OptionType.TEXT;

    /**
     * 配置说明 - 用于后台管理
     */
    @Column(length = 500)
    private String description;

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
     * 最后修改者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updateBy;

    /**
     * 选项类型枚举
     */
    public enum OptionType {
        TEXT,           // 文本框
        TEXTAREA,       // 多行文本
        NUMBER,         // 数字
        CHECKBOX,         // 复选框
        SELECT,          // 下拉选择
        EMAIL,          // 邮箱
        URL,              // 网址
        PASSWORD          // 密码
    }

    /**
     * 常见配置键常量
     */
    public static class ConfigKeys {

        // Site 配置
        public static final String SITE_TITLE = "site.title";              // 网站标题
        public static final String SITE_TAGLINE = "site.tagline";          // 网站副标题
        public static final String SITE_DESCRIPTION = "site.description";  // 网站描述
        public static final String SITE_KEYWORD = "site.keywords";          // 搜索关键词
        public static final String SITE_EMAIL = "site.email";              // 客服邮箱
        public static final String AUTHOR_NAME = "author.name";            // 作者名称
        public static final String SITE_PAGE_SIZE = "site.page_size";      // 页面大小
        public static final String COMMENTS_ENABLED = "site.comments.enabled"; // 评论开启

        // 社交媒体
        public static final String SOCIAL_GITHUB = "social.github";    // GitHub
        public static final String SOCIAL_WEIBO = "social.weibo";        // 微博
        public static final String SOCIAL_TWITTER = "social.twitter";      // Twitter/X

        // SEO 配置
        public static final String SEO_ROBOTS_META = "seo.robots_meta";    // robots meta
        public static final String SEO_SITE_VERIFICATION = "seo.google_site_verification";  // Google验证

        // 系统 配置
        public static final String SYSTEM_ADMIN_EMAIL = "system.admin_email";           // 管理员邮箱

        // 主题 配置
        public static final String THEME_CURRENT = "theme.current";                 // 当前主题
    }

    /**
     * 获取配置值（文本类型）
     */
    public String getValueAsString() {
        return this.configValue;
    }

    /**
     * 获取配置值（数字类型）
     */
    public Integer getValueAsInteger() {
        if (this.configValue == null || this.configValue.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(this.configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取配置值（布尔类型）
     */
    public Boolean getValueAsBoolean() {
        if (this.configValue == null) return null;
        return "true".equalsIgnoreCase(this.configValue) ||
               "1".equals(this.configValue) ||
               "yes".equalsIgnoreCase(this.configValue);
    }

    /**
     * 判断是否为指定的配置键
     */
    public boolean isKey(String key) {
        return this.configKey.equals(key);
    }
}
