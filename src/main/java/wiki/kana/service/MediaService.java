package wiki.kana.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import wiki.kana.entity.Media;
import wiki.kana.entity.User;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.MediaRepository;
import wiki.kana.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 媒体文件管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    // ==================== CRUD 操作 ====================

    /**
     * 根据ID查找媒体
     */
    @Transactional(readOnly = true)
    public Media findById(Long id) {
        log.debug("查找媒体 ID: {}", id);
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + id));
    }

    /**
     * 根据文件名查找媒体
     */
    @Transactional(readOnly = true)
    public Media findByFileName(String fileName) {
        log.debug("查找媒体文件名: {}", fileName);
        return mediaRepository.findByFileName(fileName)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with filename: " + fileName));
    }

    /**
     * 查找所有媒体
     */
    @Transactional(readOnly = true)
    public List<Media> findAll() {
        log.debug("查询全部媒体文件");
        return mediaRepository.findAll();
    }

    /**
     * 根据媒体类型查找
     */
    @Transactional(readOnly = true)
    public List<Media> findByType(Media.MediaType type) {
        log.debug("查找类型为 {} 的媒体", type);
        return mediaRepository.findByType(type);
    }

    /**
     * 根据上传者查找媒体
     */
    @Transactional(readOnly = true)
    public List<Media> findByUploadedBy(Long userId) {
        log.debug("查找用户 {} 的媒体", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mediaRepository.findByUploadedById(user.getId());
    }

    // ==================== 媒体上传和管理 ====================

    /**
     * 创建新媒体记录（上传后调用）
     */
    public Media createMedia(Media media, Long uploadedById) {
        log.info("创建媒体记录，上传者ID: {}", uploadedById);

        // 验证上传者
        User uploadedBy = userRepository.findById(uploadedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + uploadedById));

        // 检查文件名是否已存在
        if (mediaRepository.findByFileName(media.getFileName()).isPresent()) {
            throw new DuplicateResourceException("Media file already exists: " + media.getFileName());
        }

        // 设置上传者
        media.setUploadedBy(uploadedBy);

        // 验证必要字段
        validateMedia(media);

        Media savedMedia = mediaRepository.save(media);
        log.info("已创建媒体记录 ID: {}", savedMedia.getId());

        return savedMedia;
    }

    /**
     * 更新媒体信息
     */
    public Media updateMedia(Long id, Media updatedMedia) {
        log.info("更新媒体 ID: {}", id);

        Media existingMedia = findById(id);

        // 更新允许的字段
        if (StringUtils.hasText(updatedMedia.getOriginalName())) {
            existingMedia.setOriginalName(updatedMedia.getOriginalName());
        }
        if (StringUtils.hasText(updatedMedia.getDescription())) {
            existingMedia.setDescription(updatedMedia.getDescription());
        }
        if (StringUtils.hasText(updatedMedia.getAltText())) {
            existingMedia.setAltText(updatedMedia.getAltText());
        }
        if (StringUtils.hasText(updatedMedia.getCdnUrl())) {
            existingMedia.setCdnUrl(updatedMedia.getCdnUrl());
        }
        if (StringUtils.hasText(updatedMedia.getLocalUrl())) {
            existingMedia.setLocalUrl(updatedMedia.getLocalUrl());
        }
        if (updatedMedia.getWidth() != null) {
            existingMedia.setWidth(updatedMedia.getWidth());
        }
        if (updatedMedia.getHeight() != null) {
            existingMedia.setHeight(updatedMedia.getHeight());
        }

        return mediaRepository.save(existingMedia);
    }

    /**
     * 删除媒体
     */
    public void deleteMedia(Long id) {
        log.info("删除媒体 ID: {}", id);

        if (!mediaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Media not found with id: " + id);
        }

        Media media = findById(id);

        // 在实际应用中，这里应该删除物理文件
        // deletePhysicalFile(media.getFileName());

        mediaRepository.deleteById(id);
        log.info("媒体 ID: {} 已删除", id);
    }

    // ==================== 图片处理 ====================

    /**
     * 创建图片媒体并设置尺寸
     */
    public Media createImageMedia(String originalName, String fileName, Long fileSize,
                                  int width, int height, Long uploadedById) {
        log.info("创建图片媒体: {} => {}", originalName, fileName);

        Media media = Media.builder()
                .originalName(originalName)
                .fileName(fileName)
                .fileSize(fileSize)
                .width(width)
                .height(height)
                .type(Media.MediaType.IMAGE)
                .build();

        return createMedia(media, uploadedById);
    }

    /**
     * 创建缩略图
     */
    public Media createThumbnail(Media originalMedia, String thumbnailFileName, int width, int height) {
        log.info("创建缩略图: {} -> {}", originalMedia.getFileName(), thumbnailFileName);

        Media thumbnail = Media.builder()
                .originalName(originalMedia.getOriginalName() + "_thumb")
                .fileName(thumbnailFileName)
                .fileExtension(originalMedia.getFileExtension())
                .fileSize(calculateThumbnailSize(originalMedia.getFileSize()))
                .width(width)
                .height(height)
                .type(Media.MediaType.THUMBNAIL)
                .uploadedBy(originalMedia.getUploadedBy())
                .description("Thumbnail of " + originalMedia.getDescription())
                .build();

        return mediaRepository.save(thumbnail);
    }

    // ==================== 搜索和过滤 ====================

    /**
     * 获取最近的媒体文件
     */
    @Transactional(readOnly = true)
    public List<Media> getRecentMedia(int limit) {
        log.debug("获取最近的媒体文件，限制: {}", limit);
        return mediaRepository.findRecentMedia(Math.max(1, limit));
    }

    /**
     * 获取所有图片
     */
    @Transactional(readOnly = true)
    public List<Media> getAllImages() {
        log.debug("获取所有图片");
        return mediaRepository.findByType(Media.MediaType.IMAGE);
    }

    /**
     * 获取所有文档
     */
    @Transactional(readOnly = true)
    public List<Media> getAllDocuments() {
        log.debug("获取所有文档");
        return mediaRepository.findByType(Media.MediaType.DOCUMENT);
    }

    // ==================== 统计功能 ====================

    /**
     * 统计媒体总数
     */
    @Transactional(readOnly = true)
    public long countAllMedia() {
        log.debug("统计媒体总数");
        return mediaRepository.count();
    }

    /**
     * 统计指定类型的媒体数量
     */
    @Transactional(readOnly = true)
    public long countByType(Media.MediaType type) {
        log.debug("统计类型 {} 的媒体数量", type);
        return mediaRepository.countByType(type);
    }

    /**
     * 统计用户的媒体数量
     */
    @Transactional(readOnly = true)
    public long countByUser(Long userId) {
        log.debug("统计用户 {} 的媒体数量", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mediaRepository.findByUploadedById(user.getId()).size();
    }

    // ==================== 工具方法 ====================

    /**
     * 生成唯一文件名
     */
    public String generateUniqueFileName(String originalName, String extension) {
        String baseName = StringUtils.hasText(originalName) ? originalName : "file";
        String sanitizedName = baseName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");

        if (sanitizedName.length() > 50) {
            sanitizedName = sanitizedName.substring(0, 50);
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = sanitizedName + "_" + uuid;

        if (StringUtils.hasText(extension)) {
            return fileName + (extension.startsWith(".") ? extension : "." + extension);
        }

        return fileName;
    }

    /**
     * 验证媒体文件
     */
    private void validateMedia(Media media) {
        if (media == null) {
            throw new IllegalArgumentException("Media cannot be null");
        }

        if (!StringUtils.hasText(media.getOriginalName())) {
            throw new IllegalArgumentException("Original name is required");
        }

        if (!StringUtils.hasText(media.getFileName())) {
            throw new IllegalArgumentException("File name is required");
        }

        if (media.getFileSize() == null || media.getFileSize() <= 0) {
            throw new IllegalArgumentException("File size must be greater than 0");
        }

        if (media.getType() == null) {
            throw new IllegalArgumentException("Media type is required");
        }

        // 验证文件大小限制 (10MB)
        if (media.getFileSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }

    /**
     * 计算缩略图大小（约原始文件的1/10）
     */
    private Long calculateThumbnailSize(Long originalSize) {
        return originalSize / 10;
    }

    /**
     * 判断文件是否为图片类型（根据扩展名）
     */
    public boolean isImageFile(String extension) {
        if (!StringUtils.hasText(extension)) {
            return false;
        }

        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
               ext.equals("gif") || ext.equals("bmp") || ext.equals("webp");
    }

    /**
     * 根据文件扩展名确定媒体类型
     */
    public Media.MediaType determineMediaType(String extension) {
        if (!StringUtils.hasText(extension)) {
            return Media.MediaType.DOCUMENT;
        }

        String ext = extension.toLowerCase();

        if (isImageFile(ext)) {
            return Media.MediaType.IMAGE;
        } else if (ext.equals("mp4") || ext.equals("avi") || ext.equals("mov")) {
            return Media.MediaType.VIDEO;
        } else if (ext.equals("mp3") || ext.equals("wav") || ext.equals("flac")) {
            return Media.MediaType.AUDIO;
        } else {
            return Media.MediaType.DOCUMENT;
        }
    }
}