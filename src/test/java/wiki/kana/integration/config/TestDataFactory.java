package wiki.kana.integration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import wiki.kana.entity.*;
import wiki.kana.service.*;

import java.util.*;

/**
 * 测试数据工厂
 * 提供统一的测试数据创建和批量初始化功能
 */
@Slf4j
@Component
public class TestDataFactory {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ThemesService themesService;

    // ==================== 用户数据工厂 ====================

    /**
     * 创建测试用户
     */
    public User createTestUser(String username, String role) {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String normalizedUsername = sanitizeIdentifier(username, "user");

        User user = User.builder()
                .username(normalizedUsername + "_" + uniqueSuffix)
                .password("testPassword123")
                .email(generateEmail(username, uniqueSuffix))
                .displayName("Test " + normalizedUsername)
                .role(User.UserRole.valueOf(role))
                .isActive(true)
                .bio("测试用户：" + username)
                .build();

        return userService.createUser(user);
    }

    /**
     * 批量创建测试用户
     */
    public List<User> createTestUsers(int count) {
        List<User> users = new ArrayList<>();
        String[] roles = {"ADMIN", "EDITOR", "USER", "USER"};

        for (int i = 0; i < count; i++) {
            String role = roles[i % roles.length];
            User user = createTestUser("user" + (i + 1), role);
            users.add(user);
        }

        return users;
    }

    /**
     * 创建作者用户集
     */
    public Map<String, User> createAuthorSet() {
        Map<String, User> authors = new HashMap<>();

        authors.put("techAuthor", createTestUser("技术作者", "EDITOR"));
        authors.put("lifeAuthor", createTestUser("生活作者", "EDITOR"));
        authors.put("admin", createTestUser("管理员", "ADMIN"));

        return authors;
    }

    // ==================== 分类数据工厂 ====================

    /**
     * 创建测试分类
     */
    public Category createTestCategory(String name) {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String finalName = name + "_" + uniqueSuffix;
        Category category = Category.builder()
                .name(finalName)
                .description("测试分类：" + name)
                .isActive(true)
                .slug(generateSlug(finalName, "category"))
                .build();

        return categoryService.createCategory(category, null);
    }

    /**
     * 创建配置层次结构的分类
     */
    public Map<String, Category> createCategoryHierarchy() {
        Map<String, Category> categories = new HashMap<>();

        // 创建一级分类
        Category tech = createTestCategory("技术");
        Category life = createTestCategory("生活");
        Category study = createTestCategory("学习");

        categories.put("tech", tech);
        categories.put("life", life);
        categories.put("study", study);

        // 创建二级分类
        Category java = createTestCategory("Java");
        Category spring = createTestCategory("Spring");
        Category python = createTestCategory("Python");

        // 设置父分类关系
        java.setParent(tech);
        spring.setParent(tech);
        python.setParent(tech);

        java = categoryService.updateCategory(java.getId(), java, tech.getId());
        spring = categoryService.updateCategory(spring.getId(), spring, tech.getId());
        python = categoryService.updateCategory(python.getId(), python, tech.getId());

        categories.put("java", java);
        categories.put("spring", spring);
        categories.put("python", python);

        return categories;
    }

    // ==================== 标签数据工厂 ====================

    /**
     * 创建测试标签
     */
    public Tag createTestTag(String name) {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String finalName = name + "_" + uniqueSuffix;
        Tag tag = Tag.builder()
                .name(finalName)
                .slug(generateSlug(finalName, "tag"))
                .description("测试标签：" + name)
                .build();

        return tagService.createTag(tag);
    }

    /**
     * 创建常用标签集
     */
    public Map<String, Tag> createCommonTags() {
        Map<String, Tag> tags = new HashMap<>();

        String[] tagNames = {"Java", "Spring", "MySQL", "Redis", "Docker", "前端", "API", "架构"};

        for (String tagName : tagNames) {
            Tag tag = createTestTag(tagName);
            tags.put(tagName.toLowerCase(), tag);
        }

        return tags;
    }

    /**
     * 批量创建标签并指定使用次数
     */
    public List<Tag> createTagsWithUsage(Map<String, Integer> tagUsage) {
        List<Tag> tags = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : tagUsage.entrySet()) {
            Tag tag = createTestTag(entry.getKey());
            // 手动设置使用次数（通过多次创建和删除文章来模拟）
            for (int i = 0; i < entry.getValue(); i++) {
                tagService.incrementUsageCount(tag.getId());
            }
            tags.add(tag);
        }

