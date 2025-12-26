package wiki.kana.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wiki.kana.dto.CommonResponse;
import wiki.kana.dto.auth.UserProfileResponse;
import wiki.kana.dto.user.UserUpdateRequest;
import wiki.kana.entity.User;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.UserService;
import wiki.kana.util.JwtTokenUtil;

/**
 * 用户信息接口
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 更新当前用户的用户信息（用户名、邮箱、昵称、可选密码）
     */
    @PutMapping("/me")
    public ResponseEntity<CommonResponse<UserProfileResponse>> updateProfile(
            HttpServletRequest request,
            @Valid @RequestBody UserUpdateRequest updateRequest) {

        String token = jwtTokenUtil.extractTokenFromHeader(request.getHeader("Authorization"));
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return unauthorizedResponse();
        }

        // 验证至少提供一个更新字段
        if (!StringUtils.hasText(updateRequest.getUsername())
            && !StringUtils.hasText(updateRequest.getEmail())
            && !StringUtils.hasText(updateRequest.getNickname())
            && !StringUtils.hasText(updateRequest.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "至少需要提供一个要更新的字段（用户名、邮箱、昵称或密码）"));
        }

        Long userId = jwtTokenUtil.getUserIdFromToken(token);

        try {
            User updated;

            // 如果提供了密码，说明要修改密码
            if (StringUtils.hasText(updateRequest.getPassword())) {
                // 验证密码和确认密码匹配
                if (!updateRequest.getPassword().equals(updateRequest.getConfirmPassword())) {
                    return ResponseEntity.badRequest()
                            .body(CommonResponse.error("VALIDATION_ERROR", "确认密码与新密码不匹配"));
                }
            }

            // 更新基本信息（用户名、邮箱、昵称）
            if (StringUtils.hasText(updateRequest.getUsername())
                || StringUtils.hasText(updateRequest.getEmail())
                || StringUtils.hasText(updateRequest.getNickname())) {

                User updateData = User.builder()
                        .username(StringUtils.hasText(updateRequest.getUsername()) ? updateRequest.getUsername().trim() : null)
                        .email(StringUtils.hasText(updateRequest.getEmail()) ? updateRequest.getEmail().trim() : null)
                        .displayName(StringUtils.hasText(updateRequest.getNickname()) ? updateRequest.getNickname().trim() : null)
                        .build();

                updated = userService.updateUser(userId, updateData);
            } else {
                // 如果只改密码，则只查询用户不变更其他信息
                updated = userService.findById(userId);
            }

            // 如果提供了新密码，调用密码更新接口
            if (StringUtils.hasText(updateRequest.getPassword())) {
                log.debug("Updating password for user with ID: {}", userId);
                updated = userService.updatePassword(userId, updateRequest.getPassword());
            }

            UserProfileResponse response = UserProfileResponse.builder()
                    .id(updated.getId())
                    .username(updated.getUsername())
                    .email(updated.getEmail())
                    .nickname(updated.getDisplayName())
                    .role(updated.getRole() != null ? updated.getRole().name() : null)
                    .avatar(updated.getAvatarUrl())
                    .isActive(updated.getIsActive())
                    .lastLoginAt(updated.getLastLoginAt())
                    .createdAt(updated.getCreatedAt())
                    .build();

            return ResponseEntity.ok(CommonResponse.success(response, "用户信息更新成功"));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("USER_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    private <T> ResponseEntity<CommonResponse<T>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.error("UNAUTHORIZED", "Token无效或已过期"));
    }
}
