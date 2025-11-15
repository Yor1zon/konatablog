package wiki.kana.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import wiki.kana.entity.*;
import wiki.kana.service.*;

import java.util.List;

/**
 * 数据库验证命令行工具 - 专门用于检查测试数据是否真实写入数据库
 * 运行命令: ./mvnw spring-boot:run -Dspring-boot.run.profiles=integration -Dspring-boot.run.main-class=wiki.kana.integration.DatabaseVerificationCLI
 */
@SpringBootApplication
@Profile("integration")
public class DatabaseVerificationCLI implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostService postService;

    @Autowired
    private TagService tagService;

    public static void main(String[] args) {
        SpringApplication.run(DatabaseVerificationCLI.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== KONATABLOG 数据库验证工具 ===");
        System.out.println("正在检查数据库: data/konatablog-integration.db");
        System.out.println();

        try {
            // 1. 检查用户表
            System.out.println("📋 用户表 (users)");
            System.out.println("================================================");
            List<User> users = userService.findAll();
            if (users.isEmpty()) {
                System.out.println("❌ 用户表为空");
            } else {
                System.out.println("✅ 找到 " + users.size() + " 个用户:");
                for (User user : users) {
                    System.out.println("   ID: " + user.getId() +
                                     ", 用户名: " + user.getUsername() +
                                     ", 邮箱: " + user.getEmail() +
                                     ", 角色: " + user.getRole() +
                                     ", 激活状态: " + user.getIsActive());
                }
            }
            System.out.println();

            // 2. 检查分类表
            System.out.println("📂 分类表 (categories)");
            System.out.println("================================================");
            List<Category> categories = categoryService.findAll();
            if (categories.isEmpty()) {
                System.out.println("❌ 分类表为空");
            } else {
                System.out.println("✅ 找到 " + categories.size() + " 个分类:");
                for (Category category : categories) {
                    System.out.println("   ID: " + category.getId() +
                                     ", 名称: " + category.getName() +
                                     ", 描述: " + category.getDescription() +
                                     ", 父分类ID: " + (category.getParent() != null ? category.getParent().getId() : "无") +
                                     ", 激活状态: " + category.getIsActive());
                }
            }
            System.out.println();

            // 3. 检查文章表
            System.out.println("📝 文章表 (posts)");
            System.out.println("================================================");
            List<Post> posts = postService.findAll();
            if (posts.isEmpty()) {
                System.out.println("❌ 文章表为空");
            } else {
                System.out.println("✅ 找到 " + posts.size() + " 篇文章:");
                for (Post post : posts) {
                    System.out.println("   ID: " + post.getId() +
                                     ", 标题: " + post.getTitle() +
                                     ", 作者ID: " + (post.getAuthor() != null ? post.getAuthor().getId() : "无") +
                                     ", 分类ID: " + (post.getCategory() != null ? post.getCategory().getId() : "无") +
                                     ", 状态: " + post.getStatus() +
                                     ", 查看次数: " + post.getViewCount());
                }
            }
            System.out.println();

            // 4. 检查标签表
            System.out.println("🏷️  标签表 (tags)");
            System.out.println("================================================");
            List<Tag> tags = tagService.findAll();
            if (tags.isEmpty()) {
                System.out.println("❌ 标签表为空");
            } else {
                System.out.println("✅ 找到 " + tags.size() + " 个标签:");
                for (Tag tag : tags) {
                    System.out.println("   ID: " + tag.getId() +
                                     ", 名称: " + tag.getName() +
                                     ", 描述: " + tag.getDescription() +
                                     ", 使用次数: " + tag.getUsageCount());
                }
            }
            System.out.println();

            // 5. 显示统计信息
            System.out.println("📊 数据库统计信息");
            System.out.println("================================================");
            System.out.println("用户总数: " + userService.countAllUsers());
            System.out.println("分类总数: " + categories.size());
            System.out.println("文章总数: " + postService.countAllPosts());
            System.out.println("已发布文章数: " + postService.countPublishedPosts());
            System.out.println("标签总数: " + tagService.countAllTags());
            System.out.println();

            // 6. 验证结果
            boolean hasData = !users.isEmpty() && !categories.isEmpty() && !posts.isEmpty();
            if (hasData) {
                System.out.println("🎉 验证成功！数据库包含真实数据。");
                System.out.println("💡 您可以使用SQLite客户端直接查看 data/konatablog-integration.db 文件");
            } else {
                System.out.println("⚠️  数据库为空或数据不完整。");
            }

        } catch (Exception e) {
            System.err.println("❌ 数据库验证失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 自动退出
        System.exit(0);
    }
}