package wiki.kana.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体类 - 管理博客管理员信息
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "mediaList"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名 - 用于登录和显示
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码 - 使用BCrypt加密
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 显示名称 - 博主昵称
     */
    @Column(length = 100)
    private String displayName;

    /**
     * 邮箱地址
     */
    @Column(length = 150, unique = true)
    private String email;

    /**
     * 用户角色 - 管理员
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.ADMIN;

    /**
     * 用户头像URL
     */
    @Column(length = 500)
    private String avatarUrl;

    /**
     * 关于我信息
     */
    @Column(columnDefinition = "TEXT")
    private String bio;

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
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 是否激活
     */
    private Boolean isActive = true;

    /**
     * 关联：用户上传的媒体文件
     */
    @OneToMany(mappedBy = "uploadedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Media> mediaList = new ArrayList<>();

    /**
     * 关联：用户创建的博客
     */
    @OneToMany(mappedBy = "author")
    private List<Post> posts = new ArrayList<>();

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        ADMIN,      // 管理员
        EDITOR,      // 编辑
        USER        // 普通用户
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }
}