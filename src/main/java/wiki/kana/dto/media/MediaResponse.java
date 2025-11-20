package wiki.kana.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 媒体响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private Long id;
    private String originalName;
    private String fileName;
    private String fileExtension;
    private Long fileSize;
    private String mimeType;
    private String url;
    private String localPath;
    private String type;
    private Integer width;
    private Integer height;
    private String description;
    private String altText;
    private LocalDateTime uploadedAt;
    private UploaderDto uploadedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploaderDto {
        private Long id;
        private String username;
        private String displayName;
    }
}
