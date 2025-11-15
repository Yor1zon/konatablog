package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.Themes;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.ThemesRepository;

import java.util.List;
import java.util.Optional;

/**
 * 主题管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ThemesService {

    private final ThemesRepository themesRepository;

    // ==================== CRUD 操作 ====================

    /**
     * 根据ID查找主题
     */
    @Transactional(readOnly = true)
    public Themes findById(Long id) {
        log.debug("查找主题 ID: {}", id);
        return themesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found with id: " + id));
    }

    /**
     * 根据Slug查找主题
     */
    @Transactional(readOnly = true)
    public Themes findBySlug(String slug) {
        log.debug("查找主题 Slug: {}", slug);
        return themesRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found with slug: " + slug));
    }

    /**
     * 根据名称查找主题
     */
    @Transactional(readOnly = true)
    public Themes findByName(String name) {
        log.debug("查找主题名称: {}", name);
        return themesRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found with name: " + name));
    }

    /**
     * 查找所有主题
     */
    @Transactional(readOnly = true)
    public List<Themes> findAll() {
        log.debug("查找所有主题");
        return themesRepository.findAllOrderByActive();
    }

    /**
     * 查找活动主题
     */
    @Transactional(readOnly = true)
    public List<Themes> findActiveThemes() {
        log.debug("查找活动主题");
        return themesRepository.findActiveThemes();
    }

    /**
     * 查找当前活动主题（只有一个）
     */
    @Transactional(readOnly = true)
    public Themes findCurrentActiveTheme() {
        log.debug("查找当前活动主题");
        return themesRepository.findCurrentActiveTheme()
                .orElseThrow(() -> new ResourceNotFoundException("No active theme found"));
    }

    /**
     * 查找默认主题
     */
    @Transactional(readOnly = true)
    public Themes findDefaultTheme() {
        log.debug("查找默认主题");
        return themesRepository.findDefaultTheme()
                .orElseThrow(() -> new ResourceNotFoundException("No default theme found"));
    }

    /**
     * 查找非活动主题
     */
    @Transactional(readOnly = true)
    public List<Themes> findInactiveThemes() {
        log.debug("查找非活动主题");
        return themesRepository.findInactiveThemes();
    }

    // ==================== 主题创建和更新 ====================

    /**
     * 创建新主题
     */
    public Themes createTheme(Themes theme) {
        log.info("创建新主题: {} ({})", theme.getName(), theme.getSlug());

        // 验证输入
        validateTheme(theme);

        // 检查Slug是否已存在
        if (themesRepository.findBySlug(theme.getSlug()).isPresent()) {
            throw new DuplicateResourceException("Theme slug already exists: " + theme.getSlug());
        }

        // 检查名称是否已存在（与版本组合）
        Optional<Themes> existing = themesRepository.findByNameAndVersion(theme.getName(), theme.getVersion());
        if (existing.isPresent()) {
            throw new DuplicateResourceException("Theme with name and version already exists: " +
                theme.getName() + " v" + theme.getVersion());
        }

        // 设置默认值
        if (theme.getIsActive() == null) {
            theme.setIsActive(false);
        }
        if (theme.getIsDefault() == null) {
            theme.setIsDefault(false);
        }

        Themes savedTheme = themesRepository.save(theme);
        log.info("已创建主题 ID: {}", savedTheme.getId());

        return savedTheme;
    }

    /**
     * 更新主题
     */
    public Themes updateTheme(Long id, Themes updatedTheme) {
        log.info("更新主题 ID: {}", id);

        Themes existingTheme = findById(id);

        // 更新允许的字段
        if (StringUtils.hasText(updatedTheme.getName())) {
            existingTheme.setName(updatedTheme.getName());
        }
        if (StringUtils.hasText(updatedTheme.getDescription())) {
            existingTheme.setDescription(updatedTheme.getDescription());
        }
        if (StringUtils.hasText(updatedTheme.getVersion())) {
            existingTheme.setVersion(updatedTheme.getVersion());
        }
        if (StringUtils.hasText(updatedTheme.getAuthor())) {
            existingTheme.setAuthor(updatedTheme.getAuthor());
        }
        if (StringUtils.hasText(updatedTheme.getPreviewUrl())) {
            existingTheme.setPreviewUrl(updatedTheme.getPreviewUrl());
        }
        if (StringUtils.hasText(updatedTheme.getConfigPath())) {
            existingTheme.setConfigPath(updatedTheme.getConfigPath());
        }
        if (StringUtils.hasText(updatedTheme.getThemeSettings())) {
            existingTheme.setThemeSettings(updatedTheme.getThemeSettings());
        }

        return themesRepository.save(existingTheme);
    }

    /**
     * 删除主题
     */
    public void deleteTheme(Long id) {
        log.info("删除主题 ID: {}", id);

        Themes theme = findById(id);

        // 检查是否为活动主题
        if (theme.getIsActive()) {
            throw new IllegalStateException("Cannot delete active theme: " + theme.getName());
        }

        themesRepository.deleteById(id);
        log.info("主题 ID: {} 已删除", id);
    }

    // ==================== 主题激活和状态管理 ====================

    /**
     * 激活主题（取消其他所有主题的激活状态）
     */
    public Themes activateTheme(Long id) {
        log.info("激活主题 ID: {}", id);

        Themes theme = findById(id);

        // 取消所有现有活动主题
        List<Themes> activeThemes = themesRepository.findActiveThemes();
        for (Themes activeTheme : activeThemes) {
            activeTheme.deactivate();
            themesRepository.save(activeTheme);
        }

        // 激活新主题
        theme.setActiveTheme();
        Themes activatedTheme = themesRepository.save(theme);

        log.info("主题 [{}] 已激活", theme.getName());
        return activatedTheme;
    }

    /**
     * 激活主题（通过slug）
     */
    public Themes activateThemeBySlug(String slug) {
        log.info("通过Slug激活主题: {}", slug);

        Themes theme = findBySlug(slug);
        return activateTheme(theme.getId());
    }

    /**
     * 取消激活主题
     */
    public Themes deactivateTheme(Long id) {
        log.info("取消激活主题 ID: {}", id);

        Themes theme = findById(id);
        theme.deactivate();

        Themes deactivatedTheme = themesRepository.save(theme);
        log.info("主题 [{}] 已取消激活", theme.getName());

        return deactivatedTheme;
    }

    /**
     * 设置为默认主题（同时激活）
     */
    public Themes setDefaultTheme(Long id) {
        log.info("设置默认主题 ID: {}", id);

        return activateTheme(id);
    }

    // ==================== 主题配置管理 ====================

    /**
     * 更新主题配置
     */
    public Themes updateThemeSettings(Long id, String settings) {
        log.info("更新主题配置 ID: {}", id);

        Themes theme = findById(id);
        theme.setThemeSettings(settings);

        return themesRepository.save(theme);
    }

    /**
     * 更新主题配置项
     */
    public Themes updateThemeSetting(Long id, String key, String value) {
        log.info("更新主题配置项：{} = {} (主题ID: {})", key, value, id);

        Themes theme = findById(id);
        theme.setSetting(key, value);

        return themesRepository.save(theme);
    }

    /**
     * 设置主题预览图
     */
    public Themes updateThemePreview(Long id, String previewUrl) {
        log.info("更新主题预览图 ID: {}", id);

        Themes theme = findById(id);
        theme.setPreviewUrl(previewUrl);

        return themesRepository.save(theme);
    }

    // ==================== 主题搜索和过滤 ====================

    /**
     * 按作者查找主题
     */
    @Transactional(readOnly = true)
    public List<Themes> findByAuthor(String author) {
        log.debug("查找作者 {} 的主题", author);
        return themesRepository.findByAuthorOrderByNameAsc(author);
    }

    /**
     * 按版本排序的主题
     */
    @Transactional(readOnly = true)
    public List<Themes> findAllOrderByVersion() {
        log.debug("按版本排序查找主题");
        return themesRepository.findAllOrderByVersion();
    }

    /**
     * 查找包含指定配置的主题
     */
    @Transactional(readOnly = true)
    public List<Themes> findThemesWithSetting(String key) {
        log.debug("查找包含配置 {} 的主题", key);
        return themesRepository.findThemesWithSetting(key);
    }

    // ==================== 统计功能 ====================

    /**
     * 统计主题总数
     */
    @Transactional(readOnly = true)
    public long countAllThemes() {
        log.debug("统计主题总数");
        return themesRepository.countAllThemes();
    }

    /**
     * 统计活动主题数量
     */
    @Transactional(readOnly = true)
    public long countActiveThemes() {
        log.debug("统计活动主题数量");
        return themesRepository.countActiveThemes();
    }

    /**
     * 统计非活动主题数量
     */
    @Transactional(readOnly = true)
    public long countInactiveThemes() {
        log.debug("统计非活动主题数量");
        return countAllThemes() - countActiveThemes();
    }

    /**
     * 检查是否有活动主题
     */
    @Transactional(readOnly = true)
    public boolean hasActiveTheme() {
        log.debug("检查是否有活动主题");
        return themesRepository.hasActiveTheme();
    }

    // ==================== 初始化默认主题 ====================

    /**
     * 初始化默认主题
     */
    public void initializeDefaultTheme() {
        log.info("初始化默认主题");

        if (!themesRepository.hasActiveTheme()) {
            Themes defaultTheme = Themes.builder()
                    .name("Default")
                    .slug("default")
                    .description("KONATABLOG默认主题")
                    .version("1.0.0")
                    .author("KANA")
                    .configPath("themes/default/theme.conf")
                    .isActive(true)
                    .isDefault(true)
                    .themeSettings("{\"colors\": {\"primary\": \"#007bff\", \"secondary\": \"#6c757d\"}}")
                    .build();

            themesRepository.save(defaultTheme);
            log.info("默认主题已初始化并激活");
        } else {
            log.debug("已存在活动主题，跳过默认主题初始化");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 生成主题Slug
     */
    public String generateSlug(String name) {
        if (!StringUtils.hasText(name)) {
            return "unnamed-theme";
        }

        String sanitized = name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");

        return sanitized;
    }

    /**
     * 验证主题数据
     */
    private void validateTheme(Themes theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Theme cannot be null");
        }

        if (!StringUtils.hasText(theme.getName())) {
            throw new IllegalArgumentException("Theme name is required");
        }

        if (!StringUtils.hasText(theme.getSlug())) {
            throw new IllegalArgumentException("Theme slug is required");
        }

        if (theme.getName().length() > 100) {
            throw new IllegalArgumentException("Theme name cannot exceed 100 characters");
        }

        if (theme.getSlug().length() > 100) {
            throw new IllegalArgumentException("Theme slug cannot exceed 100 characters");
        }

        if (theme.getSlug().length() > 0 && !theme.getSlug().matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Theme slug must contain only lowercase letters, numbers, and hyphens");
        }
    }

    /**
     * 安全删除主题（检查是否在使用中）
     */
    public void safeDeleteTheme(Long id) {
        Themes theme = findById(id);

        if (theme.getIsActive()) {
            throw new IllegalStateException("Cannot delete active theme");
        }

        if (theme.getIsDefault()) {
            throw new IllegalStateException("Cannot delete default theme");
        }

        deleteTheme(id);
    }

    /**
     * 克隆主题（创建副本）
     */
    public Themes cloneTheme(Long id, String newName, String newSlug) {
        log.info("克隆主题 ID: {} -> {} ({})", id, newName, newSlug);

        Themes originalTheme = findById(id);

        Themes clonedTheme = Themes.builder()
                .name(newName)
                .slug(newSlug)
                .description("克隆自: " + originalTheme.getDescription())
                .version(originalTheme.getVersion() + "-clone")
                .author(originalTheme.getAuthor())
                .previewUrl(originalTheme.getPreviewUrl())
                .configPath(originalTheme.getConfigPath())
                .isActive(false)
                .isDefault(false)
                .themeSettings(originalTheme.getThemeSettings())
                .build();

        return createTheme(clonedTheme);
    }
}