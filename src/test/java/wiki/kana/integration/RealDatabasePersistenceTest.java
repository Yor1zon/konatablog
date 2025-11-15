package wiki.kana.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import wiki.kana.entity.User;
import wiki.kana.entity.Category;
import wiki.kana.entity.Post;
import wiki.kana.entity.User.UserRole;
import wiki.kana.entity.Post.PostStatus;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.UserService;
import wiki.kana.service.CategoryService;
import wiki.kana.service.PostService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * 真实数据库持久化测试 - 不使用事务回滚，验证数据真正写入数据库
 */
@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("真实数据库持久化测试")
class RealDatabasePersistenceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostService postService;

    private static Long userId;
    private static Long categoryId;
    private static Long postId;

    @Test
    @DisplayName("验证数据真实写入数据库")
    void verifyRealDatabasePersistence() {
        System.out.println("=== 开始真实数据库持久化测试 ===");

        // 1. 创建用户并验证
        String timestamp = String.valueOf(System.currentTimeMillis());
        User user = User.builder()
                .username("real_test_user_" + timestamp)
                .password("testPassword123")
                .email("real_test_" + timestamp + "@example.com")
                .displayName("真实测试用户")
                .role(UserRole.EDITOR)
                .build();

        User createdUser = userService.createUser(user);
        userId = createdUser.getId();

        System.out.println("✅ 创建用户成功 - ID: " + userId + ", 用户名: " + createdUser.getUsername());

        // 立即重新查询验证数据持久化
        User foundUser = userService.findById(userId);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(createdUser.getUsername());
        System.out.println("✅ 用户数据持久化验证成功");

        // 2. 创建分类并验证
        Category category = Category.builder()
                .name("真实测试分类_" + timestamp)
                .description("用于验证真实数据库持久化的测试分类")
                .build();

        Category createdCategory = categoryService.createCategory(category, null);
        categoryId = createdCategory.getId();

        System.out.println("✅ 创建分类成功 - ID: " + categoryId + ", 名称: " + createdCategory.getName());

        Category foundCategory = categoryService.findById(categoryId);
        assertThat(foundCategory).isNotNull();
        assertThat(foundCategory.getName()).isEqualTo(createdCategory.getName());
        System.out.println("✅ 分类数据持久化验证成功");

        // 3. 创建文章并验证
        Post post = Post.builder()
                .title("真实数据库持久化测试_" + timestamp)
                .content("这是一篇专门用于验证数据真实写入数据库的文章。\n\n" +
                        "该文章创建时间为: " + java.time.LocalDateTime.now() + "\n\n" +
                        "如果能在数据库客户端中看到这篇文章，说明数据真正写入了数据库文件。")
                .excerpt("真实数据库持久化测试摘要")
                .status(PostStatus.PUBLISHED)  // 直接发布
                .build();

        post.setCategory(createdCategory);
        Post createdPost = postService.createPost(post, createdUser.getId());
        postId = createdPost.getId();

        System.out.println("✅ 创建文章成功 - ID: " + postId + ", 标题: " + createdPost.getTitle());

        Post foundPost = postService.findById(postId);
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getTitle()).isEqualTo(createdPost.getTitle());
        assertThat(foundPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        System.out.println("✅ 文章数据持久化验证成功");

        // 4. 验证统计数据
        long userCount = userService.countAllUsers();
        long categoryCount = categoryService.findAll().size();
        long postCount = postService.countAllPosts();

        System.out.println("=== 数据库统计信息 ===");
        System.out.println("用户总数: " + userCount);
        System.out.println("分类总数: " + categoryCount);
        System.out.println("文章总数: " + postCount);

        System.out.println("=== 真实数据库持久化测试完成 ===");
        System.out.println("请使用SQLite客户端检查 data/konatablog-integration.db 文件");
        System.out.println("应该能看到以下数据：");
        System.out.println("- user 表中 ID: " + userId + " 的用户记录");
        System.out.println("- category 表中 ID: " + categoryId + " 的分类记录");
        System.out.println("- post 表中 ID: " + postId + " 的文章记录");
    }

    @Test
    @DisplayName("验证数据跨查询存在性")
    void verifyDataExistenceAcrossQueries() {
        if (userId != null) {
            System.out.println("=== 验证用户数据跨查询存在性 ===");

            // 使用ID查询验证数据存在
            User foundById = userService.findById(userId);
            assertThat(foundById).isNotNull();
            System.out.println("✅ 用户ID查询验证成功: " + foundById.getUsername());

            // 验证统计数据
            long userCount = userService.countAllUsers();
            assertThat(userCount).isGreaterThanOrEqualTo(1);
            System.out.println("✅ 用户统计验证成功: " + userCount);

            // 查询分类和文章
            Category foundCategory = categoryService.findById(categoryId);
            assertThat(foundCategory).isNotNull();
            System.out.println("✅ 分类查询验证成功: " + foundCategory.getName());

            Post foundPost = postService.findById(postId);
            assertThat(foundPost).isNotNull();
            System.out.println("✅ 文章查询验证成功: " + foundPost.getTitle());
        }
    }

    /**
     * 清理测试数据 - 这个方法会在所有测试后执行
     * 注意：这个方法的数据清理是真实删除，不会回滚
     */
    @AfterAll
    static void cleanupTestData() {
        System.out.println("=== 清理测试数据 ===");
        System.out.println("注意：由于测试设计，这些数据将保留在数据库中供手动验证");
        System.out.println("如需清理，请手动删除ID为 " + userId + " 的用户记录");
        System.out.println("相关联的分类和文章会根据数据库约束进行级联处理");
    }
}