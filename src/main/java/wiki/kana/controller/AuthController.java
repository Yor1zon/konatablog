package wiki.kana.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wiki.kana.dto.CommonResponse;
import wiki.kana.dto.auth.LoginRequest;
import wiki.kana.dto.auth.LoginResponse;
import wiki.kana.dto.auth.UserProfileResponse;
import wiki.kana.entity.User;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.UserService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Date;

/**
 * 认证控制器
 * 负责用户登录、登出、获取用户信息等认证相关功能
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600) // 允许跨域，生产环境建议配置具体域名
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录结果包含JWT Token
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("User login attempt: {}", loginRequest.getUsername());

        try {
            // 认证用户
            User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

            // 生成JWT Token
            String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

            // 构建用户信息响应
            LoginResponse.UserDto userDto = LoginResponse.UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getDisplayName())
                    .role(user.getRole().name())
                    .avatar(user.getAvatarUrl())
                    .isActive(user.getIsActive())
                    .build();

            LoginResponse loginResponse = LoginResponse.builder()
                    .token(token)
                    .user(userDto)
                    .build();

            log.info("User logged in successfully: {}", user.getUsername());
            return ResponseEntity.ok(CommonResponse.success(loginResponse, "登录成功"));

        } catch (IllegalArgumentException e) {
            log.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("INVALID_CREDENTIALS", "用户名或密码错误"));
        } catch (ResourceNotFoundException e) {
            log.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("INVALID_CREDENTIALS", "用户名或密码错误"));
        } catch (IllegalStateException e) {
            log.warn("Inactive user attempted login: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CommonResponse.error("USER_INACTIVE", "用户账户已被禁用"));
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param request HTTP请求
     * @return 用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<CommonResponse<UserProfileResponse>> getProfile(HttpServletRequest request) {
        // 从请求头中获取Token
        String authHeader = request.getHeader("Authorization");
        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);

        if (token == null || !jwtTokenUtil.validateToken(token)) {
            log.warn("Invalid or missing token in profile request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("INVALID_TOKEN", "Token无效或已过期"));
        }

        try {
            // 从Token中获取用户信息
            String username = jwtTokenUtil.getUsernameFromToken(token);
            Long userId = jwtTokenUtil.getUserIdFromToken(token);

            // 从数据库获取最新用户信息
            User user = userService.findById(userId);

            // 构建用户信息响应
            UserProfileResponse profileResponse = UserProfileResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getDisplayName())
                    .role(user.getRole().name())
                    .avatar(user.getAvatarUrl())
                    .isActive(user.getIsActive())
                    .lastLoginAt(user.getLastLoginAt())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(CommonResponse.success(profileResponse));

        } catch (ResourceNotFoundException e) {
            log.warn("User not found in token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("USER_NOT_FOUND", "用户不存在"));
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }

    /**
     * 用户登出
     * 注意：JWT是无状态的，实际登出需要在客户端删除Token
     * 这里记录登出日志，实现Token黑名单功能可在此扩展
     *
     * @param request HTTP请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);

        if (token != null) {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            log.info("User logged out: {}", username);

            // TODO: 可以在这里实现Token黑名单功能
            // - 将Token加入Redis黑名单
            // - 设置Token过期时间等
        }

        return ResponseEntity.ok(CommonResponse.success("登出成功"));
    }

    /**
     * 验证Token有效性
     *
     * @param request HTTP请求
     * @return 验证结果
     */
    @GetMapping("/validate")
    public ResponseEntity<CommonResponse<Boolean>> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);

        if (token == null) {
            return ResponseEntity.ok(CommonResponse.success(false, "Token不存在"));
        }

        boolean isValid = jwtTokenUtil.validateToken(token);
        if (!isValid) {
            return ResponseEntity.ok(CommonResponse.success(false, "Token无效或已过期"));
        }

        // 检查用户是否仍然活跃
        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            User user = userService.findByUsername(username);

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return ResponseEntity.ok(CommonResponse.success(false, "用户账户已被禁用"));
            }

            return ResponseEntity.ok(CommonResponse.success(true, "Token有效"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(CommonResponse.success(false, "用户不存在"));
        }
    }

    /**
     * 刷新Token
     * 当旧Token即将过期时，可以获取新的Token
     *
     * @param request HTTP请求
     * @return 新的Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<String>> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);

        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("INVALID_TOKEN", "Token无效或已过期"));
        }

        try {
            // 检查Token是否即将过期（例如：剩余时间少于1小时）
            Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);
            long timeUntilExpiration = expirationDate.getTime() - System.currentTimeMillis();
            long oneHourInMillis = 60 * 60 * 1000;

            if (timeUntilExpiration > oneHourInMillis) {
                // Token仍然有效且过期时间较长，不需要刷新
                return ResponseEntity.ok(CommonResponse.success(token, "Token仍然有效"));
            }

            // 生成新的Token
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            String role = jwtTokenUtil.getRoleFromToken(token);

            User user = userService.findById(userId);
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(CommonResponse.error("USER_INACTIVE", "用户账户已被禁用"));
            }

            String newToken = jwtTokenUtil.generateToken(userId, username, role);

            log.info("Token refreshed for user: {}", username);
            return ResponseEntity.ok(CommonResponse.success(newToken, "Token刷新成功"));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("USER_NOT_FOUND", "用户不存在"));
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("INTERNAL_ERROR", "服务器内部错误"));
        }
    }
}
