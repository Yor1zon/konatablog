package wiki.kana.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wiki.kana.dto.CommonResponse;
import wiki.kana.dto.media.MediaResponse;
import wiki.kana.entity.Media;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.FileStorageException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.service.FileStorageService;
import wiki.kana.service.MediaService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 媒体文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class MediaController {

    private static final long MAX_UPLOAD_SIZE = 5L * 1024 * 1024;
    private static final String DEFAULT_SUB_DIR = "media";

    private final MediaService mediaService;
    private final FileStorageService fileStorageService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 获取媒体列表
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<MediaResponse>>> listMedia(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long uploadedBy) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        Pageable pageable = buildPageable(page, size, sort);

        List<Media> mediaList;
        try {
            mediaList = mediaService.findAll();
            if (StringUtils.hasText(type)) {
                Media.MediaType filterType = Media.MediaType.valueOf(type.toUpperCase(Locale.ENGLISH));
                mediaList = mediaList.stream()
                        .filter(media -> media.getType() == filterType)
                        .collect(Collectors.toList());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "不支持的媒体类型"));
        }

        if (uploadedBy != null) {
            mediaList = mediaList.stream()
                    .filter(media -> media.getUploadedBy() != null && uploadedBy.equals(media.getUploadedBy().getId()))
                    .collect(Collectors.toList());
        }

        Page<Media> pageData = buildPageFromList(mediaList, pageable);
        return ResponseEntity.ok(CommonResponse.success(pageData.map(this::toMediaResponse)));
    }

    /**
     * 上传媒体文件
     */
    @PostMapping("/upload")
    public ResponseEntity<CommonResponse<MediaResponse>> uploadMedia(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "type", required = false) String type) {

        Long userId = resolveUserId(request);
        if (userId == null) {
            return unauthorizedResponse();
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "请选择需要上传的文件"));
        }

        if (file.getSize() > MAX_UPLOAD_SIZE) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", "文件大小不能超过5MB"));
        }

        try {
            Media.MediaType mediaType = resolveMediaType(type, file);
            FileStorageService.StoredFile storedFile = fileStorageService.store(file, DEFAULT_SUB_DIR);

            Media media = Media.builder()
                    .originalName(storedFile.getOriginalFilename())
                    .fileName(storedFile.getStoredFilename())
                    .fileExtension(storedFile.getExtension())
                    .fileSize(storedFile.getSize())
                    .type(mediaType)
                    .localUrl(storedFile.getPublicUrl())
                    .description(description)
                    .altText(altText)
                    .build();

            Media saved = mediaService.createMedia(media, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommonResponse.success(toMediaResponse(saved), "文件上传成功"));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error("DUPLICATE_RESOURCE", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("RESOURCE_NOT_FOUND", e.getMessage()));
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("STORAGE_ERROR", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.error("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * 删除媒体文件
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteMedia(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (resolveUserId(request) == null) {
            return unauthorizedResponse();
        }

        try {
            mediaService.deleteMedia(id);
            return ResponseEntity.ok(CommonResponse.success("媒体删除成功"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error("MEDIA_NOT_FOUND", e.getMessage()));
        }
    }

    private Media.MediaType resolveMediaType(String type, MultipartFile file) {
        if (StringUtils.hasText(type)) {
            return Media.MediaType.valueOf(type.toUpperCase(Locale.ENGLISH));
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return mediaService.determineMediaType(extension);
    }

    private MediaResponse toMediaResponse(Media media) {
        MediaResponse.MediaResponseBuilder builder = MediaResponse.builder()
                .id(media.getId())
                .originalName(media.getOriginalName())
                .fileName(media.getFileName())
                .fileExtension(media.getFileExtension())
                .fileSize(media.getFileSize())
                .mimeType(resolveMimeType(media.getFileExtension()))
                .url(media.getUrl())
                .localPath(media.getLocalUrl())
                .type(media.getType() != null ? media.getType().name() : null)
                .width(media.getWidth())
                .height(media.getHeight())
                .description(media.getDescription())
                .altText(media.getAltText())
                .uploadedAt(media.getCreatedAt());

        if (media.getUploadedBy() != null) {
            builder.uploadedBy(MediaResponse.UploaderDto.builder()
                    .id(media.getUploadedBy().getId())
                    .username(media.getUploadedBy().getUsername())
                    .displayName(media.getUploadedBy().getDisplayName())
                    .build());
        }

        return builder.build();
    }

    private String resolveMimeType(String extension) {
        if (!StringUtils.hasText(extension)) {
            return null;
        }
        String ext = extension.toLowerCase(Locale.ENGLISH);
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "mp4":
                return "video/mp4";
            case "mp3":
                return "audio/mpeg";
            default:
                return "application/octet-stream";
        }
    }

    private Pageable buildPageable(int page, int size, String sortParam) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Sort sort = Sort.by("createdAt").descending();
        if (StringUtils.hasText(sortParam)) {
            String[] parts = sortParam.split(",");
            String property = parts[0];
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) {
                sort = Sort.by(property).ascending();
            } else {
                sort = Sort.by(property).descending();
            }
        }

        return PageRequest.of(safePage, safeSize, sort);
    }

    private Page<Media> buildPageFromList(List<Media> mediaList, Pageable pageable) {
        if (CollectionUtils.isEmpty(mediaList)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int start = Math.min((int) pageable.getOffset(), mediaList.size());
        int end = Math.min(start + pageable.getPageSize(), mediaList.size());
        List<Media> content = mediaList.subList(start, end);
        return new PageImpl<>(content, pageable, mediaList.size());
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
