package wiki.kana.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import wiki.kana.entity.User;
import wiki.kana.entity.Category;
import wiki.kana.entity.Post;
import wiki.kana.entity.User.UserRole;
import wiki.kana.entity.Post.PostStatus;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.service.UserService;
import wiki.kana.service.CategoryService;
import wiki.kana.service.PostService;
import wiki.kana.service.TagService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 正常工作的业务集成测试
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("业务集成测试")
@Transactional
class WorkingBusinessIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostService postService;

    @Autowired
    private TagService tagService;

    @Test
    @DisplayName("用户管理集成测试")
    void userManagementIntegrationTest() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 创建用户
        User user = User.builder()
                .username("integration_user_" + suffix)
                .password("testPassword123")
                .email("integration_" + suffix + "@test.com")
                .displayName("集成测试用户")
                .role(UserRole.EDITOR)
                .build();

        User createdUser = userService.createUser(user);
        System.out.println("=== 创建用户成功: " + createdUser.getUsername() + " (ID: " + createdUser.getId() + ") ===");

        // 验证用户创建
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo(user.getUsername());

        // 用户认证
        User authenticatedUser = userService.authenticate(user.getUsername(), "testPassword123");
        System.out.println("=== 用户认证成功 ===");

        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getId()).isEqualTo(createdUser.getId());

        // 验证用户统计
        long userCount = userService.countAllUsers();
        assertThat(userCount).isGreaterThan(0);
        System.out.println("=== 用户总数: " + userCount + " ===");

        // 清理
        userService.deleteUser(createdUser.getId());
        System.out.println("=== 测试用户已清理 ===");
    }

    @Test
    @DisplayName("分类管理集成测试")
    void categoryManagementIntegrationTest() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 创建父分类
        Category parentCategory = Category.builder()
                .name("技术分类_" + suffix)
                .description("技术相关内容")
                .build();

        Category createdParent = categoryService.createCategory(parentCategory, null);
        System.out.println("=== 创建父分类: " + createdParent.getName() + " ===");

        assertThat(createdParent).isNotNull();
        assertThat(createdParent.getId()).isNotNull();
        assertThat(createdParent.getParent()).isNull();

        // 创建子分类
        Category childCategory = Category.builder()
                .name("Java分类_" + suffix)
                .description("Java相关内容")
                .build();

        Category createdChild = categoryService.createCategory(childCategory, createdParent.getId());
        System.out.println("=== 创建子分类: " + createdChild.getName() + " (父分类: " + createdParent.getName() + ") ===");

        assertThat(createdChild).isNotNull();
        assertThat(createdChild.getParent().getId()).isEqualTo(createdParent.getId());

        // 验证层次结构
        List<Category> children = categoryService.findChildren(createdParent.getId());
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getId()).isEqualTo(createdChild.getId());

        // 清理
        categoryService.deleteCategory(createdChild.getId());
        categoryService.deleteCategory(createdParent.getId());
        System.out.println("=== 分类已清理 ===");
    }

    @Test
    @DisplayName("文章创建发布集成测试")
    void postCreationIntegrationTest() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 创建作者
        User author = User.builder()
                .username("author_" + suffix)
                .password("testPassword123")
                .email("author_" + suffix + "@test.com")
                .role(UserRole.EDITOR)
                .displayName("测试作者")
                .build();

        User createdAuthor = userService.createUser(author);
        System.out.println("=== 创建作者: " + createdAuthor.getUsername() + " ===");

        // 创建分类
        Category category = Category.builder()
                .name("技术博客_" + suffix)
                .description("技术相关博客")
                .build();

        Category createdCategory = categoryService.createCategory(category, null);
        System.out.println("=== 创建分类: " + createdCategory.getName() + " ===");

        // 创建文章
        Post post = Post.builder()
                .title("Spring Boot集成测试_" + suffix)
                .content("这是一篇关于Spring Boot集成测试的文章...")
                .excerpt("Spring Boot集成测试摘要")
                .status(PostStatus.DRAFT)
                .build();

        post.setCategory(createdCategory);
        Post createdPost = postService.createPost(post, createdAuthor.getId());
        System.out.println("=== 创建文章: " + createdPost.getTitle() + " (状态: " + createdPost.getStatus() + ") ===");

        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getId()).isNotNull();
        assertThat(createdPost.getAuthor().getId()).isEqualTo(createdAuthor.getId());
        assertThat(createdPost.getCategory().getId()).isEqualTo(createdCategory.getId());

        // 发布文章
        Post publishedPost = postService.publishPost(createdPost.getId());
        System.out.println("=== 发布文章成功 ===");

        assertThat(publishedPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(publishedPost.getPublishedAt()).isNotNull();

        // 验证统计
        long authorPostCount = postService.countPostsByAuthor(createdAuthor.getId());
        assertThat(authorPostCount).isEqualTo(1);

        long publishedCount = postService.countPublishedPosts();
        assertThat(publishedCount).isGreaterThanOrEqualTo(1);

        System.out.println("=== 统计信息 ===");
        System.out.println("作者文章数: " + authorPostCount);
        System.out.println("发布文章数: " + publishedCount);
        System.out.println("系统用户数: " + userService.countAllUsers());
        System.out.println("系统分类数: " + categoryService.findAll().size());

        // 清理
        postService.deletePost(publishedPost.getId());
        userService.deleteUser(createdAuthor.getId());
        categoryService.deleteCategory(createdCategory.getId());
        System.out.println("=== 测试数据已清理 ===");
    }

    @Test
    @DisplayName("数据库持久化验证")
    void databasePersistenceTest() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 创建用户验证持久化
        User user = User.builder()
                .username("persistence_user_" + suffix)
                .password("testPassword123")
                .email("persistence_" + suffix + "@test.com")
                .build();

        User createdUser = userService.createUser(user);

        // 验证数据持久化
        User foundById = userService.findById(createdUser.getId());
        assertThat(foundById).isNotNull();
        assertThat(foundById.getUsername()).isEqualTo(user.getUsername());

        // 验证用户统计
        long userCount = userService.countAllUsers();
        assertThat(userCount).isGreaterThan(0);

        System.out.println("=== 数据库持久化验证成功 ===");
        System.out.println("用户ID: " + createdUser.getId());
        System.out.println("用户总数: " + userCount);

        // 清理
        userService.deleteUser(createdUser.getId());
        System.out.println("=== 持久化测试数据已清理 ===");
    }

    @Test
    @DisplayName("异常处理集成测试")
    void exceptionHandlingIntegrationTest() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 测试资源不存在异常
        assertThatThrownBy(() -> userService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        assertThatThrownBy(() -> categoryService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        assertThatThrownBy(() -> postService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");

        // 测试重复资源异常
        User user = User.builder()
                .username("duplicate_user_" + suffix)
                .password("testPassword123")
                .email("duplicate_" + suffix + "@test.com")
                .build();

        User createdUser = userService.createUser(user);

        User duplicateUser = User.builder()
                .username("duplicate_user_" + suffix)  // 同一用户名
                .password("testPassword123")
                .email("different_" + suffix + "@test.com")
                .build();

        assertThatThrownBy(() -> userService.createUser(duplicateUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");

        System.out.println("=== 异常处理验证成功 ===");

        // 清理
        userService.deleteUser(createdUser.getId());
        System.out.println("=== 异常测试数据已清理 ===");
    }
}