package wiki.kana.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wiki.kana.dto.CommonResponse;
import wiki.kana.dto.settings.SettingsAvatarResponse;
import wiki.kana.dto.settings.SettingsItemResponse;
import wiki.kana.dto.settings.SettingsPublicResponse;
import wiki.kana.dto.settings.SettingsUpdateRequest;
import wiki.kana.entity.Media;
import wiki.kana.entity.Settings;
import wiki.kana.entity.Themes;
import wiki.kana.exception.FileStorageException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.FileStorageService;
import wiki.kana.service.MediaService;
import wiki.kana.service.SettingsService;
import wiki.kana.service.ThemesService;
import wiki.kana.service.UserService;
import wiki.kana.util.JwtTokenUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private static final int MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final String AVATAR_SUB_DIR = "avatars";

    private final SettingsService settingsService;
    private final ThemesService themesService;
    private final MediaService mediaService;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 公开设置
     */
    @GetMapping("/public")
    public ResponseEntity<CommonResponse<SettingsPublicResponse>> getPublicSettings() {
        return ResponseEntity.ok(CommonResponse.success(buildPublicResponse()));
    }

    /**
     * 获取所有设置（需认证）
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<SettingsItemResponse>>> listSettings(HttpServletRequest request) {
        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        List<SettingsItemResponse> responses = settingsService.findAll().stream()
                .map(this::toSettingsItemResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    /**
     * 更新设置
     */
    @PutMapping
    public ResponseEntity<CommonResponse<SettingsPublicResponse>> updateSettings(
            HttpServletRequest request,
            @Valid @RequestBody SettingsUpdateRequest updateRequest) {

        Long userId = resolveUserId(request);
        if (userId == null) {
            return unauthorizedResponse();
        }

        try {
            if (StringUtils.hasText(updateRequest.getBlogName())) {
                settingsService.setValue(Settings.ConfigKeys.SITE_TITLE, updateRequest.getBlogName(), "site", null, null, userId);
            }
            if (StringUtils.hasText(updateRequest.getBlogDescription())) {
                settingsService.setValue(Settings.ConfigKeys.SITE_DESCRIPTION, updateRequest.getBlogDescription(), "site", Settings.OptionType.TEXTAREA, null, userId);
            }
            if (StringUtils.hasText(updateRequest.getBlogTagline())) {
                settingsService.setValue(Settings.ConfigKeys.SITE_TAGLINE, updateRequest.getBlogTagline(), "site", null, null, userId);
            }
            if (StringUtils.hasText(updateRequest.getAuthorName())) {
                settingsService.setValue(Settings.ConfigKeys.AUTHOR_NAME, updateRequest.getAuthorName(), "site", null, null, userId);
            }
            if (StringUtils.hasText(updateRequest.getAuthorEmail())) {
                settingsService.setValue(Settings.ConfigKeys.SITE_EMAIL, updateRequest.getAuthorEmail(), "site", Settings.OptionType.EMAIL, null, userId);
            }
            if (updateRequest.getPageSize() != null) {
                settingsService.setValue(Settings.ConfigKeys.SITE_PAGE_SIZE, String.valueOf(updateRequest.getPageSize()), "site", Settings.OptionType.NUMBER, null, userId);
            }
            if (updateRequest.getCommentEnabled() != null) {
                settingsService.setValue(Settings.ConfigKeys.COMMENTS_ENABLED, String.valueOf(updateRequest.getCommentEnabled()), "site", Settings.OptionType.CHECKBOX, null, userId);
            }
            if (StringUtils.hasText(updateRequest.getTheme())) {
                Themes activated = themesService.activateThemeBySlug(updateRequest.getTheme());
                settingsService.setValue(Settings.ConfigKeys.THEME_CURRENT, activated.getSlug(), "theme", Settings.OptionType.TEXT, null, userId);
            }

            return ResponseEntity.ok(CommonResponse.success(buildPublicResponse(), "系统设置已更新"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<CommonResponse<SettingsAvatarResponse>> uploadAvatar(
            HttpServletRequest request,
            @RequestParam("avatar") MultipartFile avatarFile) {

        Long userId = resolveUserId(request);
        if (userId == null) {
            return unauthorizedResponse();
        }

        if (avatarFile == null || avatarFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "请选择需要上传的头像"));
        }

        if (avatarFile.getSize() > MAX_AVATAR_SIZE) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "头像大小不能超过5MB"));
        }

        try {
            FileStorageService.StoredFile storedFile = fileStorageService.store(avatarFile, AVATAR_SUB_DIR);
            Media media = Media.builder()
                    .originalName(storedFile.getOriginalFilename())
                    .fileName(storedFile.getStoredFilename())
                    .fileExtension(storedFile.getExtension())
                    .fileSize(storedFile.getSize())
                    .type(Media.MediaType.AVATAR)
                    .localUrl(storedFile.getPublicUrl())
                    .description("用户头像")
                    .build();

            mediaService.createMedia(media, userId);
            userService.updateAvatar(userId, storedFile.getPublicUrl());

            SettingsAvatarResponse response = SettingsAvatarResponse.builder()
                    .avatarUrl(storedFile.getPublicUrl())
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.success(response, "头像上传成功"));
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("STORAGE_ERROR", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        }
    }

    private SettingsPublicResponse buildPublicResponse() {
        return SettingsPublicResponse.builder()
                .blogName(settingsService.getSiteTitle())
                .blogDescription(settingsService.getSiteDescription())
                .blogTagline(settingsService.getSiteTagline())
                .authorName(settingsService.getString(Settings.ConfigKeys.AUTHOR_NAME, "博主"))
                .authorEmail(settingsService.getString(Settings.ConfigKeys.SITE_EMAIL, "contact@blog.com"))
                .pageSize(settingsService.getInteger(Settings.ConfigKeys.SITE_PAGE_SIZE, 10))
                .commentEnabled(settingsService.getBoolean(Settings.ConfigKeys.COMMENTS_ENABLED, true))
                .theme(settingsService.getCurrentTheme())
                .build();
    }

    private SettingsItemResponse toSettingsItemResponse(Settings settings) {
        return SettingsItemResponse.builder()
                .id(settings.getId())
                .key(settings.getConfigKey())
                .value(settings.getConfigValue())
                .group(settings.getConfigGroup())
                .description(settings.getDescription())
                .optionType(settings.getOptionType() != null ? settings.getOptionType().name() : null)
                .updatedBy(settings.getUpdateBy() != null ? settings.getUpdateBy().getUsername() : null)
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private Long resolveUserId(HttpServletRequest request) {
        String token = jwtTokenUtil.extractTokenFromHeader(request.getHeader("Authorization"));
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return null;
        }
        return jwtTokenUtil.getUserIdFromToken(token);
    }

    private <T> ResponseEntity<CommonResponse<T>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.error("UNAUTHORIZED", "Token无效或已过期"));
    }
}
