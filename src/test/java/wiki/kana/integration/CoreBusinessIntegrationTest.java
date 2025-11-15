package wiki.kana.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import wiki.kana.entity.*;
import wiki.kana.exception.*;
import wiki.kana.integration.config.TestDataFactory;
import wiki.kana.service.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 核心业务集成测试 - 真实数据库
 * 测试Service层的核心业务逻辑和数据持久化
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("核心业务集成测试 (真实数据库)")
@Transactional
class CoreBusinessIntegrationTest {

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Test
    @DisplayName("用户创建和认证流程")
    void userCreationAndAuthenticationFlow() {
        // Given: 创建测试用户
        String username = "test_user_" + System.currentTimeMillis();
        User testUser = User.builder()
                .username(username)
                .password("testPassword123")
                .email(username + "@test.com")
                .displayName("Test User")
                .build();

        // When: 创建用户
        User createdUser = userService.createUser(testUser);

        // Then: 验证用户创建成功
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getIsActive()).isTrue();

        // When: 用户认证
        User authenticatedUser = userService.authenticate(username, "testPassword123");

        // Then: 验证认证成功
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getId()).isEqualTo(createdUser.getId());
        assertThat(authenticatedUser.getLastLoginAt()).isNotNull();

        // 验证用户可以被查询
        User foundUser = userService.findById(createdUser.getId());
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(username);

        // 清理：删除测试用户
        userService.deleteUser(createdUser.getId());
        assertThatThrownBy(() -> userService.findById(createdUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("分类创建和层次结构")
    void categoryCreationAndHierarchy() {
        // Given: 创建父分类
        Category parentCategory = Category.builder()
                .name("父分类_" + System.currentTimeMillis())
                .description("父分类描述")
                .build();

        // When: 创建父分类
        Category createdParent = categoryService.createCategory(parentCategory, null);

        // Then: 验证父分类创建成功
        assertThat(createdParent).isNotNull();
        assertThat(createdParent.getId()).isNotNull();
        assertThat(createdParent.getName()).isEqualTo(parentCategory.getName());
        assertThat(createdParent.getParent()).isNull();

        // Given: 创建子分类
        Category childCategory = Category.builder()
                .name("子分类_" + System.currentTimeMillis())
                .description("子分类描述")
                .build();

        // When: 创建子分类并关联父分类
        Category createdChild = categoryService.createCategory(childCategory, createdParent.getId());

        // Then: 验证子分类创建成功且关联正确
        assertThat(createdChild).isNotNull();
        assertThat(createdChild.getId()).isNotNull();
        assertThat(createdChild.getParent().getId()).isEqualTo(createdParent.getId());

        // 验证可以查询子分类
        List<Category> children = categoryService.findChildren(createdParent.getId());
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getId()).isEqualTo(createdChild.getId());

        // 清理：删除分类
        categoryService.deleteCategory(createdChild.getId());
        categoryService.deleteCategory(createdParent.getId());
    }

    @Test
    @DisplayName("标签创建和使用统计")
    void tagCreationAndUsageTracking() {
        // Given: 创建测试标签
        Tag testTag = Tag.builder()
                .name("测试标签_" + System.currentTimeMillis())
                .description("测试标签描述")
                .build();

        // When: 创建标签
        Tag createdTag = tagService.createTag(testTag);

        // Then: 验证标签创建成功
        assertThat(createdTag).isNotNull();
        assertThat(createdTag.getId()).isNotNull();
        assertThat(createdTag.getName()).isEqualTo(testTag.getName());
        assertThat(createdTag.getSlug()).isNotNull();

        // 验证可以查询标签
        Tag foundTag = tagService.findById(createdTag.getId());
        assertThat(foundTag).isNotNull();
        assertThat(foundTag.getName()).isEqualTo(testTag.getName());

        // 测试标签搜索
        List<Tag> searchResults = tagService.searchByName("测试标签");
        assertThat(searchResults).isNotEmpty();

        // 清理：删除标签
        tagService.deleteTag(createdTag.getId());
        assertThatThrownBy(() -> tagService.findById(createdTag.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("文章创建、发布和关联管理")
    void postCreationPublishingAndAssociation() {
        // Given: 准备基础数据
        User author = testDataFactory.createTestUser("post_author", "EDITOR");
        Category category = testDataFactory.createTestCategory("文章分类");
        Tag tag1 = testDataFactory.createTestTag("标签1");
        Tag tag2 = testDataFactory.createTestTag("标签2");

        // When: 创建文章
        Post newPost = Post.builder()
                .title("测试文章_" + System.currentTimeMillis())
                .content("这是一篇测试文章的内容...")
                .excerpt("测试文章摘要")
                .build();

        newPost.setTags(List.of(tag1, tag2));
        newPost.setCategory(category);
        Post createdPost = postService.createPost(newPost, author.getId());

        // Then: 验证文章创建成功
        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getId()).isNotNull();
        assertThat(createdPost.getStatus()).isEqualTo(Post.PostStatus.DRAFT);
        assertThat(createdPost.getTags()).hasSize(2);
        assertThat(createdPost.getCategory().getId()).isEqualTo(category.getId());
        assertThat(createdPost.getAuthor().getId()).isEqualTo(author.getId());

        // When: 发布文章
        Post publishedPost = postService.publishPost(createdPost.getId());

        // Then: 验证发布成功
        assertThat(publishedPost).isNotNull();
        assertThat(publishedPost.getStatus()).isEqualTo(Post.PostStatus.PUBLISHED);
        assertThat(publishedPost.getPublishedAt()).isNotNull();

        // 验证作者文章统计
        long authorPostCount = postService.countPostsByAuthor(author.getId());
        assertThat(authorPostCount).isGreaterThan(0);

        // 清理：删除文章
        postService.deletePost(publishedPost.getId());
        assertThatThrownBy(() -> postService.findById(publishedPost.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // 清理：删除测试数据
        userService.deleteUser(author.getId());
        categoryService.deleteCategory(category.getId());
        tagService.deleteTag(tag1.getId());
        tagService.deleteTag(tag2.getId());
    }

    @Test
    @DisplayName("完整博客内容创作流程")
    void completeBlogContentCreationFlow() {
        // Given: 创建作者用户
        User author = testDataFactory.createTestUser("content_creator", "EDITOR");

        // When: 创建分类
        Category techCategory = Category.builder()
                .name("技术分享")
                .description("技术相关文章")
                .build();
        Category createdCategory = categoryService.createCategory(techCategory, null);

        // 创建标签
        Tag javaTag = testDataFactory.createTestTag("Java");
        Tag springTag = testDataFactory.createTestTag("Spring");

        // 创建多篇文章
        for (int i = 1; i <= 3; i++) {
            Post post = Post.builder()
                    .title("技术文章 " + i)
                    .content("这是第 " + i + " 篇技术文章的内容")
                    .excerpt("技术文章 " + i + " 的摘要")
                    .build();

            post.setTags(List.of(javaTag, springTag));
            post.setCategory(createdCategory);
            Post createdPost = postService.createPost(post, author.getId());

            // 发布文章
            postService.publishPost(createdPost.getId());
        }

        // Then: 验证创作结果
        long authorPostCount = postService.countPostsByAuthor(author.getId());
        assertThat(authorPostCount).isEqualTo(3);

        long publishedCount = postService.countPublishedPosts();
        assertThat(publishedCount).isGreaterThanOrEqualTo(3);

        long categoryPostCount = categoryService.countPublishedPosts(createdCategory.getId());
        assertThat(categoryPostCount).isGreaterThanOrEqualTo(3);

        // 验证系统统计
        long totalPosts = postService.countAllPosts();
        assertThat(totalPosts).isGreaterThanOrEqualTo(3);

        long totalUsers = userService.countAllUsers();
        assertThat(totalUsers).isGreaterThan(0);

        long totalCategories = categoryService.findAll().size();
        assertThat(totalCategories).isGreaterThanOrEqualTo(1);

        long totalTags = tagService.countAllTags();
        assertThat(totalTags).isGreaterThanOrEqualTo(2);

        System.out.println("=== 博客内容创作流程验证成功 ===");
        System.out.println("作者文章数: " + authorPostCount);
        System.out.println("发布文章数: " + publishedCount);
        System.out.println("分类文章数: " + categoryPostCount);
        System.out.println("系统统计 - 文章: " + totalPosts + ", 用户: " + totalUsers +
                          ", 分类: " + totalCategories + ", 标签: " + totalTags);
    }

    @AfterEach
    void cleanupTestData() {
        try {
            // 清理测试创建的数据，避免影响其他测试
            List<Post> posts = postService.findAll();
            for (Post post : posts) {
                try {
                    postService.deletePost(post.getId());
                } catch (Exception e) {
                    // 忽略删除失败
                }
            }
        } catch (Exception e) {
            System.err.println("清理测试数据时出错: " + e.getMessage());
        }
    }
}
