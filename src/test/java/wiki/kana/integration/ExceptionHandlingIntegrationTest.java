package wiki.kana.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import wiki.kana.entity.*;
import wiki.kana.exception.*;
import wiki.kana.service.*;

import static org.assertj.core.api.Assertions.*;

/**
 * 异常处理集成测试 - 真实数据库
 * 测试Service层的异常处理和数据验证
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("异常处理集成测试 (真实数据库)")
@Transactional
class ExceptionHandlingIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Test
    @DisplayName("用户认证异常场景")
    void userAuthenticationExceptionScenarios() {
        // 测试不存在的用户认证
        assertThatThrownBy(() -> userService.authenticate("nonexistent_user", "password"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        String baseUsername = "auth_test_user_" + System.currentTimeMillis();
        String baseEmail = baseUsername + "@test.com";

        // 测试错误密码认证
        User testUser = User.builder()
                .username(baseUsername)
                .password("correctPassword")
                .email(baseEmail)
                .build();
        User createdUser = userService.createUser(testUser);

        try {
            // 错误密码
            assertThatThrownBy(() -> userService.authenticate(baseUsername, "wrongPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid password");

            // 禁用用户认证
            User deactivatedUser = userService.deactivateUser(createdUser.getId());
            assertThatThrownBy(() -> userService.authenticate(baseUsername, "correctPassword"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deactivated");
        } finally {
            userService.deleteUser(createdUser.getId());
        }
    }

    @Test
    @DisplayName("重复数据约束验证")
    void duplicateDataConstraintValidation() {
        String baseUsername = "unique_user_" + System.currentTimeMillis();
        String baseEmail = baseUsername + "@test.com";

        // 创建测试用户
        User user1 = User.builder()
                .username(baseUsername)
                .password("password123")
                .email(baseEmail)
                .build();
        User createdUser1 = userService.createUser(user1);

        try {
            // 测试重复用户名
            User duplicateUsernameUser = User.builder()
                    .username(baseUsername)
                    .password("password456")
                    .email("different_" + baseEmail)
                    .build();

            assertThatThrownBy(() -> userService.createUser(duplicateUsernameUser))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Username already exists");

            // 测试重复邮箱
            User duplicateEmailUser = User.builder()
                    .username(baseUsername + "_diff")
                    .password("password456")
                    .email(baseEmail)
                    .build();

            assertThatThrownBy(() -> userService.createUser(duplicateEmailUser))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");
        } finally {
            userService.deleteUser(createdUser1.getId());
        }
    }

    @Test
    @DisplayName("资源不存在异常")
    void resourceNotFoundExceptionScenarios() {
        // 测试不存在的用户
        assertThatThrownBy(() -> userService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        // 测试不存在的文章
        assertThatThrownBy(() -> postService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");

        // 测试不存在的分类
        assertThatThrownBy(() -> categoryService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class);

        // 测试不存在的标签
        assertThatThrownBy(() -> tagService.findById(99999L))
                .isInstanceOf(ResourceNotFoundException.class);

        // 测试删除不存在的资源
        assertThatThrownBy(() -> userService.deleteUser(99999L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> postService.deletePost(99999L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> categoryService.deleteCategory(99999L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> tagService.deleteTag(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("无效输入数据验证")
    void invalidInputDataValidation() {
        // 测试空用户名
        User emptyUsernameUser = User.builder()
                .username("")
                .password("password123")
                .build();

        assertThatThrownBy(() -> userService.createUser(emptyUsernameUser))
                .isInstanceOf(IllegalArgumentException.class);

        // 测试短密码
        User shortPasswordUser = User.builder()
                .username("valid_username")
                .password("short")
                .build();

        assertThatThrownBy(() -> userService.createUser(shortPasswordUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 6 characters long");

        // 测试无效邮箱
        User invalidEmailUser = User.builder()
                .username("valid_username")
                .password("password123")
                .email("invalid_email")
                .build();

        assertThatThrownBy(() -> userService.createUser(invalidEmailUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("文章操作异常场景")
    void postOperationExceptionScenarios() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 创建测试用户和分类
        User author = User.builder()
                .username("post_test_author_" + suffix)
                .password("password123")
                .email("author_" + suffix + "@test.com")
                .build();
        User createdAuthor = userService.createUser(author);

        Category category = Category.builder()
                .name("测试分类_" + suffix)
                .description("测试分类描述")
                .build();
        Category createdCategory = categoryService.createCategory(category, null);

        try {
            // 测试关联不存在的作者
            Post postWithInvalidAuthor = Post.builder()
                    .title("无效作者文章")
                    .content("内容")
                    .build();

            assertThatThrownBy(() -> postService.createPost(postWithInvalidAuthor, 99999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Author not found");

            // 测试创建空文章
            Post emptyPost = Post.builder()
                    .title("")
                    .content("")
                    .build();

            assertThatThrownBy(() -> postService.createPost(emptyPost, createdAuthor.getId()))
                    .isInstanceOf(IllegalArgumentException.class);

        } finally {
            // 清理测试数据
            userService.deleteUser(createdAuthor.getId());
            categoryService.deleteCategory(createdCategory.getId());
        }
    }

    @Test
    @DisplayName("分类和标签操作异常场景")
    void categoryAndTagOperationExceptionScenarios() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String categoryName = "重复分类名_" + suffix;
        String tagName = "重复标签名_" + suffix;

        // 测试重复分类名称
        Category category1 = Category.builder()
                .name(categoryName)
                .description("第一个分类")
                .build();
        Category createdCategory1 = categoryService.createCategory(category1, null);

        try {
            Category category2 = Category.builder()
                    .name(categoryName)
                    .description("第二个分类")
                    .build();

            assertThatThrownBy(() -> categoryService.createCategory(category2, null))
                    .isInstanceOf(DuplicateResourceException.class);

            // 测试重复标签名称
            Tag tag1 = Tag.builder()
                    .name(tagName)
                    .description("第一个标签")
                    .build();
            Tag createdTag1 = tagService.createTag(tag1);

            Tag tag2 = Tag.builder()
                    .name(tagName)
                    .description("第二个标签")
                    .build();

            assertThatThrownBy(() -> tagService.createTag(tag2))
                    .isInstanceOf(DuplicateResourceException.class);

        } finally {
            // 清理测试数据
            categoryService.deleteCategory(createdCategory1.getId());
        }
    }

    @Test
    @DisplayName("边界条件测试")
    void boundaryConditionTests() {
        // 测试超长字符串
        String longString = "a".repeat(300); // 300字符

        User longUsernameUser = User.builder()
                .username("a".repeat(100)) // 超过50字符限制
                .password("password123")
                .build();

        assertThatThrownBy(() -> userService.createUser(longUsernameUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed");

        // 测试空值处理
        User emptyUser = User.builder()
                .username("")
                .password("")
                .build();

        assertThatThrownBy(() -> userService.createUser(emptyUser))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
