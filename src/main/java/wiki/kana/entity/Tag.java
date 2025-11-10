package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签实体类
 */
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tags_slug", columnList = "slug", unique = true),
        @Index(name = "idx_tags_name", columnList = "name", unique = true),
        @Index(name = "idx_posts_usage", columnList = "usage_count")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"posts"})
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标签名称 - 例如 "Java", "Spring Boot"
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * URL友好的标识符 - 例如 "java", "spring-boot"
     */
    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    /**
     * 标签描述 - 用途说明
     */
    @Column(length = 500)
    private String description;

    /**
     * 使用次数 - 关联的博客数量
     */
    @Column(name = "usage_count", columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * 标签颜色 - 前端显示用（可选）
     */
    @Column(length = 7)  // #FF5733
    private String color;

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
     * 关联：使用该标签的所有博客
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "tag_id"),
        inverseJoinColumns = @JoinColumn(name = "post_id")
    )
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    /**
     * 自动生成 URL 友好的 slug - 从 name 生成
     */
    public void generateSlug() {
        if (this.name != null) {
            // 生成规则：
            // 1. 转为小写
            // 2. 去掉特殊字符
            // 3. 空格替换为连字符
            this.slug = this.name
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .trim();
        }
    }

    /**
     * 检查是否已被使用 - 使用次数 > 0
     */
    public boolean isUsed() {
        return this.usageCount > 0;
    }

    /**
     * 增加使用计数
     */
    public void incrementUsage() {
        this.usageCount++;
    }

    /**
     * 减少使用计数（不能小于0）
     */
    public void decrementUsage() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }

    /**
     * 获取关联博客数量
     */
    public int getPostCount() {
        return this.posts != null ? this.posts.size() : 0;
    }

    /**
     * 添加关联博客
     */
    public void addPost(Post post) {
        if (this.posts == null) {
            this.posts = new ArrayList<>();
        }
        if (!this.posts.contains(post)) {
            this.posts.add(post);
            this.usageCount++;
        }
    }

    /**
     * 移除关联博客
     */
    public void removePost(Post post) {
        if (this.posts != null) {
            this.posts.remove(post);
            if (this.usageCount > 0) {
                this.usageCount--;
            }
        }
    }
}