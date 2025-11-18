package wiki.kana.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import wiki.kana.entity.User;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUser();
    }

    /**
     * 创建默认管理员用户
     */
    private void initializeDefaultUser() {
        try {
            // 检查是否已存在ADMIN角色的用户
            if (userRepository.findByRole(User.UserRole.ADMIN).isEmpty()) {
                User adminUser = User.builder()
                        .username("admin")
                        .email("admin@blog.com")
                        .password(passwordEncoder.encode("admin123"))
                        .displayName("konatabloger")
                        .role(User.UserRole.ADMIN)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                userRepository.save(adminUser);
                log.info("✅ 默认管理员用户创建成功: admin/admin123");
            } else {
                log.info("ℹ️ 管理员用户已存在，跳过创建");
            }
        } catch (Exception e) {
            log.error("❌ 创建默认用户失败", e);
        }
    }
}