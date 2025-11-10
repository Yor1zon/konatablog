package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 博客分类实体类
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_slug", columnList = "slug", unique = true),
        @Index(name = "idx_categories_parent", columnList = "parent_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"posts", "children"})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分类名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 分类描述
     */
    @Column(length = 500)
    private String description;

    /**
     * URL友好的标识符
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * 排序权重 - 数字越大越靠前
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Boolean isActive = true;

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
     * 父分类 - 支持层级分类（一级层级）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * 子分类列表
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    /**
     * 关联：分类下的博客
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    /**
     * 获取博客数量 - 统计方法（可通过JPA查询替代）
     */
    public int getPostCount() {
        return this.posts != null ? this.posts.size() : 0;
    }

    /**
     * 判断是否为顶级分类
     */
    public boolean isTopLevel() {
        return this.parent == null;
    }

    /**
     * 获取完整路径（从根到当前）
     */
    public String getFullPath() {
        if (this.parent == null) {
            return "/" + this.slug;
        }
        return parent.getFullPath() + "/" + this.slug;
    }
}