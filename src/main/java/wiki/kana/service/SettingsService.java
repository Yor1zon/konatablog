package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.Settings;
import wiki.kana.entity.User;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.SettingsRepository;
import wiki.kana.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统配置管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;

    // ==================== 基础CRUD操作 ====================

    /**
     * 根据ID查找设置
     */
    @Transactional(readOnly = true)
    public Settings findById(Long id) {
        log.debug("查找设置 ID: {}", id);
        return settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found with id: " + id));
    }

    /**
     * 根据配置键查找设置
     */
    @Transactional(readOnly = true)
    public Settings findByKey(String configKey) {
        log.debug("查找配置键: {}", configKey);
        return settingsRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found with key: " + configKey));
    }

    /**
     * 根据配置组查找设置
     */
    @Transactional(readOnly = true)
    public List<Settings> findByGroup(String configGroup) {
        log.debug("查找配置组: {}", configGroup);
        return settingsRepository.findByConfigGroupOrderByConfigKey(configGroup);
    }

    /**
     * 查找所有设置（按分组排序）
     */
    @Transactional(readOnly = true)
    public List<Settings> findAll() {
        log.debug("查找所有设置");
        return settingsRepository.findAllGrouped();
    }

    // ==================== 配置值获取 ====================

    /**
     * 获取配置值（字符串类型）
     */
    @Transactional(readOnly = true)
    public String getString(String configKey) {
        return getString(configKey, null);
    }

    /**
     * 获取配置值（字符串类型，带默认值）
     */
    @Transactional(readOnly = true)
    public String getString(String configKey, String defaultValue) {
        try {
            Optional<Settings> settings = settingsRepository.findByConfigKey(configKey);
            return settings.map(Settings::getConfigValue).filter(StringUtils::hasText).orElse(defaultValue);
        } catch (Exception e) {
            log.warn("获取配置值失败: {}，使用默认值: {}", configKey, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取配置值（整数类型）
     */
    @Transactional(readOnly = true)
    public Integer getInteger(String configKey) {
        return getInteger(configKey, null);
    }

    /**
     * 获取配置值（整数类型，带默认值）
     */
    @Transactional(readOnly = true)
    public Integer getInteger(String configKey, Integer defaultValue) {
        try {
            Optional<Settings> settings = settingsRepository.findByConfigKey(configKey);
            return settings.map(Settings::getValueAsInteger).orElse(defaultValue);
        } catch (Exception e) {
            log.warn("获取整数配置值失败: {}，使用默认值: {}", configKey, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取配置值（布尔类型）
     */
    @Transactional(readOnly = true)
    public Boolean getBoolean(String configKey) {
        return getBoolean(configKey, null);
    }

    /**
     * 获取配置值（布尔类型，带默认值）
     */
    @Transactional(readOnly = true)
    public Boolean getBoolean(String configKey, Boolean defaultValue) {
        try {
            Optional<Settings> settings = settingsRepository.findByConfigKey(configKey);
            return settings.map(Settings::getValueAsBoolean).orElse(defaultValue);
        } catch (Exception e) {
            log.warn("获取布尔配置值失败: {}，使用默认值: {}", configKey, defaultValue);
            return defaultValue;
        }
    }

    // ==================== 配置值设置 ====================

    /**
     * 设置配置值
     */
    public Settings setValue(String configKey, String configValue) {
        return setValue(configKey, configValue, null, null, null, null);
    }

    /**
     * 设置配置值（带指定字段）
     */
    public Settings setValue(String configKey, String configValue, String configGroup,
                           Settings.OptionType optionType, String description, Long updatedById) {
        log.info("设置配置: {} = {}", configKey, configValue);

        // 检查配置是否存在
        Optional<Settings> existingOpt = settingsRepository.findByConfigKey(configKey);

        Settings settings;
        if (existingOpt.isPresent()) {
            settings = existingOpt.get();
            settings.setConfigValue(configValue);

            if (StringUtils.hasText(configGroup)) {
                settings.setConfigGroup(configGroup);
            }
            if (optionType != null) {
                settings.setOptionType(optionType);
            }
            if (StringUtils.hasText(description)) {
                settings.setDescription(description);
            }
        } else {
            settings = Settings.builder()
                    .configKey(configKey)
                    .configValue(configValue)
                    .configGroup(StringUtils.hasText(configGroup) ? configGroup : "site")
                    .optionType(optionType != null ? optionType : Settings.OptionType.TEXT)
                    .description(description)
                    .build();
        }

        // 设置更新者
        if (updatedById != null) {
            User updateUser = userRepository.findById(updatedById)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + updatedById));
            settings.setUpdateBy(updateUser);
        }

        return settingsRepository.save(settings);
    }

    /**
     * 批量设置配置
     */
    public List<Settings> setValues(Map<String, String> configMap, Long updatedById) {
        log.info("批量设置配置，数量: {}", configMap.size());

        return configMap.entrySet().stream()
                .map(entry -> setValue(entry.getKey(), entry.getValue(), null, null, null, updatedById))
                .collect(Collectors.toList());
    }

    // ==================== 配置删除 ====================

    /**
     * 删除配置
     */
    public void deleteConfig(String configKey) {
        log.info("删除配置: {}", configKey);

        if (!settingsRepository.existsByConfigKey(configKey)) {
            throw new ResourceNotFoundException("Settings not found with key: " + configKey);
        }

        Settings settings = findByKey(configKey);
        settingsRepository.delete(settings);
        log.info("配置 {} 已删除", configKey);
    }

    /**
     * 清空配置组
     */
    public void clearConfigGroup(String configGroup) {
        log.info("清空配置组: {}", configGroup);

        List<Settings> settingsList = settingsRepository.findByConfigGroupOrderByConfigKey(configGroup);
        settingsRepository.deleteAll(settingsList);

        log.info("配置组 {} 已清空，删除了 {} 个配置", configGroup, settingsList.size());
    }

    // ==================== 分组管理 ====================

    /**
     * 获取所有配置组
     */
    @Transactional(readOnly = true)
    public List<String> getAllConfigGroups() {
        log.debug("获取所有配置组");
        return settingsRepository.findAllConfigGroups();
    }

    /**
     * 获取站点配置
     */
    @Transactional(readOnly = true)
    public List<Settings> getSiteSettings() {
        log.debug("获取站点配置");
        return settingsRepository.findSiteSettings();
    }

    /**
     * 获取SEO配置
     */
    @Transactional(readOnly = true)
    public List<Settings> getSeoSettings() {
        log.debug("获取SEO配置");
        return settingsRepository.findSeoSettings();
    }

    /**
     * 获取社交媒体配置
     */
    @Transactional(readOnly = true)
    public List<Settings> getSocialSettings() {
        log.debug("获取社交媒体配置");
        return settingsRepository.findSocialSettings();
    }

    /**
     * 获取主题配置
     */
    @Transactional(readOnly = true)
    public List<Settings> getThemeSettings() {
        log.debug("获取主题配置");
        return settingsRepository.findThemeSettings();
    }

    /**
     * 获取常用配置
     */
    @Transactional(readOnly = true)
    public List<Settings> getCommonSettings() {
        log.debug("获取常用配置");
        return settingsRepository.findCommonSettings();
    }

    // ==================== 统计功能 ====================

    /**
     * 统计配置总数
     */
    @Transactional(readOnly = true)
    public long countAllSettings() {
        log.debug("统计配置总数");
        return settingsRepository.count();
    }

    /**
     * 统计指定配置组的数量
     */
    @Transactional(readOnly = true)
    public long countByGroup(String configGroup) {
        log.debug("统计配置组 {} 的配置数量", configGroup);
        return settingsRepository.countByConfigGroup(configGroup);
    }

    // ==================== 常用配置获取方法 ====================

    /**
     * 获取网站标题
     */
    @Transactional(readOnly = true)
    public String getSiteTitle() {
        return getString(Settings.ConfigKeys.SITE_TITLE, "KONATABLOG");
    }

    /**
     * 获取网站副标题
     */
    @Transactional(readOnly = true)
    public String getSiteTagline() {
        return getString(Settings.ConfigKeys.SITE_TAGLINE, "个人博客系统");
    }

    /**
     * 获取网站描述
     */
    @Transactional(readOnly = true)
    public String getSiteDescription() {
        return getString(Settings.ConfigKeys.SITE_DESCRIPTION, "基于Spring Boot的个人博客系统");
    }

    /**
     * 获取网站关键词
     */
    @Transactional(readOnly = true)
    public String getSiteKeywords() {
        return getString(Settings.ConfigKeys.SITE_KEYWORD, "博客,Spring Boot,Java");
    }

    /**
     * 获取网站邮箱
     */
    @Transactional(readOnly = true)
    public String getSiteEmail() {
        return getString(Settings.ConfigKeys.SITE_EMAIL);
    }

    /**
     * 获取当前主题
     */
    @Transactional(readOnly = true)
    public String getCurrentTheme() {
        return getString(Settings.ConfigKeys.THEME_CURRENT, "default");
    }

    // ==================== 初始化默认配置 ====================

    /**
     * 初始化默认站点配置
     */
    public void initializeDefaultSiteConfig() {
        log.info("初始化默认站点配置");

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.SITE_TITLE)) {
            setValue(Settings.ConfigKeys.SITE_TITLE, "KONATABLOG", "site", Settings.OptionType.TEXT, "网站标题", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.SITE_TAGLINE)) {
            setValue(Settings.ConfigKeys.SITE_TAGLINE, "个人博客系统", "site", Settings.OptionType.TEXT, "网站副标题", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.SITE_DESCRIPTION)) {
            setValue(Settings.ConfigKeys.SITE_DESCRIPTION, "基于Spring Boot的个人博客系统", "site", Settings.OptionType.TEXTAREA, "网站描述", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.SITE_KEYWORD)) {
            setValue(Settings.ConfigKeys.SITE_KEYWORD, "博客,Spring Boot,Java", "site", Settings.OptionType.TEXT, "SEO关键词", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.THEME_CURRENT)) {
            setValue(Settings.ConfigKeys.THEME_CURRENT, "default", "theme", Settings.OptionType.TEXT, "当前主题", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.AUTHOR_NAME)) {
            setValue(Settings.ConfigKeys.AUTHOR_NAME, "博主", "site", Settings.OptionType.TEXT, "作者名称", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.SITE_PAGE_SIZE)) {
            setValue(Settings.ConfigKeys.SITE_PAGE_SIZE, "10", "site", Settings.OptionType.NUMBER, "分页大小", null);
        }

        if (!settingsRepository.existsByConfigKey(Settings.ConfigKeys.COMMENTS_ENABLED)) {
            setValue(Settings.ConfigKeys.COMMENTS_ENABLED, "true", "site", Settings.OptionType.CHECKBOX, "是否启用评论", null);
        }

        log.info("默认站点配置初始化完成");
    }

    // ==================== 工具方法 ====================

    /**
     * 验证配置键的有效性
     */
    private void validateConfigKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }

        if (configKey.length() > 100) {
            throw new IllegalArgumentException("Configuration key cannot exceed 100 characters");
        }

        if (!configKey.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")) {
            throw new IllegalArgumentException("Configuration key format is invalid (use dot.notation)");
        }
    }

    /**
     * 检查配置是否为系统配置（不能随意删除）
     */
    private boolean isSystemConfig(String configKey) {
        return configKey.startsWith("system.") || configKey.equals("theme.current");
    }

    /**
     * 安全删除配置（检查是否为系统配置）
     */
    public void safeDeleteConfig(String configKey) {
        if (isSystemConfig(configKey)) {
            throw new IllegalArgumentException("Cannot delete system configuration: " + configKey);
        }
        deleteConfig(configKey);
    }
}
