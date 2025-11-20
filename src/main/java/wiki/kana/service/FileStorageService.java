package wiki.kana.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import wiki.kana.exception.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 简单文件存储服务
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path rootLocation;
    private final String urlPrefix;

    public FileStorageService(
            @Value("${app.media.upload-dir:uploads}") String uploadDir,
            @Value("${app.media.url-prefix:/uploads}") String urlPrefix) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.urlPrefix = normalizeUrlPrefix(urlPrefix);
        initDirectory(this.rootLocation);
    }

    /**
     * 存储文件并返回文件信息
     */
    public StoredFile store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("上传文件不能为空");
        }

        String originalFilename = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = file.getName();
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension != null) {
            extension = extension.toLowerCase();
        }

        String sanitizedBaseName = originalFilename;
        if (extension != null && sanitizedBaseName.toLowerCase().endsWith("." + extension)) {
            sanitizedBaseName = sanitizedBaseName.substring(0, sanitizedBaseName.length() - extension.length() - 1);
        }
        sanitizedBaseName = sanitizedBaseName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
        if (!StringUtils.hasText(sanitizedBaseName)) {
            sanitizedBaseName = "file";
        }

        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "");
        String storedFileName = sanitizedBaseName + "_" + uniqueSuffix + (StringUtils.hasText(extension) ? "." + extension : "");

        Path targetDir = resolveTargetDirectory(subDirectory);
        Path destinationFile = targetDir.resolve(storedFileName).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file {}", originalFilename, e);
            throw new FileStorageException("文件保存失败，请稍后再试", e);
        }

        String publicUrl = buildPublicUrl(subDirectory, storedFileName);
        return new StoredFile(originalFilename, storedFileName, extension, file.getSize(), destinationFile, publicUrl);
    }

    private Path resolveTargetDirectory(String subDirectory) {
        if (!StringUtils.hasText(subDirectory)) {
            initDirectory(rootLocation);
            return rootLocation;
        }
        Path targetDir = rootLocation.resolve(subDirectory).normalize();
        initDirectory(targetDir);
        return targetDir;
    }

    private void initDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            log.error("Failed to create upload directory {}", directory, e);
            throw new FileStorageException("无法创建上传目录: " + directory, e);
        }
    }

    private String buildPublicUrl(String subDirectory, String storedFileName) {
        StringBuilder builder = new StringBuilder();
        builder.append(urlPrefix);
        if (StringUtils.hasText(subDirectory)) {
            builder.append("/").append(subDirectory.replace("\\", "/"));
        }
        builder.append("/").append(storedFileName);
        return builder.toString();
    }

    private String normalizeUrlPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    /**
     * 已存储的文件信息
     */
    @Getter
    public static class StoredFile {
        private final String originalFilename;
        private final String storedFilename;
        private final String extension;
        private final long size;
        private final Path absolutePath;
        private final String publicUrl;

        public StoredFile(String originalFilename,
                          String storedFilename,
                          String extension,
                          long size,
                          Path absolutePath,
                          String publicUrl) {
            this.originalFilename = originalFilename;
            this.storedFilename = storedFilename;
            this.extension = extension;
            this.size = size;
            this.absolutePath = absolutePath;
            this.publicUrl = publicUrl;
        }
    }
}
