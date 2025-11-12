package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 博客文章实体类
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_slug", columnList = "slug", unique = true),
        @Index(name = "idx_posts_status_published_at", columnList = "status, published_at"),
        @Index(name = "idx_posts_category", columnList = "category_id"),
        @Index(name = "idx_posts_author", columnList = "author_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"content", "tags", "author"})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 博客标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * URL友好的标识符 - 用户友好的URL
     */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    /**
     * 浏览次数统计
     */
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 是否为特色博客
     */
    private Boolean isFeatured = false;

    /**
     * 博客状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    /**
     * Markdown 原文内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 自动生成的摘要 - HTML渲染后的摘要（500字符内）
     */
    @Column(columnDefinition = "TEXT", name = "excerpt")
    private String excerpt;

    /**
     * 标签集合 - 多对多关系
     */
    @ManyToMany(mappedBy = "posts", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    /**
     * 分类ID（外键）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * 作者ID（外键）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 博客头图 - 一对一关系
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "banner_media_id")
    private Media bannerImage;

    /**
     * 博客中的所有媒体文件 - 多对多关系
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_media",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "media_id")
    )
    @Builder.Default
    private List<Media> mediaFiles = new ArrayList<>();

    /**
     * 发布时间 - 草稿为null
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 创建时间 - 后台生成
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 后台维护
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 阅读量统计 - 每次查看时+1
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 设置为已发布状态 - 自动设置发布时间
     */
    public void publish() {
        if (this.status != PostStatus.PUBLISHED) {
            this.status = PostStatus.PUBLISHED;
            this.publishedAt = LocalDateTime.now();
        }
    }

    /**
     * 设置为草稿状态 - 清除发布时间
     */
    public void setToDraft() {
        this.status = PostStatus.DRAFT;
        this.publishedAt = null;
    }

    /**
     * 判断是否已发布
     */
    public boolean isPublished() {
        return this.status == PostStatus.PUBLISHED && this.publishedAt != null;
    }

    /**
     * 获取博客标签列表 - List<Tag> 类型
     */
    public List<Tag> getTagList() {
        return this.tags != null ? this.tags : new ArrayList<>();
    }

    /**
     * 设置博客标签 - 支持不同格式
     */
    public void addTag(Tag tag) {
        if (tag != null && this.tags != null && !this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    /**
     * 移除标签
     */
    public void removeTag(Tag tag) {
        if (tag != null && this.tags != null) {
            this.tags.remove(tag);
        }
    }

    /**
     * 设置博客头图
     */
    public void setBannerImage(Media bannerImage) {
        this.bannerImage = bannerImage;
    }

    /**
     * 获取博客头图URL
     */
    public String getBannerImageUrl() {
        return this.bannerImage != null ? this.bannerImage.getUrl() : null;
    }

    /**
     * 添加媒体文件到博客
     */
    public void addMediaFile(Media media) {
        if (media != null && this.mediaFiles != null && !this.mediaFiles.contains(media)) {
            this.mediaFiles.add(media);
        }
    }

    /**
     * 移除媒体文件
     */
    public void removeMediaFile(Media media) {
        if (media != null && this.mediaFiles != null) {
            this.mediaFiles.remove(media);
        }
    }

    /**
     * 获取博客中的图片文件
     */
    public List<Media> getImageFiles() {
        if (this.mediaFiles == null) {
            return new ArrayList<>();
        }
        return this.mediaFiles.stream()
                .filter(Media::isImage)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查博客是否包含指定媒体文件
     */
    public boolean containsMedia(Media media) {
        return this.mediaFiles != null && this.mediaFiles.contains(media);
    }

    /**
     * 博客状态枚举
     */
    public enum PostStatus {
        DRAFT,           // 草稿 (未发布)
        PUBLISHED,      // 已发布 (公开博客)
        ARCHIVED,        // 归档 (隐藏可见但可访问)
        PENDING          // 待审核（多用户时使用）
    }
}