package wiki.kana.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import wiki.kana.entity.User;
import wiki.kana.exception.*;
import wiki.kana.service.UserService;

import static org.assertj.core.api.Assertions.*;

/**
 * 简单数据库集成测试 - 验证真实数据库持久化
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("简单数据库集成测试")
@Transactional
class SimpleDatabaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("数据库用户创建和查询")
    void databaseUserCreationAndQuery() {
        String suffix = String.valueOf(System.currentTimeMillis());
        // Given: 准备用户数据
        User user = User.builder()
                .username("db_test_user_" + suffix)
                .password("testPassword123")
                .email("db_test_" + suffix + "@example.com")
                .build();

        // When: 创建用户
        User createdUser = userService.createUser(user);

        // Then: 验证用户被持久化到数据库
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();

        // 查询用户验证数据库持久化
        User foundUser = userService.findById(createdUser.getId());
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(user.getUsername());

        // 验证用户统计功能正常
        long userCount = userService.countAllUsers();
        assertThat(userCount).isGreaterThan(0);

        System.out.println("=== 数据库集成测试验证成功 ===");
        System.out.println("创建用户ID: " + createdUser.getId());
        System.out.println("用户总数: " + userCount);

        // 清理：删除测试用户
        userService.deleteUser(createdUser.getId());
        assertThatThrownBy(() -> userService.findById(createdUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        System.out.println("测试用户已删除，数据库清理完成");
    }

    @Test
    @DisplayName("数据库事务验证")
    void databaseTransactionValidation() {
        String suffix = String.valueOf(System.currentTimeMillis());
        // Given: 创建用户
        User user = User.builder()
                .username("tx_test_user_" + suffix)
                .password("testPassword123")
                .email("transaction_" + suffix + "@example.com")
                .build();

        // When: 创建用户
        User createdUser = userService.createUser(user);

        try {
            // Then: 验证用户已存在于数据库
            User foundUser = userService.findById(createdUser.getId());
            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getUsername()).isEqualTo(user.getUsername());

            // 测试用户认证功能
            User authenticatedUser = userService.authenticate(user.getUsername(), "testPassword123");
            assertThat(authenticatedUser).isNotNull();
            assertThat(authenticatedUser.getLastLoginAt()).isNotNull();

            System.out.println("=== 事务验证测试成功 ===");
            System.out.println("用户认证成功，最后登录时间: " + authenticatedUser.getLastLoginAt());

        } finally {
            // 清理测试数据
            userService.deleteUser(createdUser.getId());
        }
    }
}
