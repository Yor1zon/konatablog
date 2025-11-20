package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.User;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务层
 * 负责用户相关的业务逻辑处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== CRUD 操作 ====================

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户实体
     * @throws ResourceNotFoundException 用户不存在
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户实体
     * @throws ResourceNotFoundException 用户不存在
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱地址
     * @return 用户实体
     * @throws ResourceNotFoundException 用户不存在
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * 根据用户名或邮箱查找用户
     *
     * @param username 用户名
     * @param email    邮箱地址
     * @return 用户实体
     * @throws ResourceNotFoundException 用户不存在
     */
    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String username, String email) {
        log.debug("Finding user by username or email: {}, {}", username, email);
        return userRepository.findByUsernameOrEmail(username, email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username + " or email: " + email));
    }

    /**
     * 查找所有用户
     *
     * @return 用户列表
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        log.debug("Finding all users");
        return userRepository.findAll();
    }

    /**
     * 根据角色查找用户
     *
     * @param role 用户角色
     * @return 用户列表
     */
    @Transactional(readOnly = true)
    public List<User> findByRole(User.UserRole role) {
        log.debug("Finding users by role: {}", role);
        return userRepository.findByRole(role);
    }

    /**
     * 查找活跃用户
     *
     * @return 活跃用户列表
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        log.debug("Finding active users");
        return userRepository.findByIsActiveTrue();
    }

    // ==================== 用户创建和更新 ====================

    /**
     * 创建新用户
     *
     * @param user 用户实体（密码未加密）
     * @return 创建的用户
     * @throws DuplicateResourceException 用户名或邮箱已存在
     */
    public User createUser(User user) {
        log.info("Creating new user with username: {}", user.getUsername());

        // 验证输入
        validateUserInput(user);

        // 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists: " + user.getUsername());
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(user.getEmail()) && userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists: " + user.getEmail());
        }

        // 加密密码
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 设置默认值
        if (user.getRole() == null) {
            user.setRole(User.UserRole.ADMIN);
        }
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }

        User savedUser = userRepository.save(user);
        log.info("Successfully created user with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * 更新用户信息
     *
     * @param id       用户ID
     * @param userData 更新的用户数据
     * @return 更新后的用户
     * @throws ResourceNotFoundException 用户不存在
     * @throws DuplicateResourceException 用户名或邮箱已存在
     */
    public User updateUser(Long id, User userData) {
        log.info("Updating user with ID: {}", id);

        User existingUser = findById(id);

        // 检查用户名冲突（排除当前用户）
        if (StringUtils.hasText(userData.getUsername()) &&
            !userData.getUsername().equals(existingUser.getUsername()) &&
            userRepository.findByUsername(userData.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists: " + userData.getUsername());
        }

        // 检查邮箱冲突（排除当前用户）
        if (StringUtils.hasText(userData.getEmail()) &&
            !userData.getEmail().equals(existingUser.getEmail()) &&
            userRepository.findByEmail(userData.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists: " + userData.getEmail());
        }

        // 更新允许的字段
        if (StringUtils.hasText(userData.getUsername())) {
            existingUser.setUsername(userData.getUsername());
        }
        if (StringUtils.hasText(userData.getDisplayName())) {
            existingUser.setDisplayName(userData.getDisplayName());
        }
        if (StringUtils.hasText(userData.getEmail())) {
            existingUser.setEmail(userData.getEmail());
        }
        if (StringUtils.hasText(userData.getAvatarUrl())) {
            existingUser.setAvatarUrl(userData.getAvatarUrl());
        }
        if (StringUtils.hasText(userData.getBio())) {
            existingUser.setBio(userData.getBio());
        }
        if (userData.getRole() != null) {
            existingUser.setRole(userData.getRole());
        }
        if (userData.getIsActive() != null) {
            existingUser.setIsActive(userData.getIsActive());
        }

        User updatedUser = userRepository.save(existingUser);
        log.info("Successfully updated user with ID: {}", updatedUser.getId());

        return updatedUser;
    }

    /**
     * 更新用户密码
     *
     * @param id         用户ID
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    public User updatePassword(Long id, String newPassword) {
        log.info("Updating password for user with ID: {}", id);

        User user = findById(id);

        // 验证新密码
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        // 加密新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated password for user with ID: {}", updatedUser.getId());

        return updatedUser;
    }

    // ==================== 用户状态管理 ====================

    /**
     * 激活用户
     *
     * @param id 用户ID
     * @return 激活后的用户
     */
    public User activateUser(Long id) {
        log.info("Activating user with ID: {}", id);

        User user = findById(id);
        user.setIsActive(true);

        User activatedUser = userRepository.save(user);
        log.info("Successfully activated user with ID: {}", activatedUser.getId());

        return activatedUser;
    }

    /**
     * 禁用用户
     *
     * @param id 用户ID
     * @return 禁用后的用户
     */
    public User deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);

        User user = findById(id);
        user.setIsActive(false);

        User deactivatedUser = userRepository.save(user);
        log.info("Successfully deactivated user with ID: {}", deactivatedUser.getId());

        return deactivatedUser;
    }

    /**
     * 更新用户角色
     *
     * @param id   用户ID
     * @param role 新角色
     * @return 更新后的用户
     */
    public User updateUserRole(Long id, User.UserRole role) {
        log.info("Updating role for user with ID: {} to role: {}", id, role);

        User user = findById(id);
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated role for user with ID: {}", updatedUser.getId());

        return updatedUser;
    }

    // ==================== 认证相关 ====================

    /**
     * 用户认证
     *
     * @param username 用户名或邮箱
     * @param password 密码
     * @return 认证成功的用户
     * @throws ResourceNotFoundException 用户不存在
     */
    @Transactional(readOnly = true)
    public User authenticate(String username, String password) {
        log.debug("Authenticating user: {}", username);

        // 查找用户（支持用户名或邮箱登录）
        User user;
        if (username.contains("@")) {
            user = findByEmail(username);
        } else {
            user = findByUsername(username);
        }

        // 检查用户是否激活
        if (!user.getIsActive()) {
            throw new IllegalStateException("User account is deactivated");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        // 更新最后登录时间
        updateLastLoginTime(user.getId());

        log.info("Successfully authenticated user: {}", username);
        return user;
    }

    /**
     * 更新最后登录时间
     *
     * @param id 用户ID
     * @return 更新后的用户
     */
    public User updateLastLoginTime(Long id) {
        log.debug("Updating last login time for user: {}", id);

        User user = findById(id);
        user.updateLastLoginAt(); // 使用实体类的方法

        User updatedUser = userRepository.save(user);
        log.debug("Successfully updated last login time for user: {}", id);

        return updatedUser;
    }

    // ==================== 统计和查询 ====================

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.countAllUsers();
    }

    /**
     * 根据角色统计用户数量
     *
     * @param role 用户角色
     * @return 指定角色的用户数量
     */
    @Transactional(readOnly = true)
    public long countByRole(User.UserRole role) {
        return userRepository.countByRole(role);
    }

    /**
     * 查找最近登录的用户
     *
     * @param days 天数
     * @return 最近登录的用户列表
     */
    @Transactional(readOnly = true)
    public List<User> findRecentLoginUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        log.debug("Finding users who logged in since: {}", since);
        return userRepository.findRecentLoginUsers(since);
    }

    /**
     * 查找活跃作者（发布过博客的用户）
     *
     * @return 活跃作者列表
     */
    @Transactional(readOnly = true)
    public List<User> findActiveAuthors() {
        log.debug("Finding active authors");
        return userRepository.findActiveAuthors();
    }

    // ==================== 工具方法 ====================

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
        log.info("Successfully deleted user with ID: {}", id);
    }

    /**
     * 更新用户头像
     *
     * @param id        用户ID
     * @param avatarUrl 头像URL
     * @return 更新后的用户
     */
    public User updateAvatar(Long id, String avatarUrl) {
        log.info("Updating avatar for user ID: {}", id);
        User user = findById(id);
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return 是否存在
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * 验证用户输入
     *
     * @param user 用户实体
     */
    private void validateUserInput(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }

        if (user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        // 验证邮箱格式（如果提供）
        if (StringUtils.hasText(user.getEmail()) &&
            !user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}
