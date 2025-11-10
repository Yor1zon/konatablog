package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 媒体资源实体类 - 图片和文件管理
 */
@Entity
@Table(name = "media", indexes = {
        @Index(name = "idx_media_type", columnList = "type"),
        @Index(name = "idx_media_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_media_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名（不包含扩展名）
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * 存储的文件名 - UUID + 扩展名
     */
    @Column(name = "file_name", nullable = false, unique = true, length = 255)
    private String fileName;

    /**
     * 文件扩展名 - .jpg, .png, .gif等
     */
    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    /**
     * 文件大小 - 单位：字节
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * 媒体类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    /**
     * CDN/URL地址
     */
    @Column(name = "cdn_url", length = 500)
    private String cdnUrl;

    /**
     * 本地存储路径
     */
    @Column(name = "local_url", length = 500)
    private String localUrl;

    /**
     * 图片宽度（像素）
     */
    @Column(name = "width")
    private Integer width;

    /**
     * 图片高度（像素）
     */
    @Column(name = "height")
    private Integer height;

    /**
     * 图片描述 - 用于alt属性
     */
    @Column(length = 500)
    private String description;

    /**
     * 替代文本 - SEO和可访问性
     */
    @Column(length = 255)
    private String altText;

    /**
     * 上传者 - 关联用户ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /**
     * 创建时间
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 媒体类型枚举
     */
    public enum MediaType {
        IMAGE,         // 图片
        AVATAR,         // 用户头像
        BANNER,         // 头栏图片
        THUMBNAIL,      // 缩略图
        DOCUMENT,    // 文档
        VIDEO,          // 视频
        AUDIO             // 音频
    }

    /**
     * 生成完整URL - 本地存储时优先使用localUrl
     */
    public String getFullUrl() {
        if (this.cdnUrl != null && !this.cdnUrl.isEmpty()) {
            return this.cdnUrl;
        }
        return this.localUrl;
    }

    /**
     * 获取文件大小
     */
    public String getFormattedFileSize() {
        double bytes = this.fileSize;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;

        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", bytes, units[unitIndex]);
    }

    /**
     * 判断是否为图片类型
     */
    public boolean isImage() {
        return this.type == MediaType.IMAGE ||
               this.type == MediaType.AVATAR ||
               this.type == MediaType.BANNER ||
               this.type == MediaType.THUMBNAIL;
    }

    /**
     * 获取图片/文件访问URL
     */
    public String getUrl() {
        return getFullUrl();
    }

    /**
     * 设置访问URL
     */
    public void setUrl(String url) {
        // 优先使用CDN，备选为本地路径
        if (isImage()) {
            // 图片直接使用URL
            if (url.startsWith("http")) {
                this.cdnUrl = url;
            } else {
                this.localUrl = url;
            }
        }
    }
}