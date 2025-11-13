package wiki.kana.service;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import wiki.kana.entity.User;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 测试UserService的所有主要功能
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    private LocalDateTime beforeTest;
    private User testUser;

    private String generateUniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void setUp() {
        beforeTest = LocalDateTime.now();
        // 清理所有用户数据
        userRepository.deleteAll();
        userRepository.flush();
        log.info("测试环境初始化完成，生成随机ID: {}", generateUniqueId());
    }

    @AfterEach
    void tearDown() {
        try {
            // 清理测试用户
            if (testUser != null && testUser.getId() != null) {
                try {
                    userService.deleteUser(testUser.getId());
                } catch (Exception e) {
                    log.debug("删除测试用户时出错: {}", e.getMessage());
                }
            }

            // 清理所有测试数据
            userRepository.deleteAll();
            userRepository.flush();
            log.info("测试数据清理完成，测试用例结束");
        } catch (Exception e) {
            log.warn("测试数据清理失败: {}", e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试1：创建用户")
    void testCreateUser() {
        String uniqueId = generateUniqueId();

        User user = User.builder()
                .username("testuser_" + uniqueId)
                .password("password123")
                .displayName("测试用户_" + uniqueId)
                .email("testuser_" + uniqueId + "@example.com")
                .role(User.UserRole.USER)
                .build();

        User created = userService.createUser(user);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getUsername()).isEqualTo("testuser_" + uniqueId);
        assertThat(created.getDisplayName()).isEqualTo("测试用户_" + uniqueId);
        assertThat(created.getRole()).isEqualTo(User.UserRole.USER);
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getCreatedAt()).isAfterOrEqualTo(beforeTest);

        // 验证密码已加密
        assertFalse(created.getPassword().equals("password123"));

        // 验证密码可以使用PasswordEncoder匹配
        boolean matches = passwordEncoder.matches("password123", created.getPassword());
        assertTrue(matches);

        log.debug("成功创建用户: {} ({}) - 密码已加密: {}", created.getUsername(), created.getDisplayName(), matches);

        testUser = created;
    }

    @Test
    @Order(2)
    @DisplayName("测试2：重复用户名异常")
    void testDuplicateUsername() throws Exception {
        String uniqueId = generateUniqueId();

        User user1 = User.builder()
                .username("duplicate_test_" + uniqueId)
                .password("password123")
                .email("user1@test.com")
                .build();

        User user2 = User.builder()
                .username("duplicate_test_" + uniqueId)
                .email("test012@retry.cp.com")
                .password("password456")
                .build();

        user1 = userService.createUser(user1);

        // 验证：应该抛出重复用户名异常
        DuplicateResourceException e = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(user2)
        );

        assertTrue(e.getMessage().contains("用户名已存在")
                || e.getMessage().contains("already exists")
        );

        log.debug("成功捕获重复用户名异常: {}", e.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("测试3：重复邮箱异常")
    void testDuplicateEmail() throws Exception {
        String uniqueId = generateUniqueId();

        User user1 = User.builder()
                .username("email_test_" + uniqueId)
                .password("password123")
                .email("duplicate_email@" + uniqueId + ".com")
                .build();

        User user2 = User.builder()
                .username("user002")
                .password("password456")
                .email("duplicate_email@" + uniqueId + ".com")
                .build();

        user1 = userService.createUser(user1);

        // 验证：应该抛出重复邮箱异常
        DuplicateResourceException e = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(user2)
        );

        // 验证异常消息
        assertTrue(e.getMessage().contains("邮箱")
                || e.getMessage().contains("email")
        );

        log.debug("成功捕获重复邮箱异常: {}", e.getMessage());
    }
}