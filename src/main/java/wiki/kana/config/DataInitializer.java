package wiki.kana.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import wiki.kana.entity.Category;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;
import wiki.kana.entity.User;
import wiki.kana.repository.CategoryRepository;
import wiki.kana.repository.PostRepository;
import wiki.kana.repository.TagRepository;
import wiki.kana.repository.UserRepository;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 * 在应用启动时创建测试用户和初始化数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUser();
        initializeDefaultCategory();
        initializeSamplePost();
    }

    /**
     * 创建默认管理员用户
     */
    private void initializeDefaultUser() {
        try {
            // 仅在首次启动（用户表为空）时创建默认用户，避免覆盖/干扰现有数据
            if (userRepository.count() == 0) {
                User adminUser = User.builder()
                        .username("admin")
                        .email("admin@blog.com")
                        .password(passwordEncoder.encode("admin123"))
                        .displayName("konatabloger")
                        .role(User.UserRole.ADMIN)
                        .isActive(true)
                        .build();

                userRepository.save(adminUser);
                log.info("✅ 默认管理员用户创建成功: admin/admin123");
            } else {
                log.info("ℹ️ 用户数据已存在，跳过默认用户创建");
            }
        } catch (Exception e) {
            log.error("❌ 创建默认用户失败", e);
        }
    }

    /**
     * 创建默认分类
     */
    private void initializeDefaultCategory() {
        try {
            // 仅在首次启动（分类表为空）时创建默认分类
            if (categoryRepository.count() == 0) {
                Category defaultCategory = Category.builder()
                        .name("默认分类")
                        .slug("default")
                        .description("系统默认分类")
                        .sortOrder(0)
                        .isActive(true)
                        .build();

                categoryRepository.save(defaultCategory);
                log.info("✅ 默认分类创建成功: {} ({})", defaultCategory.getName(), defaultCategory.getSlug());
            } else {
                log.info("ℹ️ 分类数据已存在，跳过默认分类创建");
            }
        } catch (Exception e) {
            log.error("❌ 创建默认分类失败", e);
        }
    }

    /**
     * 创建示例文章（绑定 hello world 标签）
     */
    private void initializeSamplePost() {
        try {
            if (postRepository.count() != 0) {
                log.info("ℹ️ 文章数据已存在，跳过示例文章创建");
                return;
            }

            User author = userRepository.findByUsername("admin")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
            if (author == null) {
                log.warn("⚠️ 未找到可用作者，跳过示例文章创建");
                return;
            }

            Category category = categoryRepository.findBySlug("default")
                    .orElseGet(() -> categoryRepository.findAll().stream().findFirst().orElse(null));
            if (category == null) {
                log.warn("⚠️ 未找到可用分类，跳过示例文章创建");
                return;
            }

            Post post = Post.builder()
                    .title("欢迎来到 KonataBlog!")
                    .excerpt("这是一篇示例文章。")
                    .slug("hello-world")
                    .content("""
                            欢迎来到 KonataBlog!  
                            
                            这是一篇示例文章，用于验证系统初始化与基础功能。如果您看到了这篇文章，恭喜您成功部署了本项目！  
                            
                            本项目是一个轻量级的博客，旨在让用户用 Docker 快速地部署一个简洁美观的文字博客，记录想法和灵感。
                            """)
                    .status(Post.PostStatus.PUBLISHED)
                    .publishedAt(LocalDateTime.now())
                    .category(category)
                    .author(author)
                    .build();

            Post savedPost = postRepository.save(post);

            Tag tag = tagRepository.findByName("hello world")
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                                .name("hello world")
                                .description("示例标签")
                                .build();
                        newTag.generateSlug();
                        return tagRepository.save(newTag);
                    });

            // Tag 是 owning side，必须保存 Tag 才会写入 post_tags 关联表
            tag.addPost(savedPost);
            tagRepository.save(tag);

            log.info("✅ 示例文章创建成功: {} (slug: {}), tag: {}", savedPost.getTitle(), savedPost.getSlug(), tag.getName());
        } catch (Exception e) {
            log.error("❌ 创建示例文章失败", e);
        }
    }
}
