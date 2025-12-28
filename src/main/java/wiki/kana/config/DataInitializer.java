package wiki.kana.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import wiki.kana.entity.Category;
import wiki.kana.entity.User;
import wiki.kana.repository.CategoryRepository;
import wiki.kana.repository.UserRepository;

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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUser();
        initializeDefaultCategory();
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
}