        return tags;
    }

    // ==================== 文章数据工厂 ====================

    /**
     * 创建测试文章
     */
    public Post createTestPost(String title, User author, Category category, Set<Tag> tags) {
        Post post = Post.builder()
                .title(title)
                .content("这是" + title + "的详细内容。\n\n包含丰富的技术细节和实践经验...")
                .excerpt("这是" + title + "的摘要")
                .tags(new java.util.ArrayList<>(tags))
                .viewCount(0)
                .isFeatured(false)
                .status(Post.PostStatus.DRAFT)
                .build();

        post.setSlug(title.toLowerCase().replaceAll("\\s+", "-"));

        if (category != null) {
            post.setCategory(category);
        }

        return postService.createPost(post, author.getId());
    }

    /**
     * 创建多篇文章（模拟博客内容）
     */
    public List<Post> createBlogPosts(User author, Map<String, Category> categories, Map<String, Tag> tags, int count) {
        List<Post> posts = new ArrayList<>();

        String[] titles = {
            "深入理解Java虚拟机",
            "Spring Boot实战指南",
            "微服务架构设计",
            "数据库性能优化",
            "前端开发最佳实践",
            "分布式系统实现",
            "容器化部署方案",
            "代码重构技巧"
        };

        for (int i = 0; i < count && i < titles.length; i++) {
            String title = titles[i];
            Category category = categories.values().iterator().next();

            Set<Tag> postTags = new HashSet<>();
            postTags.add(tags.values().iterator().next());
            if (i % 2 == 0 && tags.size() > 1) {
                postTags.add(tags.values().stream().skip(1).findFirst().orElse(null));
            }

            Post post = createTestPost(title, author, category, postTags);
            posts.add(post);
        }

        return posts;
    }

    /**
     * 创建完整的博客内容场景
     */
    public BlogContent createBlogContent() {
        // 创建作者
        Map<String, User> authors = createAuthorSet();
        User mainAuthor = authors.get("techAuthor");

        // 创建分类层次结构
        Map<String, Category> categories = createCategoryHierarchy();

        // 创建标签集
        Map<String, Tag> tags = createCommonTags();

        // 创建文章
        List<Post> posts = createBlogPosts(mainAuthor, categories, tags, 6);

        // 发布部分文章
        for (int i = 0; i < 3 && i < posts.size(); i++) {
            postService.publishPost(posts.get(i).getId());
        }

        return new BlogContent(authors, categories, tags, posts);
    }

    // ==================== 媒体数据工厂 ====================

    /**
     * 创建测试媒体
     */
    public Media createTestMedia(String fileName, Media.MediaType type, User uploader) {
        Media media = Media.builder()
                .originalName("测试图片_" + fileName)
                .fileName(fileName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg")
                .fileExtension(".jpg")
                .fileSize((long) (100 + Math.random() * 900) * 1024) // 100KB-1MB
                .type(type)
                .width(1920)
                .height(1080)
                .description("测试媒体文件")
                .altText("测试图片")
                .localUrl("/uploads/" + fileName)
                .build();

        return mediaService.createMedia(media, uploader.getId());
    }

    /**
     * 创建媒体集
     */
    public Map<String, Media> createMediaSet(User uploader) {
        Map<String, Media> mediaSet = new HashMap<>();

        mediaSet.put("banner", createTestMedia("banner", Media.MediaType.BANNER, uploader));
        mediaSet.put("thumbnail", createTestMedia("thumb", Media.MediaType.THUMBNAIL, uploader));
        mediaSet.put("content1", createTestMedia("content1", Media.MediaType.IMAGE, uploader));
        mediaSet.put("content2", createTestMedia("content2", Media.MediaType.IMAGE, uploader));
        mediaSet.put("document", createTestMedia("doc", Media.MediaType.DOCUMENT, uploader));

        return mediaSet;
    }

    // ==================== 工具方法 ====================

    private String sanitizeIdentifier(String raw, String defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        String sanitized = raw.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return StringUtils.hasText(sanitized) ? sanitized : defaultValue;
    }

    private String generateEmail(String username, String suffix) {
        String prefix = sanitizeIdentifier(username, "user");
        return prefix + "_" + suffix + "@test.com";
    }

    private String generateSlug(String name, String prefix) {
        String base = name != null ? name.toLowerCase() : "";
        base = base.replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(base)) {
            base = prefix;
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ==================== 系统配置数据工厂 ====================

    /**
     * 初始化系统配置
     */
    public void initializeSystemConfiguration() {
        settingsService.initializeDefaultSiteConfig();

        // 站点配置
        settingsService.setValue("site.title", "KONATABLOG测试博客");
        settingsService.setValue("site.description", "这是一个测试博客系统");
        settingsService.setValue("site.keywords", "博客,Java,Spring Boot");
        settingsService.setValue("site.email", "test@konatablog.com");

        // SEO配置
        settingsService.setValue("seo.title", "博客测试 - SEO");
        settingsService.setValue("seo.description", "测试博客的SEO描述");

        // 社交媒体配置
        settingsService.setValue("social.github", "https://github.com/test");
        settingsService.setValue("social.twitter", "https://twitter.com/test");
    }

    /**
     * 初始化主题
     */
    public Themes initializeTheme() {
        themesService.initializeDefaultTheme();

        // 创建额外主题
        Themes darkTheme = Themes.builder()
                .name("暗色主题")
                .slug("dark-theme")
                .description("适合夜间阅读的暗色主题")
                .version("1.0.0")
                .author("Test Author")
                .themeSettings("{\"colors\":{\"primary\":\"#1a1a1a\",\"background\":\"#000000\"}}")
                .isActive(false)
                .isDefault(false)
                .build();

        return themesService.createTheme(darkTheme);
    }

    // ==================== 场景初始化器 ====================

    /**
     * 初始化完整的测试环境
     */
    public TestEnvironment initializeFullTestEnvironment() {
        log.info("开始初始化完整测试环境");

        // 1. 初始化配置和主题
        initializeSystemConfiguration();
        Themes darkTheme = initializeTheme();

        // 2. 创建用户和内容
        BlogContent blogContent = createBlogContent();

        // 3. 创建媒体
        Map<String, Media> mediaSet = createMediaSet(blogContent.getPosts().get(0).getAuthor());

        log.info("测试环境初始化完成 - 用户:{}, 分类:{}, 标签:{}, 文章:{}, 媒体:{}",
                blogContent.getAuthors().size(),
                blogContent.getCategories().size(),
                blogContent.getTags().size(),
                blogContent.getPosts().size(),
                mediaSet.size());

        return new TestEnvironment(blogContent, mediaSet, darkTheme);
    }

    // ==================== 辅助清理方法 ====================

    /**
     * 清理指定用户的所有数据
     */
    public void cleanupUserData(User user) {
        log.debug("清理用户 {} 的所有数据", user.getUsername());

        // 删除用户的文章
        List<Post> userPosts = postService.findByAuthor(user.getId(),
                org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
        for (Post post : userPosts) {
            postService.deletePost(post.getId());
        }

        // 删除用户
        userService.deleteUser(user.getId());
    }

    // ==================== 数据载体类 ====================

    /**
     * 博客内容数据载体
     */
    public static class BlogContent {
        private final Map<String, User> authors;
        private final Map<String, Category> categories;
        private final Map<String, Tag> tags;
        private final List<Post> posts;

        public BlogContent(Map<String, User> authors, Map<String, Category> categories,
                         Map<String, Tag> tags, List<Post> posts) {
            this.authors = authors;
            this.categories = categories;
            this.tags = tags;
            this.posts = posts;
        }

        public Map<String, User> getAuthors() { return authors; }
        public Map<String, Category> getCategories() { return categories; }
        public Map<String, Tag> getTags() { return tags; }
        public List<Post> getPosts() { return posts; }
    }

    /**
     * 测试环境数据载体
     */
    public static class TestEnvironment {
        private final BlogContent blogContent;
        private final Map<String, Media> mediaSet;
        private final Themes darkTheme;

        public TestEnvironment(BlogContent blogContent, Map<String, Media> mediaSet, Themes darkTheme) {
            this.blogContent = blogContent;
            this.mediaSet = mediaSet;
            this.darkTheme = darkTheme;
        }

        public BlogContent getBlogContent() { return blogContent; }
        public Map<String, Media> getMediaSet() { return mediaSet; }
        public Themes getDarkTheme() { return darkTheme; }
    }
}
