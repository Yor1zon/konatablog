package wiki.kana.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wiki.kana.dto.CommonResponse;
import wiki.kana.dto.theme.ThemeConfigRequest;
import wiki.kana.dto.theme.ThemeResponse;
import wiki.kana.entity.Themes;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.ThemesService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主题控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ThemesController {

    private final ThemesService themesService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;

    /**
     * 获取主题列表
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<ThemeResponse>>> listThemes(HttpServletRequest request) {
        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        List<ThemeResponse> responses = themesService.findAll().stream()
                .map(this::toThemeResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CommonResponse.success(responses));
    }

    /**
     * 激活主题
     */
    @PostMapping("/{themeId}/activate")
    public ResponseEntity<CommonResponse<ThemeResponse>> activateTheme(
            @PathVariable Long themeId,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            Themes activated = themesService.activateTheme(themeId);
            return ResponseEntity.ok(CommonResponse.success(toThemeResponse(activated), "主题已启用"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("THEME_NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 更新主题配置
     */
    @PutMapping("/{themeId}/config")
    public ResponseEntity<CommonResponse<ThemeResponse>> updateThemeConfig(
            @PathVariable Long themeId,
            HttpServletRequest request,
            @Valid @RequestBody ThemeConfigRequest configRequest) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            String configJson = objectMapper.writeValueAsString(configRequest.getConfig());
            Themes updated = themesService.updateThemeSettings(themeId, configJson);
            return ResponseEntity.ok(CommonResponse.success(toThemeResponse(updated), "主题配置已更新"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("THEME_NOT_FOUND", e.getMessage()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize theme config", e);
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "配置格式不正确"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    private ThemeResponse toThemeResponse(Themes theme) {
        return ThemeResponse.builder()
                .id(theme.getId())
                .name(theme.getName())
                .slug(theme.getSlug())
                .description(theme.getDescription())
                .version(theme.getVersion())
                .author(theme.getAuthor())
                .previewUrl(theme.getPreviewUrl())
                .active(theme.getIsActive())
                .isDefault(theme.getIsDefault())
                .config(parseSettings(theme.getThemeSettings()))
                .createdAt(theme.getCreatedAt())
                .updatedAt(theme.getUpdatedAt())
                .build();
    }

    private Map<String, Object> parseSettings(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse theme settings JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
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
